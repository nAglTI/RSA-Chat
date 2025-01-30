package com.hypergonial.chat.view.components

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.popTo
import com.arkivanov.decompose.router.stack.popWhile
import com.arkivanov.decompose.router.stack.pushNew
import com.arkivanov.decompose.router.stack.replaceAll
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackHandlerOwner
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import com.arkivanov.essenty.lifecycle.doOnPause
import com.arkivanov.essenty.lifecycle.doOnResume
import com.hypergonial.chat.model.ChatClient
import com.hypergonial.chat.model.Client
import com.hypergonial.chat.model.FocusChannelEvent
import com.hypergonial.chat.model.FocusGuildEvent
import com.hypergonial.chat.model.SessionInvalidatedEvent
import com.hypergonial.chat.model.payloads.Snowflake
import com.hypergonial.chat.platform
import com.hypergonial.chat.view.components.prompts.CreateChannelComponent
import com.hypergonial.chat.view.components.prompts.CreateGuildComponent
import com.hypergonial.chat.view.components.prompts.DefaultCreateChannelComponent
import com.hypergonial.chat.view.components.prompts.DefaultCreateGuildComponent
import com.hypergonial.chat.view.components.prompts.DefaultJoinGuildComponent
import com.hypergonial.chat.view.components.prompts.DefaultNewGuildComponent
import com.hypergonial.chat.view.components.prompts.JoinGuildComponent
import com.hypergonial.chat.view.components.prompts.NewGuildComponent
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


interface RootComponent : BackHandlerOwner {
    val stack: Value<ChildStack<*, Child>>


    fun onBackClicked()
    fun onBackClicked(toIndex: Int)

    sealed class Child(open val component: Displayable) {
        class LoginChild(override val component: LoginComponent) : Child(component)
        class RegisterChild(override val component: RegisterComponent) : Child(component)
        class DebugSettingsChild(override val component: DebugSettingsComponent) : Child(component)
        class MainChild(override val component: SidebarComponent) : Child(component)
        class NotFoundChild(override val component: NotFoundComponent) : Child(component)
        class NewGuildChild(override val component: NewGuildComponent) : Child(component)
        class CreateGuildChild(override val component: CreateGuildComponent) : Child(component)
        class JoinGuildChild(override val component: JoinGuildComponent) : Child(component)
        class CreateChannelChild(override val component: CreateChannelComponent) : Child(component)
    }
}

