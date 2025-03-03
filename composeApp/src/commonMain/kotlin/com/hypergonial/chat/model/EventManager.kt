package com.hypergonial.chat.model

import co.touchlab.kermit.Logger
import com.arkivanov.essenty.lifecycle.Lifecycle
import com.arkivanov.essenty.lifecycle.doOnDestroy
import kotlin.reflect.KClass
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** A type that has an event manager. */
interface EventManagerAware {
    val eventManager: EventManager
}

// Opt-In annotation for internal event manager API
@RequiresOptIn(level = RequiresOptIn.Level.WARNING, message = "This API is for internal use only.")
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class InternalEventManagerApi

/**
 * A wrapper for a suspend subscribe callback.
 *
 * @param callback The callback to call when an event of the given type is dispatched.
 */
private class EventSubscriber<EventT : Event>(val callback: suspend (EventT) -> Unit) {
    suspend fun invoke(event: EventT) = callback(event)

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

/** Instructions for the event manager actor. */
private sealed class Instruction {
    /** Subscribe to an event type with the provided subscriber. */
    class Subscribe(val event: KClass<out Event>, val subscriber: EventSubscriber<out Event>) : Instruction()

    /**
     * Subscribe to the internal event queue. This event queue is guaranteed to receive events before the "public"
     * queue.
     */
    class SubscribeInternal(val event: KClass<out Event>, val subscriber: EventSubscriber<out Event>) : Instruction()

    /** Unsubscribe from an event type with the provided subscriber. */
    class Unsubscribe(val event: KClass<out Event>, val subscriber: EventSubscriber<out Event>) : Instruction()

    /** Unsubscribe from the internal event queue. */
    class UnsubscribeInternal(val event: KClass<out Event>, val subscriber: EventSubscriber<out Event>) : Instruction()

    /** Dispatch a new event to all current subscribers of this event type. */
    class Dispatch(val event: Event) : Instruction()
}

/** Inner class managing the event subscriptions and dispatching. */
private class EventManagerImpl : Actor<Instruction>() {
    /** A map of event types to their subscribers. */
    private val internalSubscribers: MutableMap<KClass<out Event>, HashSet<EventSubscriber<out Event>>> = mutableMapOf()

    /** A map of event types to their subscribers. */
    private val subscribers: MutableMap<KClass<out Event>, HashSet<EventSubscriber<out Event>>> = mutableMapOf()

    private val logger = Logger.withTag("EventManagerImpl")

    override fun onMessage(message: Instruction) {
        when (message) {
            is Instruction.Subscribe -> subscribe(message.event, message.subscriber)
            is Instruction.Unsubscribe -> unsubscribe(message.event, message.subscriber)
            is Instruction.SubscribeInternal -> subscribeInternal(message.event, message.subscriber)
            is Instruction.UnsubscribeInternal -> unsubscribeInternal(message.event, message.subscriber)
            is Instruction.Dispatch -> dispatch(message.event)
        }
    }

    private fun <T : Event> subscribe(eventType: KClass<out T>, subscriber: EventSubscriber<out T>) {
        val eventSubscribers = subscribers.getOrPut(eventType) { hashSetOf() }
        eventSubscribers.add(subscriber)
    }

    private fun <T : Event> subscribeInternal(eventType: KClass<out T>, subscriber: EventSubscriber<out T>) {
        val eventSubscribers = internalSubscribers.getOrPut(eventType) { hashSetOf() }
        eventSubscribers.add(subscriber)
    }

    private fun <T : Event> unsubscribe(eventType: KClass<out T>, subscriber: EventSubscriber<out T>) {
        val eventSubscribers = subscribers[eventType]
        eventSubscribers?.remove(subscriber)
    }

    private fun <T : Event> unsubscribeInternal(eventType: KClass<out T>, subscriber: EventSubscriber<out T>) {
        val eventSubscribers = internalSubscribers[eventType]
        eventSubscribers?.remove(subscriber)
    }

    private fun <T : Event> getSubscribers(eventType: KClass<T>): Set<EventSubscriber<out Event>> {
        return this.subscribers[eventType] ?: emptySet()
    }

    private fun <T : Event> getInternalSubscribers(eventType: KClass<T>): Set<EventSubscriber<out Event>> {
        return this.internalSubscribers[eventType] ?: emptySet()
    }

    @Suppress("UNCHECKED_CAST", "TooGenericExceptionThrown")
    private fun dispatch(event: Event) {
        if (scope == null) {
            throw RuntimeException("Event manager not started")
        }

        logger.d { "Dispatching event: ${event::class.simpleName}" }

        scope?.launch {
            // Internal subscribers are guaranteed to receive events
            // before the "public" queue, so we dispatch them first.
            val internalJobs = mutableListOf<Job>()

            getInternalSubscribers(event::class).forEach {
                val subscriber = it as EventSubscriber<Event>
                internalJobs.add(scope?.launch { subscriber.invoke(event) } ?: return@launch)
            }

            // Wait until all internal listeners have finished
            internalJobs.forEach { it.join() }

            getSubscribers(event::class).forEach {
                val subscriber = it as EventSubscriber<Event>
                scope?.launch { subscriber.invoke(event) } ?: return@launch
            }
        }
    }
}

/** A class for handling event subscriptions and dispatching. */
class EventManager {
    private val inner: EventManagerImpl = EventManagerImpl()

    /** Runs the event manager. This call unblocks only when the event manager is stopped. */
    suspend fun run() {
        inner.run()
    }

    /** Stops the event manager. */
    fun stop() {
        inner.stop()
    }

