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
import com.hypergonial.chat.model.payloads.gateway.MessageCreate
import com.hypergonial.chat.model.payloads.gateway.Ready
import com.hypergonial.chat.model.payloads.gateway.StartTyping
import com.hypergonial.chat.model.payloads.gateway.TypingStart
import com.hypergonial.chat.model.payloads.rest.AuthResponse
import com.hypergonial.chat.model.payloads.rest.ChannelCreateRequest
import com.hypergonial.chat.model.payloads.rest.GuildCreateRequest
import com.hypergonial.chat.model.payloads.rest.GuildUpdateRequest
import com.hypergonial.chat.model.payloads.rest.MessageCreateRequest
import com.hypergonial.chat.model.payloads.rest.MessageUpdateRequest
import com.hypergonial.chat.model.payloads.rest.UserRegisterRequest
import com.hypergonial.chat.model.payloads.rest.UserUpdateRequest
import com.hypergonial.chat.platform
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
import kotlin.time.Duration.Companion.seconds
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
import kotlinx.datetime.Clock
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
@OptIn(InternalEventManagerApi::class)
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
    /** A mapping of channel ID to Pair(messageId, ackJob) */
    private val ackJobs: HashMap<Snowflake, Pair<Snowflake, Job>> = HashMap()
    /** A job that when completed, should indicate to the current gateway session to close the connection */
    private var gatewayCloseJob = Job()
    /**
     * A job that when completed indicates that the gateway has successfully connected. Can be used to wait until the
     * connection succeeds.
     */
    private var gatewayConnectedJob = Job()
    /** A job that represents the current gateway session */
    private var gatewaySession: Job? = null
    /** A job that manages typing indicators and dispatches appropriate events */
    private var typingIndicatorManageJob: Job? = null
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
                    encodeDefaults = false
                }
            )
        }
        install(UserAgent) { agent = "chat-frontend-${platform.name}" }

        install(WebSockets) {
            contentConverter =
                KotlinxWebsocketSerializationConverter(
                    Json {
                        ignoreUnknownKeys = true
                        encodeDefaults = false
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
        typingIndicatorManageJob = scope.launch { manageTypingIndicators() }
        scope.launch { eventManager.run() }
        eventManager.apply {
            subscribeInternal(::onGuildCreate)
            subscribeInternal(::onGuildUpdate)
            subscribeInternal(::onGuildRemove)
            subscribeInternal(::onChannelCreate)
            subscribeInternal(::onChannelRemove)
            subscribeInternal(::onMessageCreate)
            subscribeInternal(::onMessageUpdate)
            subscribeInternal(::onMessageRemove)
            subscribeInternal(::onMessageAck)
            subscribeInternal(::onMemberCreate)
            subscribeInternal(::onMemberRemove)
            subscribeInternal(::onUserUpdate)
            subscribeInternal(::onSessionInvalidated)
        }
    }

    override fun replaceScope(scope: CoroutineScope) {
        closeGateway()
        eventManager.stop()
        typingIndicatorManageJob?.cancel()
        _scope = scope
        typingIndicatorManageJob = scope.launch { manageTypingIndicators() }
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

        _isSuspended = false
        // Clear all cached messages when resuming, since these are possibly wildly out of date
        cache.clearMessageCache()

        if (isLoggedIn()) {
            withTimeout(2500) { waitUntilDisconnected() }

            logger.i { "Reconnecting to gateway..." }
            if (isLoggedIn()) {
                connect()
            }
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

            // Abstract away raw typing events, do not dispatch TYPING_START on indicator updates
            if (msg is TypingStart) {
                if (cache.updateTypingIndicator(msg.data.channelId, msg.data.userId)) {
                    logger.d { "Dispatching event: 'TypingStartEvent'" }
                    eventManager.dispatch(TypingStartEvent(msg.data.channelId, msg.data.userId))
                }
                continue
            }

            // Remove typing indicators when a message is created by that user
            if (msg is MessageCreate) {
                if (cache.removeTypingIndicator(msg.message.channelId, msg.message.author.id)) {
                    logger.d { "Dispatching event: 'TypingEndEvent'" }
                    eventManager.dispatch(TypingEndEvent(msg.message.channelId, msg.message.author.id))
                }
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
    private suspend fun sendGatewayMessage(msg: GatewayMessage) = responses.send(msg)

    override fun closeGateway() {
        gatewayCloseJob.complete()
    }

    override fun connect() = connect(isReconnect = false)

    private fun connect(isReconnect: Boolean) {
        gatewayCloseJob = Job()
        gatewayConnectedJob = Job()
        gatewaySession = scope.launch { runGatewaySession(isReconnect) }
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

    private suspend fun runGatewaySession(isReconnect: Boolean) {
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

            val readyEvent =
                ReadyEvent(
                    ready.data.user,
                    ready.data.guilds,
                    ready.data.readStates.associate {
                        it.channelId to ReadState(it.lastMessageId, it.lastReadMessageId)
                    },
                    isReconnect,
                )

            for ((channelId, readState) in readyEvent.readStates) {
                cache.setReadState(channelId, readState)
            }

            eventManager.dispatch(
                ReadyEvent(
                    ready.data.user,
                    ready.data.guilds,
                    ready.data.readStates.associate {
                        it.channelId to ReadState(it.lastMessageId, it.lastReadMessageId)
                    },
                    isReconnect,
                )
            )

            logger.i { "Gateway session is ready" }

            if (initialGuildIds.isEmpty()) {
                readyJob.complete()
            }

            val jobs =
                mapOf(
                    "RECV" to launch { receiveEvents(this@webSocket) },
                    "HEARTBEAT" to launch { performHeartbeating(this@webSocket) },
                    "SEND" to launch { forwardInternalMessages(this@webSocket) },
                    "MANUAL" to launch { gatewayCloseJob.join() },
                )

            jobs.forEach { (jobName, job) ->
                job.invokeOnCompletion {
                    if (it != null && it !is CancellationException) {
                        logger.e { "Job $jobName failed: $it" }
                    }
                }
            }

            var willReconnect = true

            // Wait until one of the jobs terminates
            select {
                jobs.forEach { (jobName, job) ->
                    job.onJoin {
                        // Cancel all other jobs if one terminates
                        jobs.values.forEach { it.cancel() }

                        // Gateway was closed manually
                        if (jobName == "MANUAL") {
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

    @OptIn(DelicateCacheApi::class)
    private fun onMessageCreate(event: MessageCreateEvent) {
        cache.setLastMessageId(event.message.channelId, event.message.id)

        // If we sent the message then hopefully it's also the last one we read
        if (event.message.author.id == cache.ownUser?.id) {
            cache.setLastReadMessageId(event.message.channelId, event.message.id)
        }

        cache.addMessage(event.message)
    }

    @OptIn(DelicateCacheApi::class)
    private fun onMessageUpdate(event: MessageUpdateEvent) {
        cache.updateMessage(event.message)
    }

    private fun onMessageRemove(event: MessageRemoveEvent) {
        cache.dropMessage(event.channelId, event.id)
    }

    private fun onMessageAck(event: MessageAckEvent) {
        cache.setLastReadMessageId(event.channelId, event.messageId)
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

    private suspend fun manageTypingIndicators() {
        while (true) {
            val now = Clock.System.now()
            cache.retainTypingIndicators { channelId, indicator ->
                // We use 6 seconds here to allow some leeway for network latency
                if (now - indicator.lastUpdated <= 6.seconds) true
                else {
                    eventManager.dispatch(TypingEndEvent(channelId, indicator.userId))
                    false
                }
            }
            delay(1000)
        }
    }

    override suspend fun waitUntilReady() {
        readyJob.join()
    }

    override suspend fun login(username: String, password: Secret<String>) {
        val resp = http.get("users/auth") { basicAuth(username, password.expose()) }.body<AuthResponse>()
        token = resp.token

        // Wipe all settings if the user has changed
        if (settings.getLastLoggedInAs() != resp.userId) {
            cache.clear()
            settings.clearUserPreferences()
        }

        settings.setToken(token!!.expose())
        settings.setLastLoggedInAs(resp.userId)

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

    override suspend fun updateSelf(scope: (UserUpdateRequest.Builder.() -> Unit)): User {
        val user =
            http
                .patch("users/@me") {
                    contentType(ContentType.Application.Json)
                    setBody(UserUpdateRequest.Builder().apply(scope).build())
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

    override suspend fun updateGuild(guildId: Snowflake, scope: GuildUpdateRequest.Builder.() -> Unit): Guild =
        http
            .patch("guilds/$guildId") {
                contentType(ContentType.Application.Json)
                setBody(GuildUpdateRequest.Builder().apply(scope).build())
            }
            .body<Guild>()

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
        limit: Int,
    ): List<Message> {
        require(listOfNotNull(before, after, around).size <= 1) { "Only one of before, after, or around can be set" }
        require(limit > 0) { "Limit must be greater than 0" }

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
        // Ignore request if a typing indicator is already active
        if (cache.getTypingIndicator(channelId, cache.ownUser!!.id)?.isActive() == true) {
            return
        }

        sendGatewayMessage(StartTyping(channelId))
    }

    override fun ackMessages(channelId: Snowflake) {
        val readState = cache.getReadState(channelId) ?: return
        val messageId = readState.lastMessageId ?: return

        // Ignore request if channel is already all read
        if (readState.lastReadMessageId?.let { it >= messageId } == true) {
            return
        }

        // Ignore request if a newer message is already being acked
        if (ackJobs[channelId]?.first?.let { it >= messageId } == true) {
            return
        }

        logger.i { "Acking message $messageId" }

        // Debounce acks to prevent spamming the api
        ackJobs[channelId]?.second?.cancel()
        val job =
            scope.launch {
                delay(2000)
                http.post("channels/$channelId/messages/$messageId/ack")
            }
        job.invokeOnCompletion { ackJobs.remove(channelId) }
        ackJobs[channelId] = Pair(messageId, job)
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
                    if (files.isEmpty()) return@onUpload

                    val completionRate = bytesSent.toDouble() / (contentLength?.toDouble() ?: return@onUpload)
                    eventManager.dispatch(UploadProgressEvent(nonce ?: return@onUpload, completionRate))
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
