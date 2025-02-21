package com.hypergonial.chat.model

import co.touchlab.kermit.Logger
import com.hypergonial.chat.genSessionId
import com.hypergonial.chat.model.exceptions.ClientException
import com.hypergonial.chat.model.exceptions.InvalidPayloadException
import com.hypergonial.chat.model.exceptions.NotFoundException
import com.hypergonial.chat.model.exceptions.RequestTimeoutException
import com.hypergonial.chat.model.exceptions.ResumeFailureException
import com.hypergonial.chat.model.exceptions.TransportException
import com.hypergonial.chat.model.exceptions.UnknownFailureException
import com.hypergonial.chat.model.exceptions.getApiException
import com.hypergonial.chat.model.payloads.Channel
import com.hypergonial.chat.model.payloads.Guild
import com.hypergonial.chat.model.payloads.Member
import com.hypergonial.chat.model.payloads.Message
import com.hypergonial.chat.model.payloads.Snowflake
import com.hypergonial.chat.model.payloads.User
import com.hypergonial.chat.model.payloads.gateway.EventConvertible
import com.hypergonial.chat.model.payloads.gateway.GatewayMessage
import com.hypergonial.chat.model.payloads.gateway.Heartbeat
import com.hypergonial.chat.model.payloads.gateway.HeartbeatAck
import com.hypergonial.chat.model.payloads.gateway.Hello
import com.hypergonial.chat.model.payloads.gateway.Identify
import com.hypergonial.chat.model.payloads.gateway.Ready
import com.hypergonial.chat.model.payloads.gateway.StartTyping
import com.hypergonial.chat.model.payloads.rest.AuthResponse
import com.hypergonial.chat.model.payloads.rest.ChannelCreateRequest
import com.hypergonial.chat.model.payloads.rest.GuildCreateRequest
import com.hypergonial.chat.model.payloads.rest.MessageCreateRequest
import com.hypergonial.chat.model.payloads.rest.MessageUpdateRequest
import com.hypergonial.chat.model.payloads.rest.UserRegisterRequest
import com.hypergonial.chat.model.payloads.rest.UserUpdateRequest
import com.hypergonial.chat.platform
import com.hypergonial.chat.toDataUrl
import io.github.vinceglb.filekit.core.PlatformFile
import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.call.body
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.onUpload
import io.ktor.client.plugins.timeout
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.receiveDeserialized
import io.ktor.client.plugins.websocket.sendSerialized
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.basicAuth
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.forms.FormPart
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.quote
import io.ktor.serialization.WebsocketDeserializeException
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.serialization.kotlinx.json.json
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel as QueueChannel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withTimeout
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.serializer

/**
 * The primary implementation of a client that connects to the chat backend
 *
 * This class is responsible for managing the connection to the chat backend, sending and receiving messages, and
 * dispatching events to the event manager.
 *
 * @param scope A coroutineScope to launch background tasks on
 * @param maxReconnectAttempts The maximum number of reconnection attempts before the client gives up
 */
class ChatClient(scope: CoroutineScope, override val maxReconnectAttempts: Int = 3) : Client {
    /** The bearer token used for authentication */
    private var token: Secret<String>? = settings.getToken()?.let { Secret(it) }

    private var _scope = scope
    private var _isSuspended = false
    private var _reconnectAttempts = 0

    override val isSuspended
        get() = _isSuspended

    override val scope
        get() = _scope

    override val reconnectAttempts
        get() = _reconnectAttempts

    override val sessionId: String = genSessionId()

