package com.hypergonial.chat.model

import com.arkivanov.essenty.lifecycle.Lifecycle
import com.arkivanov.essenty.lifecycle.doOnDestroy
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.reflect.KClass

/** A wrapper for a suspend subscribe callback. */
private class EventSubscriber<EventT : Event>(val callback: suspend (EventT) -> Unit) {
    suspend fun invoke(event: EventT) {
        callback(event)
    }

    override fun equals(other: Any?): Boolean {
        if (other !is EventSubscriber<*>) {
            return false
        }

        return callback == other.callback
    }

    override fun hashCode(): Int {
        return callback.hashCode()
    }
}

/** A type that has an event manager. */
interface EventManagerAware {
    val eventManager: EventManager
}

/** A class for handling event subscriptions and dispatching. */
class EventManager {
    private val subscribers: MutableMap<KClass<out Event>, MutableList<EventSubscriber<out Event>>> =
        mutableMapOf()

    /** Add a subscriber to this event manager.
     * The provided callback will be called when an event of that type is dispatched.
     *
     * @param callback The callback to call when an event of the given type is dispatched.
     * It will be called with the event as the only parameter.
     */
    inline fun <reified T : Event> subscribe(noinline callback: suspend (T) -> Unit) {
        subscribe(T::class, callback)
    }

    /** Add a subscriber to this event manager in a lifecycle-aware way.
     * The provided callback will be called when an event of that type is dispatched.
     *
     * @param callback The callback to call when an event of the given type is dispatched.
     * It will be called with the event as the only parameter.
     * @param lifecycle The lifecycle to unsubscribe the callback on.
     */
    inline fun <reified T : Event> subscribeWithLifeCycle(
        lifecycle: Lifecycle,
        noinline callback: suspend (T) -> Unit
    ) {
        subscribe(T::class, callback)
        lifecycle.doOnDestroy { unsubscribe(T::class, callback) }
    }

    /**
     * Add a subscriber to this event manager.
     * The provided callback will be called when an event of that type is dispatched.
     * The eventType parameter may be omitted if the type can be inferred from the callback.
     *
     * @param eventType The type of event to subscribe to.
     * @param callback The callback to call when an event of the given type is dispatched.
     * It will be called with the event as the only parameter.
     */
    fun <T : Event> subscribe(eventType: KClass<T>, callback: suspend (T) -> Unit) {
        val eventSubscribers = subscribers.getOrPut(eventType) { mutableListOf() }
        eventSubscribers.add(EventSubscriber(callback))
    }

    /**
     * Remove a subscriber from this event manager.
     * The provided callback will no longer be called when an event of that type is dispatched.
     * If the subscriber was not subscribed to the given event type, does nothing.
     *
     * @param callback The callback to remove from the subscribers list.
     */
    inline fun <reified T : Event> unsubscribe(noinline callback: suspend (T) -> Unit) {
        unsubscribe(T::class, callback)
    }

    /**
     * Remove a subscriber from this event manager.
     * The provided callback will no longer be called when an event of that type is dispatched.
     * If the subscriber was not subscribed to the given event type, does nothing.
     *
     * @param eventType The type of event to unsubscribe from.
     * @param callback The callback to remove from the subscribers list.
     */
    fun <T : Event> unsubscribe(eventType: KClass<T>, callback: suspend (T) -> Unit) {
        val eventSubscribers = subscribers[eventType]
        eventSubscribers?.retainAll { it != EventSubscriber(callback) }
    }

    /**
     * Get all subscribers for the given event type.
     * This resolves the subscribers for the given type.
     *
     * @param eventType The type of event to get subscribers for.
     *
     * @return A list of all subscribers for the given event type.
     */
    private fun <T : Event> getSubscribers(eventType: KClass<T>): List<EventSubscriber<out Event>> {
        return this.subscribers[eventType] ?: emptyList()
    }

    /**
     * Wait for an event of the given type to be dispatched.
     * This will suspend the coroutine until an event of the given type is dispatched.
     *
     * @param eventType The type of event to wait for.
     * @param predicate An optional predicate to filter the events.
     *
     * @return The event that was dispatched.
     */
    suspend fun <EventT : Event> waitFor(
        eventType: KClass<EventT>,
        predicate: ((EventT) -> Boolean)? = null
    ): EventT {
        var callback: ((EventT) -> Unit)? = null
        val result = suspendCancellableCoroutine { continuation ->
            callback = { event ->
                if (predicate == null || predicate(event)) {
                    continuation.resumeWith(Result.success(event))
                }
            }
            subscribe(eventType, callback!!)
            continuation.invokeOnCancellation {
                callback?.let { unsubscribe(eventType, it) }
            }
        }
        callback?.let { unsubscribe(eventType, it) }
        return result
    }

    /**
     * Dispatch the given event to all current subscribers of the event type.
     *
     * @param event The event to dispatch.
     */
    @Suppress("UNCHECKED_CAST")
    suspend fun dispatch(event: Event) = coroutineScope {
        val eventSubscribers = getSubscribers(event::class)
        eventSubscribers.forEach {
            val subscriber = it as EventSubscriber<Event>
            launch {
                subscriber.invoke(event)
            }
        }
    }
}
