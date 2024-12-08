package com.hypergonial.chat.components

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.popTo
import com.arkivanov.decompose.router.stack.webhistory.WebHistoryController
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackHandlerOwner
import kotlinx.serialization.Serializable

/** The navigation state config for the root component. */
@Serializable
private sealed interface Config {
    @Serializable
    data object Root : Config
    @Serializable
    data object Home : Config
    @Serializable
    data object Login : Config
    @Serializable
    data object NotFound : Config
}



interface RootComponent : BackHandlerOwner {
    val data: Value<RootData>

    fun onBackClicked()
    fun onBackClicked(toIndex: Int)

    data class RootData(
        val isLoggedIn: Boolean = false,
    )

    sealed class Child {
        class LoginChild(val component: LoginComponent) : Child()
        class HomeChild(val component: HomeComponent) : Child()
        class NotFoundChild(val component: NotFoundComponent) : Child()
    }
}

@OptIn(ExperimentalDecomposeApi::class)
class DefaultRootComponent(
    ctx: ComponentContext,
    deepLink: DeepLink = DeepLink.None,
    webHistoryController: WebHistoryController? = null
) : RootComponent, ComponentContext by ctx {
    private val nav = StackNavigation<Config>()

    private val stack = childStack(
        source = nav,
        serializer = Config.serializer(),
        initialStack = { getInitialStack(webHistoryPaths = webHistoryController?.historyPaths, deepLink = deepLink) },
        childFactory = ::child,
    )

    override val data = MutableValue(RootComponent.RootData())

    init {
        // Attach the web history controller to the navigator.
        webHistoryController?.attach(
            navigator = nav,
            serializer = Config.serializer(),
            stack = stack,
            getPath = ::getPathForConfig,
            getConfiguration = ::getConfigForPath,
        )
    }

    /** The child factory for the root component's childStack. */
    private fun child(config: Config, componentContext: ComponentContext): RootComponent.Child = when (config) {
        is Config.Login -> RootComponent.Child.LoginChild(
            DefaultLoginComponent(
                ctx = componentContext,
                onLoginComplete = {
                    data.value = data.value.copy(isLoggedIn = true)
                }
            )
        )

        Config.NotFound -> RootComponent.Child.NotFoundChild(
            DefaultNotFoundComponent(
                ctx = componentContext,
            )
        )
        Config.Home -> RootComponent.Child.HomeChild(
            DefaultHomeComponent(
                ctx = componentContext,
            )
        )

        /* TODO: Handle not being logged in */
        /* NOTE: You cannot access data.value because for some god forsaken reason it is null at app startup */
        Config.Root -> RootComponent.Child.HomeChild(
            DefaultHomeComponent(
                ctx = componentContext,
            )
        )
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
        private const val WEB_PATH_HOME = "home"

        private fun getInitialStack(webHistoryPaths: List<String>?, deepLink: DeepLink): List<Config> =
            webHistoryPaths?.takeUnless(List<*>::isEmpty)?.map(::getConfigForPath) ?: getInitialStack(deepLink)

        private fun getInitialStack(deepLink: DeepLink): List<Config> = when (deepLink) {
            is DeepLink.None -> listOf(Config.Root)
            is DeepLink.Web -> listOf(Config.Root, getConfigForPath(deepLink.path)).distinct()
        }

        private fun getPathForConfig(config: Config): String = when (config) {
            Config.Root -> ""
            Config.Home -> "/$WEB_PATH_HOME"
            Config.Login -> "/$WEB_PATH_LOGIN"
            Config.NotFound -> "/$WEB_PATH_NOT_FOUND"
        }

        private fun getConfigForPath(path: String): Config = when (path.removePrefix("/")) {
            WEB_PATH_LOGIN -> Config.Login
            "" -> Config.Root
            else -> Config.NotFound
        }
    }

    /** A deep link pointing to a page in the app.  */
    sealed interface DeepLink {
        data object None : DeepLink
        class Web(val path: String) : DeepLink
    }
}


