package com.hypergonial.chat.view.components

import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import com.hypergonial.chat.model.ChannelCreateEvent
import com.hypergonial.chat.model.Client
import com.hypergonial.chat.model.FocusChannelEvent
import com.hypergonial.chat.model.FocusGuildEvent
import com.hypergonial.chat.model.GuildCreateEvent
import com.hypergonial.chat.model.ReadyEvent
import com.hypergonial.chat.model.payloads.Channel
import com.hypergonial.chat.model.payloads.Guild
import com.hypergonial.chat.model.payloads.Snowflake
import com.hypergonial.chat.withFallbackValue
import kotlinx.serialization.Serializable

/** The main sidebar component that is displayed on the left side of the screen.
 * It also contains the main content in a child slot. */
interface SidebarComponent {
    fun onHomeSelected()
    fun onGuildSelected(guildId: Snowflake)
    fun onChannelSelected(channelId: Snowflake)
    fun onGuildCreateClicked()
    fun onChannelCreateClicked()
    fun onLogoutClicked()

    val mainContent: Value<ChildSlot<*, MainContentComponent>>

    val data: Value<SidebarState>

    data class SidebarState(
        val selectedGuild: Snowflake? = null,
        val selectedChannel: Snowflake? = null,
        val topBarContent: String = "Chat",
        val guilds: List<Guild> = emptyList(),
        val channels: List<Channel> = emptyList(),
        val isConnecting: Boolean = true,
        // The state of the navigation drawer
        val navDrawerState: DrawerState = DrawerState(DrawerValue.Closed)
    )
}

class DefaultSideBarComponent(
    val ctx: ComponentContext,
    val client: Client,
    val onGuildCreateRequested: () -> Unit,
    val onChannelCreateRequested: (Snowflake) -> Unit,
    val onLogout: () -> Unit
) : SidebarComponent, ComponentContext by ctx {
    private val scope = ctx.coroutineScope()

    override val data = MutableValue(
        SidebarComponent.SidebarState(
            guilds = client.cache.guilds.values.toList().sortedBy { it.id })
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
                is SlotConfig.Fallback -> DefaultFallbackMainComponent(childCtx) { onChannelCreateClicked() }
                is SlotConfig.Channel -> DefaultChannelComponent(
                    childCtx, client, config.channelId
                ) {}
            }
        }

    init {
        client.eventManager.subscribeWithLifeCycle(ctx.lifecycle, ::onReady)
        client.eventManager.subscribeWithLifeCycle(ctx.lifecycle, ::onGuildCreate)
        client.eventManager.subscribeWithLifeCycle(ctx.lifecycle, ::onChannelCreate)
    }

    override fun onHomeSelected() {
        slotNavigation.activate(SlotConfig.Home)
    }

    override fun onLogoutClicked() {
        onLogout()
    }

    override fun onGuildSelected(guildId: Snowflake) {
        // TODO: Update when channels have positions
        val firstChannelId = client.cache.getChannelsForGuild(guildId).keys.minOrNull()

        data.value = data.value.copy(selectedGuild = guildId,
            selectedChannel = firstChannelId,
            channels = client.cache.getChannelsForGuild(guildId).values.toList().sortedBy { it.id })

        if (firstChannelId != null) {
            slotNavigation.activate(SlotConfig.Channel(firstChannelId))
        } else {
            slotNavigation.activate(SlotConfig.Fallback)
        }
    }

    override fun onChannelSelected(channelId: Snowflake) {
        data.value = data.value.copy(selectedChannel = channelId)
        slotNavigation.activate(SlotConfig.Channel(channelId))
    }

    private suspend fun onReady(event: ReadyEvent) {
        data.value = data.value.copy(isConnecting = false)
    }

    private suspend fun onGuildCreate(event: GuildCreateEvent) {
        // TODO: Update when guilds have positions saved in prefs
        if (event.guild !in data.value.guilds) {
            data.value = data.value.copy(guilds = (data.value.guilds + event.guild).sortedBy { it.id })
        }

    }

    private suspend fun onChannelCreate(event: ChannelCreateEvent) {
        if (event.channel.guildId != data.value.selectedGuild) {
            return
        }

        if (event.channel !in data.value.channels) {
            data.value = data.value.copy(
                channels = (data.value.channels + event.channel).sortedBy { it.id },
            )
        }
    }

    private suspend fun onChannelFocus(event: FocusChannelEvent) {
        if (event.channel in data.value.channels) {
            onChannelSelected(event.channel.id)
        }
        else if (event.channel.guildId == data.value.selectedGuild) {
            data.value = data.value.copy(
                channels = (data.value.channels + event.channel).sortedBy { it.id },
            )
            onChannelSelected(event.channel.id)
        }
    }

    private suspend fun onGuildFocus(event: FocusGuildEvent) {
        if (event.guild !in data.value.guilds) {
            data.value = data.value.copy(
                guilds = (data.value.guilds + event.guild).sortedBy { it.id },
            )
        }
        onGuildSelected(event.guild.id)
    }

    override fun onGuildCreateClicked() {
        onGuildCreateRequested()
    }

    override fun onChannelCreateClicked() {
        onChannelCreateRequested(data.value.selectedGuild ?: return)
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
