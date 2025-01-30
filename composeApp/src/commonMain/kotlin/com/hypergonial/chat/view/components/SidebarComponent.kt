package com.hypergonial.chat.view.components

import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import com.arkivanov.essenty.lifecycle.doOnPause
import com.arkivanov.essenty.lifecycle.doOnResume
import com.hypergonial.chat.model.ChannelCreateEvent
import com.hypergonial.chat.model.ChannelRemoveEvent
import com.hypergonial.chat.model.Client
import com.hypergonial.chat.model.FocusChannelEvent
import com.hypergonial.chat.model.FocusGuildEvent
import com.hypergonial.chat.model.GuildCreateEvent
import com.hypergonial.chat.model.GuildRemoveEvent
import com.hypergonial.chat.model.GuildUpdateEvent
import com.hypergonial.chat.model.ReadyEvent
import com.hypergonial.chat.model.SessionInvalidatedEvent
import com.hypergonial.chat.model.payloads.Channel
import com.hypergonial.chat.model.payloads.Guild
import com.hypergonial.chat.model.payloads.Snowflake
import com.hypergonial.chat.view.content.MainContent
import com.hypergonial.chat.withFallbackValue
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable

/** The main sidebar component that is displayed on the left side of the screen.
 * It also contains the main content in a child slot. */
interface SidebarComponent : Displayable {
    fun onHomeSelected()
    fun onGuildSelected(guildId: Snowflake)
    fun onGuildSelected(guild: Guild)
    fun onChannelSelected(channelId: Snowflake)
    fun onChannelSelected(channel: Channel)
    fun onGuildCreateClicked()
    fun onChannelCreateClicked()
    fun onLogoutClicked()

    @Composable
    override fun Display() = MainContent(this)

    val mainContent: Value<ChildSlot<*, MainContentComponent>>

    val data: Value<SidebarState>

    data class SidebarState(
        val selectedGuild: Guild? = null,
        val selectedChannel: Channel? = null,
        val topBarContent: String = "Chat",
        val guilds: List<Guild> = emptyList(),
        val channels: List<Channel> = emptyList(),
        val isConnecting: Boolean = true,
        // The state of the navigation drawer
        val navDrawerState: DrawerState = DrawerState(DrawerValue.Closed),
    )
}

class DefaultSideBarComponent(
    val ctx: ComponentContext,
    val client: Client,
    val onGuildCreateRequested: () -> Unit,
    val onChannelCreateRequested: (Snowflake) -> Unit,
    val onLogout: () -> Unit
) : SidebarComponent, ComponentContext by ctx {
    override val data = MutableValue(
        SidebarComponent.SidebarState(guilds = client.cache.guilds.values.toList()
            .sortedBy { it.id })
    )

    private val slotNavigation = SlotNavigation<SlotConfig>()

    override val mainContent: Value<ChildSlot<*, MainContentComponent>> =
        childSlot(source = slotNavigation,
            serializer = SlotConfig.serializer().withFallbackValue(SlotConfig.Home),
            key = "MainContent",
            handleBackButton = false,
            initialConfiguration = { SlotConfig.Home }) { config, childCtx ->
            when (config) {
                is SlotConfig.Home -> DefaultHomeComponent(childCtx)
                is SlotConfig.Fallback -> DefaultFallbackMainComponent(
                    childCtx,
                    ::onChannelCreateClicked
                )

                is SlotConfig.Channel -> DefaultChannelComponent(
                    childCtx, client, config.channelId
                ) { onLogout() }
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
        }
    }

    private fun getDefaultGuildChannel(guildId: Snowflake): Channel? {
        val id = client.cache.getChannelsForGuild(guildId).keys.minOrNull()
        return id?.let { client.cache.getChannel(it) }
    }

    override fun onHomeSelected() {
        data.value =
            data.value.copy(selectedGuild = null, selectedChannel = null, channels = emptyList())
        slotNavigation.activate(SlotConfig.Home)
    }

    override fun onLogoutClicked() {
        onLogout()
    }

    override fun onGuildSelected(guildId: Snowflake) {
        val guild = client.cache.getGuild(guildId) ?: return
        onGuildSelected(guild)
    }

    override fun onGuildSelected(guild: Guild) {
        // TODO: Update when channels have positions
        val channel = getDefaultGuildChannel(guild.id)

        data.value = data.value.copy(selectedGuild = guild,
            selectedChannel = channel,
            channels = client.cache.getChannelsForGuild(guild.id).values.toList().sortedBy { it.id })

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
        data.value = data.value.copy(selectedChannel = channel)
        slotNavigation.activate(SlotConfig.Channel(channel.id))
    }

    private fun onReady(event: ReadyEvent) {
        data.value = data.value.copy(isConnecting = false)
    }

    private fun onGuildCreate(event: GuildCreateEvent) {
        // TODO: Update when guilds have positions saved in prefs
        if (event.guild !in data.value.guilds) {
            data.value =
                data.value.copy(guilds = (data.value.guilds + event.guild).sortedBy { it.id })
        }
    }

    private fun onGuildUpdate(event: GuildUpdateEvent) {
        if (event.guild !in data.value.guilds) {
            data.value = data.value.copy(
                guilds = (data.value.guilds + event.guild).sortedBy { it.id },
            )
        } else {
            data.value =
                data.value.copy(guilds = data.value.guilds.map { if (it.id == event.guild.id) event.guild else it })
        }
    }

    private fun onGuildRemove(event: GuildRemoveEvent) {
        data.value = data.value.copy(guilds = data.value.guilds.filter { it.id != event.guild.id })
    }

    private fun onChannelCreate(event: ChannelCreateEvent) {
        if (event.channel.guildId != data.value.selectedGuild?.id) {
            return
        }

        if (event.channel !in data.value.channels) {
            data.value = data.value.copy(
                channels = (data.value.channels + event.channel).sortedBy { it.id },
            )

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

        if (event.channel == data.value.selectedChannel) {
            data.value = data.value.copy(selectedChannel = getDefaultGuildChannel(event.channel.guildId))
        }

        data.value =
            data.value.copy(channels = data.value.channels.filter { it.id != event.channel.id })
    }

    private fun onChannelFocus(event: FocusChannelEvent) {
        if (event.channel in data.value.channels) {
            onChannelSelected(event.channel.id)
        } else if (event.channel.guildId == data.value.selectedGuild?.id) {
            data.value = data.value.copy(
                channels = (data.value.channels + event.channel).sortedBy { it.id },
            )
            onChannelSelected(event.channel)
        }
        data.value = data.value.copy(navDrawerState = DrawerState(DrawerValue.Closed))
    }

    private fun onGuildFocus(event: FocusGuildEvent) {
        if (event.guild !in data.value.guilds) {
            data.value = data.value.copy(
                guilds = (data.value.guilds + event.guild).sortedBy { it.id },
            )
        }
        onGuildSelected(event.guild)
        data.value = data.value.copy(navDrawerState = DrawerState(DrawerValue.Closed))
    }

    override fun onGuildCreateClicked() {
        onGuildCreateRequested()
    }

    override fun onChannelCreateClicked() {
        onChannelCreateRequested(data.value.selectedGuild?.id ?: return)
    }

    @Serializable
    private sealed class SlotConfig {
        /** Shown when the user has not selected a guild. */
        @Serializable
        data object Home : SlotConfig()

        /** Shown when the user has selected a guild but that guild has no channel. */
        @Serializable
        data object Fallback : SlotConfig()

        /** Shown when the user has selected a channel in a guild. */
        @Serializable
        data class Channel(val channelId: Snowflake) : SlotConfig()
    }
}
