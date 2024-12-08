package com.hypergonial.chat.view.components

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.popTo
import com.arkivanov.decompose.router.stack.replaceAll
import com.arkivanov.decompose.router.stack.webhistory.WebHistoryController
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackHandlerOwner
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.hypergonial.chat.model.Client
import com.hypergonial.chat.model.MockClient
import com.hypergonial.chat.model.Secret
import com.hypergonial.chat.view.withFallbackValue
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer


interface RootComponent : BackHandlerOwner {
    val data: Value<RootData>
    val stack: Value<ChildStack<*, *>>


    fun onBackClicked()
    fun onBackClicked(toIndex: Int)

    class RootData

    sealed class Child {
        class LoginChild(val component: LoginComponent) : Child()
        class HomeChild(val component: HomeComponent) : Child()
        class NotFoundChild(val component: NotFoundComponent) : Child()
    }
}

@OptIn(ExperimentalDecomposeApi::class)
class DefaultRootComponent(
    val ctx: ComponentContext, deepLink: DeepLink = DeepLink.None, webHistoryController: WebHistoryController? = null
) : RootComponent, ComponentContext by ctx {
    override val data = MutableValue(
        RootComponent.RootData()
    )

    private val logger = KotlinLogging.logger {}

    private val client: Client = retainedInstance {
        MockClient(token = stateKeeper.consume(key = "TOKEN", strategy = serializer<Secret<String>>()))
    }

    private val nav = StackNavigation<Config>()

    private val _stack = childStack(
        source = nav,
        serializer = Config.serializer().withFallbackValue(getDefaultConfig()),
        handleBackButton = true,
        initialStack = {
            getInitialStack(
                webHistoryPaths = webHistoryController?.historyPaths,
                deepLink = deepLink,
                isLoggedIn = client.isLoggedIn()
            )
        },
        childFactory = ::child,
    )

    override val stack: Value<ChildStack<*, RootComponent.Child>> = _stack

    init {
        // Attach the web history controller to the navigator.
        webHistoryController?.attach(
            navigator = nav,
            serializer = Config.serializer().withFallbackValue(getDefaultConfig()),
            stack = _stack,
            getPath = Companion::getPathForConfig,
            getConfiguration = Companion::getConfigForPath,
        )

        stateKeeper.register(key = "TOKEN", strategy = serializer<Secret<String>>()) {
            client.token
        }
    }

    private fun getDefaultConfig(): Config {
        if (client.isLoggedIn()) {
            return Config.Home
        }
        return Config.Login
    }

    /** The child factory for the root component's childStack. */
    private fun child(config: Config, componentContext: ComponentContext): RootComponent.Child = when (config) {
        is Config.Login -> RootComponent.Child.LoginChild(
            DefaultLoginComponent(ctx = componentContext, client = client, onLogin = { nav.replaceAll(Config.Home) })
        )

        Config.NotFound -> RootComponent.Child.NotFoundChild(
            DefaultNotFoundComponent(
                ctx = componentContext,
            )
        )

        Config.Home -> RootComponent.Child.HomeChild(DefaultHomeComponent(ctx = componentContext, onLogout = {
            client.logout()
            nav.replaceAll(Config.Login)
        }))
    }

    override fun onBackClicked() {
        nav.pop()
    }

    override fun onBackClicked(toIndex: Int) {
        nav.popTo(index = toIndex)
    }

    /** Handling for web paths. */
    private companion object {
        private const val WEB_PATH_LOGIN = "login"
        private const val WEB_PATH_NOT_FOUND = "not_found"

        private fun getInitialStack(
            webHistoryPaths: List<String>?,
            deepLink: DeepLink,
            isLoggedIn: Boolean
        ): List<Config> =
            webHistoryPaths?.takeUnless(List<*>::isEmpty)?.map(Companion::getConfigForPath) ?: getInitialStack(
                deepLink,
                isLoggedIn
            )

        private fun getInitialStack(deepLink: DeepLink, isLoggedIn: Boolean): List<Config> {
            if (!isLoggedIn) {
                return listOf(Config.Login)
            }

            return when (deepLink) {
                is DeepLink.None -> listOf(Config.Home)
                is DeepLink.Web -> listOf(Config.Home, getConfigForPath(deepLink.path)).distinct()
            }
        }

        private fun getPathForConfig(config: Config): String = when (config) {
            Config.Home -> ""
            Config.Login -> "/$WEB_PATH_LOGIN"
            Config.NotFound -> "/$WEB_PATH_NOT_FOUND"
        }

        private fun getConfigForPath(path: String): Config = when (path.removePrefix("/")) {
            "" -> Config.Home
            WEB_PATH_LOGIN -> Config.Login
            else -> Config.NotFound
        }
    }

    /** A deep link pointing to a page in the app.  */
    sealed interface DeepLink {
        data object None : DeepLink
        class Web(val path: String) : DeepLink
    }

    /** The navigation state config for the root component. */
    @Serializable
    private sealed class Config {
        @Serializable
        data object Home : Config()

        @Serializable
        data object Login : Config()

        @Serializable
        data object NotFound : Config()
    }
}


