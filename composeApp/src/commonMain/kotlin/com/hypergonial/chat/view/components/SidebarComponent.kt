package com.hypergonial.chat.view.components

import androidx.compose.runtime.Composable
import co.touchlab.kermit.Logger
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import com.hypergonial.chat.EffectContainer
import com.hypergonial.chat.containAsEffect
import com.hypergonial.chat.model.ChannelCreateEvent
import com.hypergonial.chat.model.ChannelRemoveEvent
import com.hypergonial.chat.model.Client
import com.hypergonial.chat.model.FocusAssetEvent
import com.hypergonial.chat.model.FocusChannelEvent
import com.hypergonial.chat.model.FocusGuildEvent
import com.hypergonial.chat.model.GuildCreateEvent
import com.hypergonial.chat.model.GuildRemoveEvent
import com.hypergonial.chat.model.GuildUpdateEvent
import com.hypergonial.chat.model.ReadyEvent
import com.hypergonial.chat.model.UserUpdateEvent
import com.hypergonial.chat.model.exceptions.ClientException
import com.hypergonial.chat.model.payloads.Channel
import com.hypergonial.chat.model.payloads.Guild
import com.hypergonial.chat.model.payloads.Snowflake
import com.hypergonial.chat.model.payloads.User
import com.hypergonial.chat.view.content.MainContent
import com.hypergonial.chat.withFallbackValue
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

/**
 * The main sidebar component that is displayed on the left side of the screen. It also contains the main content in a
 * child slot.
 */
interface SidebarComponent : Displayable {
    /** Called when the user clicks the home button */
    fun onHomeSelected()

    /**
     * Called when the user clicks a guild in the sidebar
     *
     * @param guildId The ID of the guild that was clicked
     */
    fun onGuildSelected(guildId: Snowflake)

    /**
     * Called when the user clicks a guild in the sidebar
     *
     * @param guild The guild that was clicked
     */
    fun onGuildSelected(guild: Guild)

    /**
     * Called when the user clicks a channel in the sidebar
     *
     * @param channelId The ID of the channel that was clicked
     */
    fun onChannelSelected(channelId: Snowflake)

    /**
     * Called when the user clicks a channel in the sidebar
     *
     * @param channel The channel that was clicked
     */
    fun onChannelSelected(channel: Channel)

    /**
     * Called when the user clicks the leave guild button
     *
     * @param guildId The ID of the guild to leave
     */
    fun onGuildLeaveClicked(guildId: Snowflake)

    /**
     * Called when the user clicks the delete guild button
     *
     * @param guildId The ID of the guild to delete
     */
    fun onGuildDeleteClicked(guildId: Snowflake)

    /**
     * Called when the user clicks the edit guild button
     *
     * @param guildId The ID of the guild to edit
     */
    fun onGuildEditClicked(guildId: Snowflake)

    /** Called when the user clicks the create guild button */
    fun onGuildCreateClicked()

    /** Called when the user clicks the create channel button */
    fun onChannelCreateClicked()

    /**
     * Called when the user clicks the edit channel button
     *
     * @param channelId The ID of the channel to edit
     */
    fun onChannelEditClicked(channelId: Snowflake)

    /**
     * Called when the user clicks the delete channel button
     *
     * @param channelId The ID of the channel to delete
     */
    fun onChannelDeleteClicked(channelId: Snowflake)

    /** Called when the user clicks the logout button */
    fun onLogoutClicked()

    /** Called when the user closes the asset viewer */
    fun onAssetViewerClosed()

    /** Called when the user clicks the user settings button */
    fun onUserSettingsClicked()

    @Composable override fun Display() = MainContent(this)

    val mainContent: Value<ChildSlot<*, MainContentComponent>>

    val data: Value<SidebarState>

