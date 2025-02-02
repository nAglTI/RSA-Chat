package com.hypergonial.chat.model

import com.arkivanov.essenty.lifecycle.Lifecycle
import com.arkivanov.essenty.lifecycle.doOnDestroy
import kotlin.reflect.KClass
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/** A type that has an event manager. */
interface EventManagerAware {
    val eventManager: EventManager
}

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
    class Subscribe(val event: KClass<out Event>, val subscriber: EventSubscriber<out Event>) : Instruction()

    class Unsubscribe(val event: KClass<out Event>, val subscriber: EventSubscriber<out Event>) : Instruction()

    class Dispatch(val event: Event) : Instruction()
}

/** Inner class managing the event subscriptions and dispatching. */
private class EventManagerActor {
    private val subscribers: MutableMap<KClass<out Event>, HashSet<EventSubscriber<out Event>>> = mutableMapOf()

    private var channel = Channel<Instruction>(Channel.Factory.UNLIMITED)
    private var scope: CoroutineScope? = null
    private var runningJob: Job? = null

    suspend fun run() = coroutineScope {
        if (runningJob != null) {
            runningJob?.cancel()
        }
        scope = this
        runningJob = launch { handleInstructions() }
    }

    fun stop() {
        runningJob?.cancel()
        runningJob = null
    }

    private suspend fun handleInstructions() {
        for (instruction in channel) {
            when (instruction) {
                is Instruction.Subscribe -> subscribe(instruction.event, instruction.subscriber)
                is Instruction.Unsubscribe -> unsubscribe(instruction.event, instruction.subscriber)
                is Instruction.Dispatch -> dispatch(instruction.event)
            }
        }
    }

    suspend fun sendInstruction(instruction: Instruction) {
        channel.send(instruction)
    }

    fun trySendInstruction(instruction: Instruction): Boolean {
        return channel.trySend(instruction).isSuccess
    }

    private fun <T : Event> subscribe(eventType: KClass<out T>, subscriber: EventSubscriber<out T>) {
        val eventSubscribers = subscribers.getOrPut(eventType) { hashSetOf() }
        eventSubscribers.add(subscriber)
    }

    private fun <T : Event> unsubscribe(eventType: KClass<out T>, subscriber: EventSubscriber<out T>) {
        val eventSubscribers = subscribers[eventType]
        eventSubscribers?.retainAll { it != subscriber }
    }

    private fun <T : Event> getSubscribers(eventType: KClass<T>): Set<EventSubscriber<out Event>> {
        return this.subscribers[eventType] ?: emptySet()
    }

    @Suppress("UNCHECKED_CAST")
    private fun dispatch(event: Event) {
        if (scope == null) {
            throw RuntimeException("Event manager not started")
        }

        val eventSubscribers = getSubscribers(event::class)
        eventSubscribers.forEach {
            val subscriber = it as EventSubscriber<Event>
            scope!!.launch { subscriber.invoke(event) }
        }
    }
}

/** A class for handling event subscriptions and dispatching. */
class EventManager {
    private val inner: EventManagerActor = EventManagerActor()

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
        val res = inner.trySendInstruction(Instruction.Subscribe(eventType, EventSubscriber(callback)))
        if (!res) {
            throw RuntimeException("Failed to subscribe $callback to $eventType")
        }
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
        val res = inner.trySendInstruction(Instruction.Unsubscribe(eventType, EventSubscriber(callback)))
        if (!res) {
            throw RuntimeException("Failed to subscribe $callback to $eventType")
        }
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
        val res = inner.trySendInstruction(Instruction.Dispatch(event))
        if (!res) {
            throw RuntimeException("Failed to dispatch $event")
        }
    }
}
