package com.hypergonial.chat.view

import co.touchlab.kermit.Logger
import com.hypergonial.chat.PlatformType
import com.hypergonial.chat.platform
import com.mmk.kmpnotifier.notification.NotifierBuilder
import com.mmk.kmpnotifier.notification.NotifierManager
import org.freedesktop.dbus.TypeRef
import org.freedesktop.dbus.annotations.DBusInterfaceName
import org.freedesktop.dbus.annotations.DBusMemberName
import org.freedesktop.dbus.annotations.DBusProperty
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder
import org.freedesktop.dbus.interfaces.DBusInterface
import org.freedesktop.dbus.messages.DBusSignal
import org.freedesktop.dbus.types.UInt32
import org.freedesktop.dbus.types.Variant

actual fun sendNotification(builder: NotifierBuilder.() -> Unit) {
    if (platform.platformType != PlatformType.LINUX) {
        NotifierManager.getLocalNotifier().notify(builder)
    } else {
        notifyViaXDGPortals(builder)
    }
}

@DBusProperty(
    name = "SupportedOptions",
    type = Notification.PropertySupportedOptionsType::class,
    access = DBusProperty.Access.READ,
)
@DBusProperty(name = "version", type = UInt32::class, access = DBusProperty.Access.READ)
@DBusInterfaceName("org.freedesktop.portal.Notification")
interface Notification : DBusInterface {
    // Note: This must be a MutableMap<K, V>, not a Map<K, V> due to a Kotlin-JVM interoperability issue
    // See: https://github.com/hypfvieh/dbus-java/issues/233
    @DBusMemberName("AddNotification") fun addNotification(id: String, notification: MutableMap<String, Variant<*>>)

    @DBusMemberName("RemoveNotification") fun removeNotification(id: String)

    @Suppress("ConstructorParameterNaming")
    class ActionInvoked(_path: String, val id: String, val action: String, val parameter: List<Variant<*>>) :
        DBusSignal(_path, id, action, parameter)

    interface PropertySupportedOptionsType : TypeRef<MutableMap<String?, Variant<*>?>?>
}

@Suppress("TooGenericExceptionCaught")
fun notifyViaXDGPortals(scope: NotifierBuilder.() -> Unit) {
    val builder = NotifierBuilder().apply(scope)

    try {
        System.setProperty("org.freedesktop.dbus.debug", "true")
        val connection = DBusConnectionBuilder.forSessionBus().build()

        val notificationService =
            connection.getRemoteObject(
                "org.freedesktop.portal.Desktop",
                "/org/freedesktop/portal/desktop",
                Notification::class.java,
            )

        val notification = HashMap<String, Variant<*>>()

        notification["title"] = Variant(builder.title)
        notification["body"] = Variant(builder.body)
        // TODO: Figure out how to set a custom icon
        // https://flatpak.github.io/xdg-desktop-portal/docs/doc-org.freedesktop.portal.Notification.html#
        notification["icon"] = Variant("internet-chat")
        notification["priority"] = Variant("normal")

        notificationService.addNotification(builder.id.toString(), notification)
        connection.disconnect()
    } catch (e: Exception) {
        Logger.withTag("Notifications").w { "Failed to send notification via XDG Portal: $e" }
    }
}