    data class SidebarState(
        /** The currently logged in user May be null if the client is still connecting */
        val currentUser: User? = null,
        /** The guild that is currently selected */
        val selectedGuild: Guild? = null,
        /** The channel that is currently selected */
        val selectedChannel: Channel? = null,
        /** The text to be displayed in the top bar of the app */
        val topBarContent: String = "Chat",
        /** The list of guilds to display in the sidebar */
        val guilds: List<Guild> = emptyList(),
        /** The list of channels to display in the sidebar */
        val channels: List<Channel> = emptyList(),
        /** If true, the app is still connecting to the server */
        val isConnecting: Boolean = true,
        /** The URL of the asset to display in the asset viewer */
        val assetViewerUrl: String? = null,
        /** If true, the asset viewer is active */
        val assetViewerActive: Boolean = false,
        /** The state of the navigation drawer */
        val navDrawerCommand: EffectContainer<NavDrawerCommand> =
            NavDrawerCommand.CLOSE_WITHOUT_ANIMATION.containAsEffect(),
        /** The state of the snackbar */
        val snackbarMessage: EffectContainer<String> = "".containAsEffect(),
    )

    /** The commands that can be sent to the navigation drawer */
    enum class NavDrawerCommand {
        CLOSE,
        OPEN,
        CLOSE_WITHOUT_ANIMATION,
        OPEN_WITHOUT_ANIMATION,
        TOGGLE,
    }
}

/**
 * The default implementation of the sidebar component
 *
 * @param ctx The component context
 * @param client The client to use for API calls
 * @param onGuildCreateRequested The callback to call when the user requests to create a guild
 * @param onChannelCreateRequested The callback to call when the user requests to create a channel
 * @param onUserSettingsRequested The callback to call when the user requests to view their settings
 * @param onLogout The callback to call when the user requests to log out
 */
