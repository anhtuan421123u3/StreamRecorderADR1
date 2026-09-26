package com.livevault.core.common

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

/** Wraps a suspend API call in a Result<T>, catching exceptions. */
suspend fun <T> safeApiCall(call: suspend () -> T): Result<T> = try {
    Result.Success(call())
} catch (e: Exception) {
    Result.Error(e, e.message)
}

/** Converts a Flow<T> to Flow<Result<T>> with loading + error states. */
fun <T> Flow<T>.asResult(): Flow<Result<T>> = this
    .map<T, Result<T>> { Result.Success(it) }
    .onStart { emit(Result.Loading) }
    .catch { emit(Result.Error(it)) }

/** Format bytes to human-readable string. */
fun Long.toReadableSize(): String {
    val kb = 1024.0
    val mb = kb * 1024
    val gb = mb * 1024
    return when {
        this >= gb -> "%.1f GB".format(this / gb)
        this >= mb -> "%.1f MB".format(this / mb)
        this >= kb -> "%.1f KB".format(this / kb)
        else -> "$this B"
    }
}

fun Long.formatBytes(): String = toReadableSize()

/** Format seconds to mm:ss or hh:mm:ss. */
fun Long.formatDuration(): String {
    val h = this / 3600
    val m = (this % 3600) / 60
    val s = this % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s)
    else "%d:%02d".format(m, s)
}

fun Int.toFormattedDuration(): String = this.toLong().formatDuration()