    private val readyJob = Job()
    /** The logger used for this class */
    private val logger = Logger.withTag("ChatClient")
    /** The JSON deserializer used for error messages */
    private val errorDeserializer = Json { prettyPrint = true }
    /** The API endpoint configuration */
    private var config = settings.getDevSettings()
    /** A queue of messages to be sent to the gateway */
    private val responses: QueueChannel<GatewayMessage> = QueueChannel()
    /** A channel for sending heartbeat ACKs between jobs */
    private val heartbeatAckQueue: QueueChannel<HeartbeatAckEvent> = QueueChannel(1)
    /** A job that when completed, should indicate to the current gateway session to close the connection */
    private var gatewayCloseJob = Job()
    /**
     * A job that when completed indicates that the gateway has successfully connected. Can be used to wait until the
     * connection succeeds.
     */
    private var gatewayConnectedJob = Job()
    /** A job that represents the current gateway session */
    private var gatewaySession: Job? = null
    /**
     * The interval at which the gateway should send heartbeats This is set by the HELLO message received from the
     * gateway.
     */
    private var heartbeatInterval: Long? = null
    /**
     * The set of guild IDs that the client should receive on startup from the gateway
     *
     * Ids are then removed from this set as they are received through GUILD_CREATE events.
     */
    private val initialGuildIds: HashSet<Snowflake> = HashSet()
    /** The event manager that is used to dispatch events */
    override val eventManager = EventManager()
    /** The cache that is used to store entities received through the gateway */
    override val cache = Cache()