class DefaultSideBarComponent(
    val ctx: ComponentContext,
    val client: Client,
    val onGuildCreateRequested: () -> Unit,
    val onChannelCreateRequested: (Snowflake) -> Unit,
    val onUserSettingsRequested: () -> Unit,
    val onLogout: () -> Unit,
) : SidebarComponent, ComponentContext by ctx {
    override val data =
        MutableValue(
            SidebarComponent.SidebarState(
                currentUser = client.cache.ownUser,
                guilds = client.cache.guilds.values.toList().sortedBy { it.id },
            )
        )

    private val slotNavigation = SlotNavigation<SlotConfig>()
    private val scope = ctx.coroutineScope()
    private val logger = Logger.withTag("DefaultSideBarComponent")

    override val mainContent: Value<ChildSlot<*, MainContentComponent>> =
        childSlot(
            source = slotNavigation,
            serializer =
                SlotConfig.serializer()
                    .withFallbackValue(SlotConfig.Home(hasGuilds = client.cache.guilds.isNotEmpty())),
            key = "MainContent",
            handleBackButton = false,
            initialConfiguration = { SlotConfig.Home(hasGuilds = client.cache.guilds.isNotEmpty()) },
        ) { config, childCtx ->
            when (config) {
                is SlotConfig.Home -> DefaultHomeComponent(childCtx, client, hasGuilds = config.hasGuilds)
                is SlotConfig.Fallback -> DefaultFallbackMainComponent(childCtx, ::onChannelCreateClicked)
                is SlotConfig.Channel -> DefaultChannelComponent(childCtx, client, config.channelId) { onLogout() }
            }
        }

    init {
        client.eventManager.apply {
            subscribeWithLifeCycle(ctx.lifecycle, ::onReady)
            subscribeWithLifeCycle(ctx.lifecycle, ::onGuildCreate)
            subscribeWithLifeCycle(ctx.lifecycle, ::onGuildUpdate)
            subscribeWithLifeCycle(ctx.lifecycle, ::onGuildRemove)
            subscribeWithLifeCycle(ctx.lifecycle, ::onChannelCreate)
            subscribeWithLifeCycle(ctx.lifecycle, ::onChannelRemove)
            subscribeWithLifeCycle(ctx.lifecycle, ::onChannelFocus)
            subscribeWithLifeCycle(ctx.lifecycle, ::onGuildFocus)
            subscribeWithLifeCycle(ctx.lifecycle, ::onAssetFocus)
            subscribeWithLifeCycle(ctx.lifecycle, ::onUserUpdate)
        }
    }

    /**
     * Returns the default channel for a guild
     *
     * @param guildId The ID of the guild to get the default channel for
     * @return The default channel for the guild
     */
    private fun getDefaultGuildChannel(guildId: Snowflake): Channel? {
        val id = client.cache.getChannelsForGuild(guildId).keys.minOrNull()
        return id?.let { client.cache.getChannel(it) }
    }

    override fun onHomeSelected() {
        data.value = data.value.copy(selectedGuild = null, selectedChannel = null, channels = emptyList())
        slotNavigation.activate(SlotConfig.Home(hasGuilds = data.value.guilds.isNotEmpty()))
    }

    override fun onLogoutClicked() {
        onLogout()
    }

    override fun onGuildSelected(guildId: Snowflake) {
        val guild = client.cache.getGuild(guildId) ?: return
        onGuildSelected(guild)
    }

    override fun onGuildSelected(guild: Guild) {
        if (guild.id == data.value.selectedGuild?.id) {
            return
        }

        // TODO: Update when channels have positions
        val channel = getDefaultGuildChannel(guild.id)

        data.value =
            data.value.copy(
                selectedGuild = guild,
                selectedChannel = channel,
                channels = client.cache.getChannelsForGuild(guild.id).values.toList().sortedBy { it.id },
            )

        if (channel?.id != null) {
            slotNavigation.activate(SlotConfig.Channel(channel.id))
        } else {
            slotNavigation.activate(SlotConfig.Fallback)
        }
    }

    override fun onChannelSelected(channelId: Snowflake) {
        val channel = client.cache.getChannel(channelId) ?: return
        onChannelSelected(channel)
    }

    override fun onChannelSelected(channel: Channel) {
        if (channel.id != data.value.selectedChannel?.id) {
            data.value = data.value.copy(selectedChannel = channel)
            slotNavigation.activate(SlotConfig.Channel(channel.id))
        }

        data.value = data.value.copy(navDrawerCommand = SidebarComponent.NavDrawerCommand.CLOSE.containAsEffect())
    }

    override fun onGuildLeaveClicked(guildId: Snowflake) {
        scope.launch {
            try {
                client.leaveGuild(guildId)
            } catch (e: ClientException) {
                data.value =
                    data.value.copy(snackbarMessage = "Unexpected error occurred: ${e.message}".containAsEffect())
                logger.e { "Failed to leave guild: ${e.message}" }
            }
        }
    }

    override fun onGuildDeleteClicked(guildId: Snowflake) {
        scope.launch {
            try {
                client.deleteGuild(guildId)
            } catch (e: ClientException) {
                data.value =
                    data.value.copy(snackbarMessage = "Unexpected error occurred: ${e.message}".containAsEffect())
                logger.e { "Failed to delete guild: ${e.message}" }
            }
        }
    }

    override fun onGuildEditClicked(guildId: Snowflake) {
        data.value = data.value.copy(snackbarMessage = "Not yet implemented".containAsEffect())
    }

    override fun onAssetViewerClosed() {
        data.value = data.value.copy(assetViewerActive = false)
    }

    private fun onReady(event: ReadyEvent) {
        data.value = data.value.copy(isConnecting = false, currentUser = event.user)
    }

    private fun onGuildCreate(event: GuildCreateEvent) {
        // TODO: Update when guilds have positions saved in prefs
        if (event.guild !in data.value.guilds) {
            data.value = data.value.copy(guilds = (data.value.guilds + event.guild).sortedBy { it.id })
        }

        // Refresh home component if it is active
        if (mainContent.value.child?.configuration is SlotConfig.Home) {
            onHomeSelected()
        }
    }

    private fun onGuildUpdate(event: GuildUpdateEvent) {
        if (event.guild !in data.value.guilds) {
            data.value = data.value.copy(guilds = (data.value.guilds + event.guild).sortedBy { it.id })
        } else {
            data.value =
                data.value.copy(guilds = data.value.guilds.map { if (it.id == event.guild.id) event.guild else it })
        }
    }

    private fun onGuildRemove(event: GuildRemoveEvent) {
        data.value = data.value.copy(guilds = data.value.guilds.filter { it.id != event.guild.id })

        if (event.guild.id == data.value.selectedGuild?.id) {
            onHomeSelected()
        }
        // Refresh home component if it is active
        else if (mainContent.value.child?.configuration is SlotConfig.Home) {
            onHomeSelected()
        }
    }

    private fun onChannelCreate(event: ChannelCreateEvent) {
        if (event.channel.guildId != data.value.selectedGuild?.id) {
            return
        }

        if (event.channel !in data.value.channels) {
            data.value = data.value.copy(channels = (data.value.channels + event.channel).sortedBy { it.id })

            // Leave fallback slot if it was active
            if (data.value.selectedChannel == null || data.value.selectedGuild != null) {
                onChannelSelected(event.channel.id)
            }
        }
    }

    private fun onChannelRemove(event: ChannelRemoveEvent) {
        if (event.channel.guildId != data.value.selectedGuild?.id) {
            return
        }

        if (event.channel.id == data.value.selectedChannel?.id) {
            data.value.selectedGuild?.id?.let { getDefaultGuildChannel(it) }?.let { onChannelSelected(it) }
        }

        data.value = data.value.copy(channels = data.value.channels.filter { it.id != event.channel.id })
    }

    private fun onChannelFocus(event: FocusChannelEvent) {
        if (event.channel in data.value.channels) {
            onChannelSelected(event.channel.id)
        } else if (event.channel.guildId == data.value.selectedGuild?.id) {
            data.value = data.value.copy(channels = (data.value.channels + event.channel).sortedBy { it.id })
            onChannelSelected(event.channel)
        }
        data.value =
            data.value.copy(
                navDrawerCommand = SidebarComponent.NavDrawerCommand.CLOSE_WITHOUT_ANIMATION.containAsEffect()
            )
    }

    private fun onGuildFocus(event: FocusGuildEvent) {
        if (event.guild !in data.value.guilds) {
            data.value = data.value.copy(guilds = (data.value.guilds + event.guild).sortedBy { it.id })
        }
        onGuildSelected(event.guild)
        data.value =
            data.value.copy(
                navDrawerCommand = SidebarComponent.NavDrawerCommand.CLOSE_WITHOUT_ANIMATION.containAsEffect()
            )
    }

    private fun onAssetFocus(event: FocusAssetEvent) {
        data.value = data.value.copy(assetViewerUrl = event.url, assetViewerActive = true)
    }

    private fun onUserUpdate(event: UserUpdateEvent) {
        if (event.user.id != data.value.currentUser?.id) return

        data.value = data.value.copy(currentUser = event.user)
    }

    override fun onGuildCreateClicked() {
        onGuildCreateRequested()
    }

    override fun onChannelCreateClicked() {
        onChannelCreateRequested(data.value.selectedGuild?.id ?: return)
    }

    override fun onChannelEditClicked(channelId: Snowflake) {
        data.value = data.value.copy(snackbarMessage = "Not yet implemented".containAsEffect())
    }

    override fun onChannelDeleteClicked(channelId: Snowflake) {
        scope.launch {
            try {
                client.deleteChannel(channelId)
            } catch (e: ClientException) {
                data.value =
                    data.value.copy(snackbarMessage = "Unexpected error occurred: ${e.message}".containAsEffect())
                logger.e { "Failed to delete channel: ${e.message}" }
            }
        }
    }

    override fun onUserSettingsClicked() {
        onUserSettingsRequested()
    }

    @Serializable
    private sealed class SlotConfig {
        /** Shown when the user has not selected a guild. */
        @Serializable data class Home(val hasGuilds: Boolean) : SlotConfig()

        /** Shown when the user has selected a guild but that guild has no channel. */
        @Serializable data object Fallback : SlotConfig()

        /** Shown when the user has selected a channel in a guild. */
        @Serializable data class Channel(val channelId: Snowflake) : SlotConfig()
    }
}