    /**
     * Add a subscriber to this event manager. The provided callback will be called when an event of that type is
     * dispatched.
     *
     * Note that events dispatched before this call will not be received by the subscriber.
     *
     * @param callback The callback to call when an event of the given type is dispatched. It will be called with the
     *   event as the only parameter.
     */
    inline fun <reified T : Event> subscribe(noinline callback: suspend (T) -> Unit) {
        subscribe(T::class, callback)
    }

    /**
     * Add a subscriber to this event manager. The provided callback will be called when an event of that type is
     * dispatched. The eventType parameter may be omitted if the type can be inferred from the callback.
     *
     * Note that events dispatched before this call will not be received by the subscriber.
     *
     * @param eventType The type of event to subscribe to.
     * @param callback The callback to call when an event of the given type is dispatched. It will be called with the
     *   event as the only parameter.
     */
    fun <T : Event> subscribe(eventType: KClass<T>, callback: suspend (T) -> Unit) {
        inner.sendMessage(Instruction.Subscribe(eventType, EventSubscriber(callback)))
    }

    /**
     * Subscribe to the internal event queue. This event queue is guaranteed to receive events before the "public"
     * queue.
     *
     * @param callback The callback to call when an event of the given type is dispatched.
     */
    @InternalEventManagerApi
    internal inline fun <reified T : Event> subscribeInternal(noinline callback: suspend (T) -> Unit) {
        subscribeInternal(T::class, callback)
    }

    /**
     * Subscribe to the internal event queue. This event queue is guaranteed to receive events before the "public"
     * queue.
     *
     * Caution: If an internal subscriber does not finish its work, the public subscribers will not be called. All
     * internal subscribers *must* return in order for the public subscribers to be called.
     *
     * @param eventType The type of event to subscribe to.
     * @param callback The callback to call when an event of the given type is dispatched.
     */
    @InternalEventManagerApi
    internal fun <T : Event> subscribeInternal(eventType: KClass<T>, callback: suspend (T) -> Unit) {
        inner.sendMessage(Instruction.SubscribeInternal(eventType, EventSubscriber(callback)))
    }

    /**
     * Add a subscriber to this event manager in a lifecycle-aware way. The provided callback will be called when an
     * event of that type is dispatched. The callback will be automatically unsubscribed when the lifecycle is
     * destroyed.
     *
     * Note that events dispatched before this call will not be received by the subscriber.
     *
     * @param callback The callback to call when an event of the given type is dispatched. It will be called with the
     *   event as the only parameter.
     * @param lifecycle The lifecycle to unsubscribe the callback on.
     */
    inline fun <reified T : Event> subscribeWithLifeCycle(
        lifecycle: Lifecycle,
        noinline callback: suspend (T) -> Unit,
    ) {
        subscribe(callback)
        lifecycle.doOnDestroy { unsubscribe(callback) }
    }

    /**
     * Remove a subscriber from this event manager. The provided callback will no longer be called when an event of that
     * type is dispatched. If the subscriber was not subscribed to the given event type, does nothing.
     *
     * @param callback The callback to remove from the subscribers list.
     */
    inline fun <reified T : Event> unsubscribe(noinline callback: suspend (T) -> Unit) {
        unsubscribe(T::class, callback)
    }

    /**
     * Remove a subscriber from this event manager. The provided callback will no longer be called when an event of that
     * type is dispatched. If the subscriber was not subscribed to the given event type, does nothing.
     *
     * @param eventType The type of event to unsubscribe from.
     * @param callback The callback to remove from the subscribers list.
     */
    fun <T : Event> unsubscribe(eventType: KClass<T>, callback: suspend (T) -> Unit) {
        inner.sendMessage(Instruction.Unsubscribe(eventType, EventSubscriber(callback)))
    }

    /**
     * Unsubscribe from the internal event queue.
     *
     * @param callback The callback to remove from the subscribers list.
     */
    @InternalEventManagerApi
    internal inline fun <reified T : Event> unsubscribeInternal(noinline callback: suspend (T) -> Unit) {
        unsubscribeInternal(T::class, callback)
    }

    /**
     * Unsubscribe from the internal event queue.
     *
     * @param eventType The type of event to unsubscribe from.
     * @param callback The callback to remove from the subscribers list.
     */
    @InternalEventManagerApi
    internal fun <T : Event> unsubscribeInternal(eventType: KClass<T>, callback: suspend (T) -> Unit) {
        inner.sendMessage(Instruction.UnsubscribeInternal(eventType, EventSubscriber(callback)))
    }

    /**
     * Wait for an event of the given type to be dispatched. This will suspend the coroutine until an event of the given
     * type is dispatched.
     *
     * @param eventType The type of event to wait for.
     * @param predicate An optional predicate to filter the events.
     * @return The event that was dispatched.
     */
    suspend inline fun <reified EventT : Event> waitFor(
        eventType: KClass<EventT>,
        noinline predicate: ((EventT) -> Boolean)? = null,
    ): EventT {
        val deferredEvent = CompletableDeferred<EventT>()

        val callback = { event: EventT ->
            if (predicate == null || predicate(event)) {
                deferredEvent.complete(event)
            }
        }

        deferredEvent.invokeOnCompletion { unsubscribe(eventType, callback) }

        subscribe<EventT>(callback)

        return deferredEvent.await()
    }

    /**
     * Dispatch the given event to all current subscribers of the event type.
     *
     * @param event The event to dispatch.
     */
    fun dispatch(event: Event) {
        inner.sendMessage(Instruction.Dispatch(event))
    }
}
