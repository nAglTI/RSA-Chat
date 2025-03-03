package com.hypergonial.chat.view.colors

import androidx.compose.ui.graphics.Color
import co.touchlab.kermit.Logger
import com.hypergonial.chat.PlatformType
import com.hypergonial.chat.platform
import org.freedesktop.dbus.annotations.DBusInterfaceName
import org.freedesktop.dbus.annotations.DBusMemberName
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder
import org.freedesktop.dbus.interfaces.DBusInterface
import org.freedesktop.dbus.types.Variant

@DBusInterfaceName("org.freedesktop.portal.Settings")
interface Settings : DBusInterface {
    @DBusMemberName("ReadOne") fun readOne(namespace: String, key: String): Variant<*>
}

object LinuxColorProvider : ColorProvider {
    private val logger = Logger.withTag("LinuxColorProvider")

    @Suppress("TooGenericExceptionCaught")
    override fun getAccentColorOfOS(): Color? {
        if (platform.platformType != PlatformType.LINUX) {
            return null
        }

        val value =
            try {
                val connection = DBusConnectionBuilder.forSessionBus().build()
                val settings =
                    connection.getRemoteObject(
                        "org.freedesktop.portal.Desktop",
                        "/org/freedesktop/portal/desktop",
                        Settings::class.java,
                    )
                val res = settings.readOne("org.freedesktop.appearance", "accent-color")
                connection.disconnect()

                res.value
            } catch (e: Exception) {
                logger.w { "Failed to get accent color from XDG Desktop Portal: $e" }
                return null
            }

        if (value !is Array<*>) {
            return null
        }

        val r = value[0] as? Double ?: return null
        val g = value[1] as? Double ?: return null
        val b = value[2] as? Double ?: return null

        if (r !in 0.0..1.0 || g !in 0.0..1.0 || b !in 0.0..1.0) {
            return null
        }

        return Color(r.toFloat(), g.toFloat(), b.toFloat())
    }
}
