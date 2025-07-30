package com.hypergonial.chat.di

import com.hypergonial.chat.data.auth.AuthManager
import com.hypergonial.chat.data.remote.ChatApiService
import com.hypergonial.chat.data.remote.ChatApiServiceImpl
import com.hypergonial.chat.data.remote.server_updates.LongPollingUpdatesProvider
import com.hypergonial.chat.data.remote.server_updates.RealtimeUpdatesProvider
import com.hypergonial.chat.model.AppSettings
import com.hypergonial.chat.model.Cache
import com.hypergonial.chat.model.ChatClient
import com.hypergonial.chat.model.Client
import com.hypergonial.chat.model.EventManager
import com.hypergonial.chat.model.InvalidationReason
import com.hypergonial.chat.model.SessionInvalidatedEvent
import com.hypergonial.chat.model.exceptions.getApiException
import com.hypergonial.chat.model.platformHttpClient
import com.hypergonial.chat.model.settings
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.bearerAuth
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.context.startKoin
import org.koin.dsl.module

// TODO: update after network will be separated from ChatClient
object DI {
    private const val BASE_URL = ""

    fun init() = startKoin {
        modules(appModule)
    }

    private val appModule = module {
        single {
            platformHttpClient {
                install(ContentNegotiation) {
                    json(Json { ignoreUnknownKeys = true; encodeDefaults = false })
                }
                install(UserAgent) { agent = "chat-frontend-common" }
                install(HttpTimeout) {
                    requestTimeoutMillis = 5000
                }
                install(HttpRequestRetry) {
                    maxRetries = 5
                    retryIf { _, response -> response.status.value == 429 }
                    exponentialDelay(2000.0)
                }
                HttpResponseValidator {
                    validateResponse { response ->
                        if (response.status.value in 400..599) {
                            if (response.status == HttpStatusCode.Unauthorized) {
                                val eventManager: EventManager = get()
                                eventManager.dispatch(
                                    SessionInvalidatedEvent(InvalidationReason.AuthenticationFailure)
                                )
                            }
                            //val bodyText = runCatching { response.bodyAsText() }.getOrElse { it.localizedMessage }
                            val exc = getApiException(response.status, "Request failed with status ${response.status.value}")
                            throw exc
                        }
                    }
                    handleResponseException { cause, _ ->
                        // Можно добавить маппинг исключений, но по умолчанию пробрасываем
                        throw cause
                    }
                }
                defaultRequest {
                    url(BASE_URL)
                    val token = get<AuthManager>().accessToken
                    token?.let { bearerAuth(it) }
                }
            }
        }

        single { settings }

        single { AuthManager(get()) }

        single<ChatApiService> { ChatApiServiceImpl(get(), BASE_URL, get()) }

        single<RealtimeUpdatesProvider> { LongPollingUpdatesProvider(get()) }

        single { Cache() }
        single { EventManager() }

        single<Client> {
            ChatClient(
                apiService = get(),
                authManager = get(),
                realtime = get(),
                cache = get(),
                eventManager = get()
            )
        }
    }
}
