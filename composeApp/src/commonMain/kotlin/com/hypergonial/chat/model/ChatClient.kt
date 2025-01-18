package com.hypergonial.chat.model

import com.hypergonial.chat.model.exceptions.ApiException
import com.hypergonial.chat.model.payloads.Channel
import com.hypergonial.chat.model.payloads.Guild
import com.hypergonial.chat.model.payloads.Message
import com.hypergonial.chat.model.payloads.Snowflake
import com.hypergonial.chat.model.payloads.User
import com.hypergonial.chat.model.payloads.gateway.EventConvertable
import com.hypergonial.chat.model.payloads.gateway.GatewayMessage
import com.hypergonial.chat.model.payloads.gateway.Heartbeat
import com.hypergonial.chat.model.payloads.gateway.Hello
import com.hypergonial.chat.model.payloads.gateway.Identify
import com.hypergonial.chat.model.payloads.gateway.InvalidSession
import com.hypergonial.chat.model.payloads.gateway.Ready
import com.hypergonial.chat.model.payloads.rest.AuthResponse
import com.hypergonial.chat.model.payloads.rest.UserRegisterRequest
import com.hypergonial.chat.platform
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.receiveDeserialized
import io.ktor.client.plugins.websocket.sendSerialized
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.basicAuth
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.serialization.kotlinx.json.json
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.serializer
import kotlinx.coroutines.channels.Channel as QueueChannel

class ChatClient : Client {
    private var token: Secret<String>? = settings.getToken()?.let { Secret(it) }
    private val readyJob = Job()
    private val logger = KotlinLogging.logger {}
    private val errorDeserializer = Json {
        prettyPrint = true
    }
    private val config = settings.getApiSettings()
    private val responses: QueueChannel<GatewayMessage> = QueueChannel()
    private var heartbeatInterval: Long? = null
    private val initialGuildIds: HashSet<Snowflake> = HashSet()
    override val eventManager = EventManager()
    override val cache = Cache()

