package com.collinscoding.utils

import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.concurrent.*
import kotlin.math.pow
import kotlin.math.roundToInt

fun <A> Collection<A>.forEachParallel(numQueues: Int, action: suspend (A) -> Unit) {
    val queueSize = this.size
    val coreThreads = minOf(numQueues, queueSize, Runtime.getRuntime().availableProcessors())
    val factory = ThreadFactory { r ->
        Thread(r).apply {
            isDaemon = true
            name = "KotlinParallelCollection"
        }
    }
    val queue = ArrayBlockingQueue<Runnable>(queueSize)
    val cachedPool =
        ThreadPoolExecutor(coreThreads, coreThreads, 5L, TimeUnit.SECONDS, queue).apply {
            allowCoreThreadTimeOut(true)
            threadFactory = factory
        }.asCoroutineDispatcher()
    runBlocking(cachedPool) {
        map { async { action(it) } }
            .forEach { it.await() }
    }
}
fun <A> Collection<A>.forEachParallel(action: suspend(A) -> Unit) = forEachParallel(Runtime.getRuntime().availableProcessors(), action)
fun <A> Collection<A>.forEachParallelIndexed(numQueues: Int, action: (index: Int, A) -> Unit) {
    var index = 0
    forEachParallel(numQueues) { item -> action(index++, item) }
}
fun <A> Collection<A>.forEachParallelIndexed(action: (index: Int, A) -> Unit) = forEachParallelIndexed(Runtime.getRuntime().availableProcessors(), action)
fun <T, R: Any, C : MutableCollection<in R>> Collection<T>.mapParallelTo(numQueues: Int, destination: C, transform: suspend (T) -> R): C {
    val me = this
    runBlocking { me.map { item -> async {transform(item)} }.forEachParallel(numQueues) { sync -> destination.add(sync.await()) } }
    return destination
}
fun <T, R: Any, C : MutableCollection<in R>> Collection<T>.mapParallelTo(destination: C, transform: suspend (T) -> R): C = mapParallelTo(Runtime.getRuntime().availableProcessors(), destination, transform)
fun <A, B: Any> Collection<A>.mapParallel(numQueues: Int, f: suspend (A) -> B): Collection<B> = mapParallelTo(numQueues, CopyOnWriteArrayList(), f)
fun <A, B: Any> Collection<A>.mapParallel(f: suspend (A) -> B): Collection<B> = mapParallel(Runtime.getRuntime().availableProcessors(), f)
fun <T, R: Any, C : MutableCollection<in R>> Collection<T>.mapNotNullParallelTo(numQueues: Int, destination: C, transform: suspend (T) -> R?): C {
    forEachParallel(numQueues) { element -> transform(element)?.let { destination.add(it) } }
    return destination
}
fun <T, R: Any, C : MutableCollection<in R>> Collection<T>.mapNotNullParallelTo(destination: C, transform: suspend (T) -> R?): C = mapNotNullParallelTo(Runtime.getRuntime().availableProcessors(), destination, transform)
fun <T, R: Any> Collection<T>.mapNotNullParallel(numQueues: Int, f: suspend (T) -> R?): Collection<R> = mapNotNullParallelTo(numQueues, CopyOnWriteArrayList(), f)
fun <T, R: Any> Collection<T>.mapNotNullParallel(f: suspend (T) -> R?): Collection<R> = mapNotNullParallel(Runtime.getRuntime().availableProcessors(), f)
fun <T, R, C : MutableCollection<in R>> Collection<T>.flatMapParallelTo(numQueues: Int, destination: C, transform: suspend (T) -> Iterable<R>): C {
    this.forEachParallel(numQueues) { element -> destination.addAll(transform(element)) }
    return destination
}
fun <T, R, C : MutableCollection<in R>> Collection<T>.flatMapParallelTo(destination: C, transform: suspend (T) -> Iterable<R>): C = flatMapParallelTo(Runtime.getRuntime().availableProcessors(), destination, transform)
fun <T, R> Collection<T>.flatMapParallel(numQueues: Int, transform: suspend (T) -> Iterable<R>): List<R> = flatMapParallelTo(numQueues, CopyOnWriteArrayList(), transform)
fun <T, R> Collection<T>.flatMapParallel(transform: suspend (T) -> Iterable<R>): List<R> = flatMapParallel(Runtime.getRuntime().availableProcessors(), transform)

fun Double.round(places: Int) = (this * 10.0.pow(places)).roundToInt() / 10.0.pow(places)
fun Double.toStringSigned() = "${if (this >= 0) "+" else "-" }${this}"

fun String.runCommand(workingDir: File = File("")): Process = split(" ").runCommand(workingDir)
fun Collection<Any>.runCommand(workingDir: File = File("")): Process = ProcessBuilder(*map { it.toString() }.toTypedArray())
        .directory(workingDir)
        .start()

fun File.notExists() = !exists()
