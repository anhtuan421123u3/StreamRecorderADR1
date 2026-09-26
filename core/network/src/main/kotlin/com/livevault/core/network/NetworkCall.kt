package com.livevault.core.network

import com.livevault.core.common.Result
import com.livevault.core.network.dto.ApiResponse
import retrofit2.Response

suspend fun <T> safeNetworkCall(call: suspend () -> Response<ApiResponse<T>>): Result<T> = try {
    val response = call()
    if (response.isSuccessful) {
        val body = response.body()
        if (body != null) {
            Result.Success(body.data)
        } else {
            Result.Error(Exception("Empty response body"))
        }
    } else {
        Result.Error(Exception("HTTP ${response.code()}: ${response.message()}"))
    }
} catch (e: Exception) {
    Result.Error(e, e.message)
}

suspend fun safeUnitCall(call: suspend () -> Response<Unit>): Result<Unit> = try {
    val response = call()
    if (response.isSuccessful) {
        Result.Success(Unit)
    } else {
        Result.Error(Exception("HTTP ${response.code()}: ${response.message()}"))
    }
} catch (e: Exception) {
    Result.Error(e, e.message)
}