    private val http = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            })
        }
        install(UserAgent) {
            agent = "chat-frontend-${platform.name}"
        }

        install(WebSockets) {
            contentConverter = KotlinxWebsocketSerializationConverter(Json)
        }

        install(HttpRequestRetry) {
            maxRetries = 5

            retryIf { _, response -> response.status.value == 429 }
            exponentialDelay(2000.0)
        }

        HttpResponseValidator {
            validateResponse { response ->
                if (!response.status.isSuccess()) {
                    val body = response.bodyAsText()

                    try {
                        val prettyJson = errorDeserializer.encodeToString(
                            errorDeserializer.serializersModule.serializer<JsonElement>(),
                            errorDeserializer.parseToJsonElement(body)
                        )
                        logger.error { "Request failed with status ${response.status.value}\nBody: $prettyJson" }
                        throw ApiException(
                            "Request failed with status ${response.status.value}"
                        )
                    } catch (e: SerializationException) {
                        logger.error { "Request failed with status ${response.status.value}\nBody (failed deserialize): ${body}" }
                        throw ApiException("Request failed with status ${response.status.value}")
                    }

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
        eventManager.subscribe(::onGuildCreate)
    }



    override fun isLoggedIn(): Boolean {
        return token != null
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
                withTimeout(5000) {
                    eventManager.waitFor(HeartbeatAckEvent::class)
                }
            } catch (e: TimeoutCancellationException) {
                logger.error { "Heartbeat ACK not received in time: $e" }
                session.outgoing.send(
                    Frame.Close(
                        CloseReason(
                            CloseReason.Codes.VIOLATED_POLICY,
                            "Heartbeat ACK not received in time"
                        )
                    )
                )
                return
            }
        }
    }

    /** Receive events from the gateway and dispatch them to the event manager. */
    private suspend fun receiveEvents(session: DefaultClientWebSocketSession) {
        while (true) {
            val msg = session.receiveDeserialized<GatewayMessage>()

            if (msg is InvalidSession) {
                logger.error { "Server invalidated gateway session: ${msg.reason}" }
                return
            }

            if (msg is EventConvertable) {
                eventManager.dispatch(msg.toEvent())
                continue
            }
        }
    }

    /** Receive internal messages sent by this application's components and forward them to the gateway. */
    private suspend fun forwardInternalMessages(session: DefaultClientWebSocketSession) {
        for (msg in responses) {
            session.sendSerialized<GatewayMessage>(msg)
        }
    }

    /** Send a message to the gateway. */
    private suspend fun sendGatewayMessage(msg: GatewayMessage) {
        responses.send(msg)
    }

    override suspend fun connect() {
        check(token != null) { "Cannot connect without a token" }



        http.webSocket(config.gatewayUrl) {
            val hello = receiveDeserialized<GatewayMessage>()

            if (hello !is Hello) {
                logger.error { "Expected HELLO, got $hello" }
                outgoing.send(
                    Frame.Close(
                        CloseReason(
                            CloseReason.Codes.VIOLATED_POLICY,
                            "Expected HELLO"
                        )
                    )
                )
                return@webSocket
            }

            heartbeatInterval = hello.data.heartbeatInterval

            sendSerialized<GatewayMessage>(Identify(token!!))

            val ready = receiveDeserialized<GatewayMessage>()

            if (ready !is Ready) {
                logger.error { "Expected READY, got $ready" }
                outgoing.send(
                    Frame.Close(
                        CloseReason(
                            CloseReason.Codes.VIOLATED_POLICY,
                            "Expected READY"
                        )
                    )
                )
                return@webSocket
            }

            cache.putOwnUser(ready.data.user)
            initialGuildIds.addAll(ready.data.guilds.map { it.id })

            for (guild in ready.data.guilds) {
                cache.putGuild(guild)
            }

            val jobs = listOf(
                launch { receiveEvents(this@webSocket) },
                launch { performHeartbeating(this@webSocket) },
                launch { forwardInternalMessages(this@webSocket) },
            )

            // Wait until one of the jobs terminates
            select {
                for (job in jobs) {
                    job.onJoin {
                        // Cancel all other jobs if one terminates
                        jobs.forEach { it.cancel() }
                    }
                }
            }
        }
    }

    private suspend fun onGuildCreate(event: GuildCreateEvent) {
        cache.putGuild(event.guild)
        event.channels.forEach { cache.putChannel(it) }
        event.members.forEach { cache.putUser(it); cache.putMember(it) }
        initialGuildIds.remove(event.guild.id)

        // If all initial guilds have been received, mark client as ready
        if (initialGuildIds.isEmpty()) {
            readyJob.complete()
        }
    }

    private suspend fun onGuildRemove(event: GuildRemoveEvent) {
        cache.dropGuild(event.guild.id)
    }

    private suspend fun onChannelCreate(event: ChannelCreateEvent) {
        cache.putChannel(event.channel)
    }

    private suspend fun onChannelRemove(event: ChannelRemoveEvent) {
        cache.dropChannel(event.channel.id)
    }

    private suspend fun onMessageCreate(event: MessageCreateEvent) {
        cache.putMessage(event.message)
    }

    private suspend fun onMemberCreate(event: MemberCreateEvent) {
        cache.putUser(event.member)
        cache.putMember(event.member)
    }

    private suspend fun onMemberRemove(event: MemberRemoveEvent) {
        cache.dropMember(event.guildId, event.id)
    }

    override suspend fun waitUntilReady() {
        readyJob.join()
    }

    override suspend fun login(username: String, password: Secret<String>) {
        token = http.get("/login") {
            basicAuth(username, password.expose())
        }.body<AuthResponse>().token
        settings.setToken(token!!.expose())
    }

    override suspend fun register(username: String, password: Secret<String>) {
        val user = http.post("/register") {
            contentType(ContentType.Application.Json)
            setBody(UserRegisterRequest(username, password))
        }.body<User>()
    }

    override suspend fun fetchGuild(guildId: Snowflake): Guild {
        TODO("Not yet implemented")
    }

    override suspend fun fetchGuildChannels(guildId: Snowflake): List<Channel> {
        TODO("Not yet implemented")
    }

    override suspend fun fetchChannel(channelId: Snowflake): Channel {
        TODO("Not yet implemented")
    }


    override suspend fun fetchMessages(
        channelId: Snowflake, before: Snowflake?, after: Snowflake?, limit: UInt
    ): List<Message> {
        TODO("Not yet implemented")
    }

    override suspend fun sendMessage(channelId: Snowflake, content: String, nonce: String?) {
        TODO("Not yet implemented")
    }

    override fun logout() {
        token = null
        settings.removeToken()
    }

    override fun onDestroy() {
        http.close()
    }

    override suspend fun editMessage(channelId: Snowflake, messageId: Snowflake, content: String?) {
        TODO("Not yet implemented")
    }


}