    /** The main http client used for API requests */
    private val http = platformHttpClient {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    encodeDefaults = true
                }
            )
        }
        install(UserAgent) { agent = "chat-frontend-${platform.name}" }

        install(WebSockets) {
            contentConverter =
                KotlinxWebsocketSerializationConverter(
                    Json {
                        ignoreUnknownKeys = true
                        encodeDefaults = true
                    }
                )
        }

        install(HttpTimeout) { requestTimeoutMillis = 5000 }

        install(HttpRequestRetry) {
            maxRetries = 5

            retryIf { _, response -> response.status.value == 429 }
            exponentialDelay(2000.0)
        }

        HttpResponseValidator {
            validateResponse { response ->
                if (response.status.value !in 400..599) {
                    return@validateResponse
                }

                if (response.status == HttpStatusCode.Unauthorized) {
                    eventManager.dispatch(SessionInvalidatedEvent(InvalidationReason.AuthenticationFailure))
                }

                val body =
                    try {
                        response.bodyAsText()
                    } catch (e: NoTransformationFoundException) {
                        "Failed to parse body: ${e.message}"
                    }

                val exc = getApiException(response.status, "Request failed with status ${response.status.value}")

                try {
                    val prettyJson =
                        errorDeserializer.encodeToString(
                            errorDeserializer.serializersModule.serializer<JsonElement>(),
                            errorDeserializer.parseToJsonElement(body),
                        )
                    logger.e {
                        "${exc::class.simpleName} - Request failed with status ${response.status.value}\n" +
                            "Path: ${response.request.url}\n" +
                            "Body: $prettyJson"
                    }

                    throw exc
                } catch (_: SerializationException) {
                    logger.e {
                        "${exc::class.simpleName} - Request failed with status ${response.status.value}\n" +
                            "Path: ${response.request.url}\n" +
                            "Body (failed deserialize): $body"
                    }
                    throw exc
                }
            }

            handleResponseException { exc, _ ->
                // Wrap unhandled errors in ClientException
                when (exc) {
                    // Do not alter CancellationException or it breaks coroutines
                    is CancellationException -> throw exc
                    // Is already wrapped, just rethrow it
                    is ClientException -> throw exc
                    is ClientRequestException -> throw getApiException(exc.response.status, exc.message, exc)
                    is ServerResponseException -> throw getApiException(exc.response.status, exc.message, exc)
                    is HttpRequestTimeoutException -> throw RequestTimeoutException(exc.message, exc)
                    is SocketTimeoutException -> throw RequestTimeoutException(exc.message, exc)
                    is ConnectTimeoutException -> throw RequestTimeoutException(exc.message, exc)
                    is IOException -> throw TransportException(exc.message, exc)
                    is SerializationException -> throw InvalidPayloadException(exc.message, exc)
                    else -> throw UnknownFailureException(exc.message, exc)
                }
            }
        }

        defaultRequest {
            url(config.apiUrl)

            if (token != null) {
                bearerAuth(token!!.expose())
            }
        }
    }

    init {
        scope.launch { eventManager.run() }
        eventManager.apply {
            subscribe(::onGuildCreate)
            subscribe(::onGuildUpdate)
            subscribe(::onGuildRemove)
            subscribe(::onChannelCreate)
            subscribe(::onChannelRemove)
            subscribe(::onMessageCreate)
            subscribe(::onMessageUpdate)
            subscribe(::onMessageRemove)
            subscribe(::onMemberCreate)
            subscribe(::onMemberRemove)
            subscribe(::onUserUpdate)
            subscribe(::onSessionInvalidated)
        }
    }

    override fun replaceScope(scope: CoroutineScope) {
        closeGateway()
        eventManager.stop()
        _scope = scope
        scope.launch { eventManager.run() }
    }

    override fun pause() {
        _isSuspended = true
        closeGateway()
        eventManager.dispatch(LifecyclePausedEvent())
    }

    override suspend fun resume() {
        if (!scope.isActive) {
            logger.e { "Cannot resume client, scope is inactive" }
            throw ResumeFailureException(
                "Cannot resume client, coroutine scope is inactive\n" +
                    "You should call replaceScope() with a new scope before calling resume()"
            )
        }

        withTimeout(2500) { waitUntilDisconnected() }
        _isSuspended = false
        logger.i { "Reconnecting to gateway..." }
        if (isLoggedIn()) {
            connect()
        }
        eventManager.dispatch(LifecycleResumedEvent())
    }

    override fun isLoggedIn(): Boolean {
        return !token?.expose().isNullOrEmpty()
    }

    override fun isReady(): Boolean {
        return readyJob.isCompleted
    }

    /** Perform heartbeating with the gateway. */
    private suspend fun performHeartbeating(session: DefaultClientWebSocketSession) {
        while (true) {
            delay(heartbeatInterval ?: 1000)
            session.sendSerialized<GatewayMessage>(Heartbeat)
            try {
                // Wait for ACK and assume session is dead if not received in time
                withTimeout(5000) { heartbeatAckQueue.receive() }
            } catch (e: TimeoutCancellationException) {
                logger.e { "Heartbeat ACK not received in time: $e" }
                session.outgoing.send(
                    Frame.Close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Heartbeat ACK not received in time"))
                )
                return
            }
        }
    }

    /** Receive events from the gateway and dispatch them to the event manager. */
    private suspend fun receiveEvents(session: DefaultClientWebSocketSession) {
        while (true) {
            // Not "val msg = try { ... } catch { ... }" because of a miscompilation in the WASM
            // target
            // Don't ask me why, I have no idea
            val msg: GatewayMessage
            try {
                msg = session.receiveDeserialized<GatewayMessage>()
            } catch (e: WebsocketDeserializeException) {
                logger.e { "Failed to deserialize message: $e\nFrame: ${e.frame}" }
                if (e.frame !is Frame.Close) {
                    continue
                } else {
                    logger.i { "Received close frame." }
                    return
                }
            } catch (e: SerializationException) {
                logger.e { "Failed to deserialize message: $e" }
                continue
            }

            if (msg is HeartbeatAck) {
                heartbeatAckQueue.send(HeartbeatAckEvent())
                continue
            }

            if (msg is EventConvertible) {
                val event = msg.toEvent()
                logger.d { "Dispatching event: '${event::class.simpleName}'" }
                eventManager.dispatch(event)
            }
        }
    }

    /** Receive internal messages sent by this application's components and forward them to the gateway. */
    private suspend fun forwardInternalMessages(session: DefaultClientWebSocketSession) {
        for (msg in responses) {
            session.sendSerialized<GatewayMessage>(msg)
        }
        logger.i { "Internal gateway message queue closed." }
    }

    /** Send a message to the gateway. */
    @Suppress("UnusedPrivateMember")
    private suspend fun sendGatewayMessage(msg: GatewayMessage) {
        responses.send(msg)
    }

    override fun closeGateway() {
        gatewayCloseJob.complete()
    }

    override fun connect() = connect(isReconnect = false)

    private fun connect(isReconnect: Boolean) {
        gatewayCloseJob = Job()
        gatewayConnectedJob = Job()
        gatewaySession = scope.launch { gatewaySession(isReconnect) }
        scope.launch {
            delay(5000)
            if (!gatewayConnectedJob.isCompleted) {
                gatewaySession?.cancel()
                gatewayConnectedJob = Job()
                _reconnectAttempts++
                eventManager.dispatch(
                    SessionInvalidatedEvent(
                        InvalidationReason.Timeout,
                        willReconnect = reconnectAttempts < maxReconnectAttempts,
                    )
                )
            }
        }
    }

    override fun isConnected(): Boolean = gatewayConnectedJob.isCompleted

    override suspend fun waitUntilConnected() = gatewayConnectedJob.join()

    override suspend fun waitUntilDisconnected() = gatewaySession?.join() ?: Unit

    private suspend fun gatewaySession(isReconnect: Boolean) {
        check(token != null) { "Cannot connect without a token" }

        logger.i { "Starting new gateway session to ${config.gatewayUrl}" }

        http.webSocket(request = { url(config.gatewayUrl) }) {
            logger.i { "Connected to gateway at ${config.gatewayUrl}" }
            val hello = receiveDeserialized<GatewayMessage>()

            if (hello !is Hello) {
                logger.e { "Expected HELLO, got $hello" }
                outgoing.send(Frame.Close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Expected HELLO")))
                return@webSocket
            }

            heartbeatInterval = hello.data.heartbeatInterval
            _reconnectAttempts = 0
            gatewayConnectedJob.complete()
            logger.i { "Heartbeat interval is set to $heartbeatInterval ms" }

            sendSerialized<GatewayMessage>(Identify(token!!))

            val ready =
                try {
                    receiveDeserialized<GatewayMessage>()
                } catch (_: ClosedReceiveChannelException) {
                    val reason = closeReason.await()
                    logger.e { "Channel was closed after IDENTIFY: $reason" }
                    eventManager.dispatch(SessionInvalidatedEvent(InvalidationReason.AuthenticationFailure))
                    return@webSocket
                } catch (e: WebsocketDeserializeException) {
                    if (e.frame is Frame.Close) {
                        logger.i { "Gateway session closed: ${e.frame}" }
                        eventManager.dispatch(SessionInvalidatedEvent(InvalidationReason.AuthenticationFailure))
                        return@webSocket
                    }

                    logger.e { "Failed to deserialize IDENTIFY response: $e\nFrame: ${e.frame}" }
                    eventManager.dispatch(SessionInvalidatedEvent(InvalidationReason.Abnormal))

                    return@webSocket
                }

            logger.i { "Gateway session authenticated" }

            if (ready !is Ready) {
                logger.e { "Expected READY, got $ready" }

                outgoing.send(Frame.Close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Expected READY")))
                eventManager.dispatch(SessionInvalidatedEvent(InvalidationReason.Abnormal))
                return@webSocket
            }

            cache.putOwnUser(ready.data.user)
            initialGuildIds.addAll(ready.data.guilds.map { it.id })

            for (guild in ready.data.guilds) {
                cache.putGuild(guild)
            }

            if (initialGuildIds.isEmpty()) {
                readyJob.complete()
            }

            logger.i { "Gateway session is ready" }

            eventManager.dispatch(ReadyEvent(ready.data.user, ready.data.guilds, isReconnect))

            val jobs =
                listOf(
                    launch { receiveEvents(this@webSocket) },
                    launch { performHeartbeating(this@webSocket) },
                    launch { forwardInternalMessages(this@webSocket) },
                    launch { gatewayCloseJob.join() },
                )

            jobs.forEachIndexed { index, job ->
                job.invokeOnCompletion {
                    if (it != null && it !is CancellationException) {
                        logger.e { "Job $index failed: $it" }
                    }
                }
            }

            var willReconnect = true

            // Wait until one of the jobs terminates
            select {
                jobs.forEachIndexed { i, job ->
                    job.onJoin {
                        // Cancel all other jobs if one terminates
                        jobs.forEach { it.cancel() }

                        // Gateway was closed manually
                        if (i == 3) {
                            willReconnect = false
                        }
                    }
                }
            }

            logger.i { "Closing gateway session" }

            outgoing.trySend(Frame.Close(CloseReason(CloseReason.Codes.NORMAL, "Gateway session terminated")))

            eventManager.dispatch(SessionInvalidatedEvent(InvalidationReason.Normal, willReconnect))
        }
    }

    private fun onSessionInvalidated(event: SessionInvalidatedEvent) {
        if (!event.willReconnect) {
            return
        }

        if (_reconnectAttempts > 0) {
            logger.w { "Backing off for ${_reconnectAttempts * 5} seconds..." }
        }

        scope.launch {
            delay(_reconnectAttempts * 5000L)
            logger.w { "Attempting to reconnect to gateway..." }
            connect(isReconnect = true)
        }
    }

    private fun onGuildCreate(event: GuildCreateEvent) {
        cache.putGuild(event.guild)
        event.channels.forEach { cache.putChannel(it) }

        event.members.forEach {
            cache.putUser(it)
            cache.putMember(it)
        }
        initialGuildIds.remove(event.guild.id)

        // If all initial guilds have been received, mark client as ready
        if (initialGuildIds.isEmpty()) {
            readyJob.complete()
        }
    }

    private fun onGuildUpdate(event: GuildUpdateEvent) {
        cache.putGuild(event.guild)
    }

    private fun onGuildRemove(event: GuildRemoveEvent) {
        cache.dropGuild(event.guild.id)
    }

    private fun onChannelCreate(event: ChannelCreateEvent) {
        cache.putChannel(event.channel)
    }

    private fun onChannelRemove(event: ChannelRemoveEvent) {
        cache.dropChannel(event.channel.id)
    }

    private fun onMessageCreate(event: MessageCreateEvent) {
        cache.addMessage(event.message)

        if (event.message.author is Member) {
            cache.putMember(event.message.author)
        }
    }

    private fun onMessageUpdate(event: MessageUpdateEvent) {
        cache.updateMessage(event.message)

        if (event.message.author is Member) {
            cache.putMember(event.message.author)
        }
    }

    private fun onMessageRemove(event: MessageRemoveEvent) {
        cache.dropMessage(event.channelId, event.id)
    }

    private fun onMemberCreate(event: MemberCreateEvent) {
        cache.putUser(event.member)
        cache.putMember(event.member)
    }

    private fun onMemberRemove(event: MemberRemoveEvent) {
        cache.dropMember(event.guildId, event.id)
    }

    private fun onUserUpdate(event: UserUpdateEvent) {
        cache.putUser(event.user)

        if (event.user.id == cache.ownUser?.id) {
            cache.putOwnUser(event.user)
        }
    }

    override suspend fun waitUntilReady() {
        readyJob.join()
    }

    override suspend fun login(username: String, password: Secret<String>) {
        token = http.get("users/auth") { basicAuth(username, password.expose()) }.body<AuthResponse>().token
        settings.setToken(token!!.expose())

        eventManager.dispatch(LoginEvent())
    }

    override suspend fun register(username: String, password: Secret<String>) {
        val user =
            http
                .post("users") {
                    contentType(ContentType.Application.Json)
                    setBody(UserRegisterRequest(username, password))
                }
                .body<User>()
        cache.putOwnUser(user)
    }

    override suspend fun updateSelf(username: String?, displayName: String?, avatar: PlatformFile?): User {
        val user =
            http
                .patch("users/@me") {
                    contentType(ContentType.Application.Json)
                    setBody(UserUpdateRequest(username, displayName, avatar?.toDataUrl()))
                }
                .body<User>()
        cache.putOwnUser(user)
        return user
    }

    override suspend fun fetchGuild(guildId: Snowflake): Guild {
        return http.get("guilds/$guildId").body<Guild>()
    }

    override suspend fun fetchChannel(channelId: Snowflake): Channel {
        return http.get("channels/$channelId").body<Channel>()
    }

    override suspend fun createGuild(name: String): Guild {
        return http
            .post("guilds") {
                contentType(ContentType.Application.Json)
                setBody(GuildCreateRequest(name = name))
            }
            .body<Guild>()
    }

    override suspend fun deleteGuild(id: Snowflake) {
        http.delete("guilds/$id")
    }

    override suspend fun leaveGuild(id: Snowflake) {
        http.delete("guilds/$id/members/@me")
    }

    override suspend fun joinGuild(guildId: Snowflake): Member {
        return http.post("guilds/$guildId/members").body<Member>()
    }

    override suspend fun createChannel(guildId: Snowflake, name: String): Channel {
        return http
            .post("guilds/$guildId/channels") {
                contentType(ContentType.Application.Json)
                setBody(ChannelCreateRequest(name = name))
            }
            .body<Channel>()
    }

    override suspend fun deleteChannel(channelId: Snowflake) {
        http.delete("channels/$channelId")
    }

    override suspend fun checkUsernameForAvailability(username: String): Boolean {
        try {
            http.get("usernames/$username")
            return false
        } catch (_: NotFoundException) {
            // If the user is not found, the username is available
            return true
        }
    }

    override suspend fun fetchMessages(
        channelId: Snowflake,
        before: Snowflake?,
        after: Snowflake?,
        around: Snowflake?,
        limit: UInt,
    ): List<Message> {
        require(listOfNotNull(before, after, around).size <= 1) { "Only one of before, after, or around can be set" }

        return http
            .get("channels/$channelId/messages") {
                parameter("before", before?.toString())
                parameter("after", after?.toString())
                parameter("around", around?.toString())
                parameter("limit", limit.toString())
            }
            .body<List<Message>>()
            .sortedBy { it.id }
    }

    override suspend fun setTypingIndicator(channelId: Snowflake) {
        sendGatewayMessage(StartTyping(channelId))
    }

    override suspend fun sendMessage(
        channelId: Snowflake,
        content: String,
        nonce: String?,
        attachments: List<PlatformFile>,
    ): Message {
        // See https://stackoverflow.com/questions/69830965/ktor-client-post-multipart-form-data
        val json =
            FormPart(
                "json",
                Json.encodeToString<MessageCreateRequest>(MessageCreateRequest(content, nonce)),
                Headers.build { append(HttpHeaders.ContentType, ContentType.Application.Json.toString()) },
            )

        val files =
            attachments.mapIndexed { i, item ->
                FormPart(
                    "attachment-$i",
                    /* TODO: Replace with InputProvider(item.getSize()) { item.inputStream().asInput() }
                    when FileKit moves to kotlinx.io instead of okio */
                    item.readBytes(),
                    Headers.build {
                        append(HttpHeaders.ContentType, Mime.fromUrl(item.name).toString())
                        append(HttpHeaders.ContentDisposition, "filename=${item.name.quote()}")
                    },
                )
            }

        return http
            .submitFormWithBinaryData(
                url = "channels/$channelId/messages",
                formData =
                    formData {
                        append(json)
                        files.forEach { append(it) }
                    },
            ) {
                // Allow a bigger timeout for uploads
                timeout {
                    requestTimeoutMillis = 60000
                    connectTimeoutMillis = 5000
                }

                onUpload { bytesSent, contentLength ->
                    val completionRate = bytesSent.toDouble() / (contentLength?.toDouble() ?: return@onUpload)
                    val nonce = nonce ?: return@onUpload
                    eventManager.dispatch(UploadProgressEvent(nonce, completionRate))
                }
            }
            .body<Message>()
    }

    override suspend fun editMessage(channelId: Snowflake, messageId: Snowflake, content: String?): Message {
        return http
            .patch("channels/$channelId/messages/$messageId") {
                contentType(ContentType.Application.Json)
                setBody(MessageUpdateRequest(content))
            }
            .body<Message>()
    }

    override suspend fun deleteMessage(channelId: Snowflake, messageId: Snowflake) {
        http.delete("channels/$channelId/messages/$messageId")
    }

    override fun reloadConfig() {
        config = settings.getDevSettings()
        token = settings.getToken()?.let { Secret(it) }
    }

    override fun logout() {
        token = null
        settings.removeToken()
        cache.clear()
        eventManager.dispatch(LogoutEvent())
    }

    override fun onDestroy() {
        closeGateway()
        eventManager.stop()

        // If we never managed to connect, wipe login credentials
        if (!readyJob.isCompleted) {
            logout()
        }

        http.close()
    }
}