class DefaultRootComponent(
    val ctx: ComponentContext,
) : RootComponent, ComponentContext by ctx {

    private val scope = ctx.coroutineScope()

    private val client: Client = retainedInstance { ChatClient(scope) }

    private val nav = StackNavigation<Config>()

    private val logger = KotlinLogging.logger {}

    private val _stack = childStack(
        source = nav,
        serializer = null,
        handleBackButton = true,
        initialStack = { getInitialStack(isLoggedIn = client.isLoggedIn()) },
        childFactory = ::child,
    )

    override val stack: Value<ChildStack<*, RootComponent.Child>> = _stack

    init {
        client.eventManager.subscribeWithLifeCycle(ctx.lifecycle, ::onSessionInvalidated)
        // If we are already logged in, connect to the gateway
        scope.launch {
            if (client.isLoggedIn()) client.connect()
        }

        if (platform.platformType.needsToSuspendClient()) {
            manageClientLifecycle()
        }
    }

    private fun manageClientLifecycle() {
        ctx.lifecycle.doOnResume {
            // Ignore resume events fired on startup
            if (!client.isSuspended) {
                return@doOnResume
            }

            // The scope may not survive configuration changes, so we may need to replace it
            if (!client.scope.isActive) {
                client.replaceScope(ctx.coroutineScope())
            }

            scope.launch {
                client.resume()
            }
        }

        ctx.lifecycle.doOnPause {
            client.pause()
        }
    }

    /** The child factory for the root component's childStack. */
    private fun child(config: Config, childCtx: ComponentContext): RootComponent.Child =
        when (config) {
            is Config.Login -> RootComponent.Child.LoginChild(
                DefaultLoginComponent(
                    ctx = childCtx,
                    client = client,
                    onLogin = { onLoginComplete(); nav.replaceAll(Config.Main) },
                    onRegisterRequest = { nav.pushNew(Config.Register) },
                    onDebugSettingsOpen = { nav.pushNew(Config.DebugSettings) }
                ),
            )

            is Config.Register -> RootComponent.Child.RegisterChild(
                DefaultRegisterComponent(
                    ctx = childCtx,
                    client = client,
                    onRegister = { nav.replaceAll(Config.Login) },
                    onBack = { nav.pop() }
                ),
            )

            is Config.DebugSettings -> RootComponent.Child.DebugSettingsChild(
                DefaultDebugSettingsComponent(
                    ctx = childCtx,
                    client = client,
                    onBack = { client.reloadConfig(); nav.pop() }
                )
            )

            is Config.NotFound -> RootComponent.Child.NotFoundChild(
                DefaultNotFoundComponent(ctx = childCtx)
            )

            is Config.Main -> RootComponent.Child.MainChild(
                DefaultSideBarComponent(
                    ctx = childCtx,
                    client = client,
                    onGuildCreateRequested = { nav.pushNew(Config.NewGuild) },
                    onChannelCreateRequested = { nav.pushNew(Config.CreateChannel(it)) },
                    onLogout = ::onLogout
                )
            )

            is Config.NewGuild -> RootComponent.Child.NewGuildChild(
                DefaultNewGuildComponent(
                    ctx = childCtx,
                    onCreateRequested = { nav.pushNew(Config.CreateGuild) },
                    onJoinRequested = { nav.pushNew(Config.JoinGuild) },
                    onCancel = { nav.pop() }
                )
            )

            is Config.CreateGuild -> RootComponent.Child.CreateGuildChild(
                DefaultCreateGuildComponent(
                    ctx = childCtx,
                    client = client,
                    onCreated = {
                        scope.launch {
                            nav.popWhile { it !is Config.Main }
                            client.eventManager.dispatch(FocusGuildEvent(it))
                        }
                    },
                    onCancel = { nav.pop() }
                )
            )

            is Config.JoinGuild -> RootComponent.Child.JoinGuildChild(
                DefaultJoinGuildComponent(
                    ctx = childCtx,
                    client = client,
                    onJoined = {
                        scope.launch {
                            nav.popWhile { it !is Config.Main }
                            val guild = client.cache.getGuild(it.guildId) ?: client.fetchGuild(it.guildId)
                            client.eventManager.dispatch(FocusGuildEvent(guild))
                        }
                    },
                    onCancel = { nav.pop() }
                )
            )

            is Config.CreateChannel -> RootComponent.Child.CreateChannelChild(
                DefaultCreateChannelComponent(
                    ctx = childCtx,
                    client = client,
                    guildId = config.guildId,
                    onCreated = {
                        scope.launch {
                            nav.pop()
                            client.eventManager.dispatch(FocusChannelEvent(it))
                        }
                    },
                    onCancel = { nav.pop() }
                )
            )
        }

    private fun onLoginComplete() {
        scope.launch {
            if (client.isLoggedIn()) {
                client.connect()
            }
        }
    }

    private fun onLogout() {
        client.closeGateway()
        client.logout()
        nav.replaceAll(Config.Login)
    }

    private fun onSessionInvalidated(event: SessionInvalidatedEvent) {
        // If the gateway session dropped while we were not suspended,
        // assume the worst has happened, and log out the user.
        if (!client.isSuspended) {
            onLogout()
        }
    }

    override fun onBackClicked() {
        nav.pop()
    }

    override fun onBackClicked(toIndex: Int) {
        nav.popTo(index = toIndex)
    }

    /** Handling for web paths. */
    private companion object {

        private fun getInitialStack(isLoggedIn: Boolean): List<Config> {
            if (!isLoggedIn) {
                return listOf(Config.Login)
            }

            return listOf(Config.Main)
        }
    }

    /** The navigation state config for the root component. */
    @Serializable
    private sealed class Config {
        @Serializable
        @SerialName("MAIN")
        data object Main : Config()

        @Serializable
        @SerialName("LOGIN")
        data object Login : Config()

        @Serializable
        @SerialName("REGISTER")
        data object Register : Config()

        @Serializable
        @SerialName("NOT_FOUND")
        data object NotFound : Config()

        @Serializable
        @SerialName("DEBUG_SETTINGS")
        data object DebugSettings : Config()

        @Serializable
        @SerialName("NEW_GUILD")
        data object NewGuild : Config()

        @Serializable
        @SerialName("CREATE_GUILD")
        data object CreateGuild : Config()

        @Serializable
        @SerialName("JOIN_GUILD")
        data object JoinGuild : Config()

        @Serializable
        @SerialName("CREATE_CHANNEL")
        data class CreateChannel(val guildId: Snowflake) : Config()
    }
}
