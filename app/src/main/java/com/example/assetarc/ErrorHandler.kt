package com.example.assetarc

import android.content.Context
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.CancellationException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

sealed class AppError : Exception() {
    data class NetworkError(override val message: String, val originalError: Throwable? = null) : AppError()
    data class DataError(override val message: String, val originalError: Throwable? = null) : AppError()
    data class ValidationError(override val message: String, val field: String? = null) : AppError()
    data class AuthenticationError(override val message: String) : AppError()
    data class UnknownError(override val message: String, val originalError: Throwable? = null) : AppError()
}

object ErrorHandler {
    private const val TAG = "ErrorHandler"
    
    fun handleError(error: Throwable, context: Context, showToast: Boolean = true) {
        val appError = when (error) {
            is AppError -> error
            is CancellationException -> return // Don't handle cancellation
            is ConnectException, is SocketTimeoutException, is UnknownHostException -> {
                AppError.NetworkError("No internet connection. Please check your network and try again.", error)
            }
            is IllegalArgumentException -> {
                AppError.ValidationError("Invalid input: ${error.message}")
            }
            else -> {
                Log.e(TAG, "Unhandled error", error)
                AppError.UnknownError("An unexpected error occurred. Please try again.", error)
            }
        }
        
        logError(appError)
        
        if (showToast) {
            showErrorToast(appError, context)
        }
    }
    
    fun handleNetworkError(responseCode: Int, context: Context, showToast: Boolean = true) {
        val error = when (responseCode) {
            401 -> AppError.AuthenticationError("Authentication failed. Please log in again.")
            403 -> AppError.AuthenticationError("Access denied. You don't have permission to access this resource.")
            404 -> AppError.DataError("The requested data was not found.")
            429 -> AppError.NetworkError("Too many requests. Please wait a moment and try again.")
            500 -> AppError.NetworkError("Server error. Please try again later.")
            502, 503, 504 -> AppError.NetworkError("Service temporarily unavailable. Please try again later.")
            else -> AppError.NetworkError("Network error (Code: $responseCode). Please try again.")
        }
        
        logError(error)
        
        if (showToast) {
            showErrorToast(error, context)
        }
    }
    
    private fun logError(error: AppError) {
        when (error) {
            is AppError.NetworkError -> {
                Log.e(TAG, "Network Error: ${error.message}", error.originalError)
            }
            is AppError.DataError -> {
                Log.e(TAG, "Data Error: ${error.message}", error.originalError)
            }
            is AppError.ValidationError -> {
                Log.w(TAG, "Validation Error: ${error.message} (Field: ${error.field})")
            }
            is AppError.AuthenticationError -> {
                Log.w(TAG, "Authentication Error: ${error.message}")
            }
            is AppError.UnknownError -> {
                Log.e(TAG, "Unknown Error: ${error.message}", error.originalError)
            }
        }
    }
    
    private fun showErrorToast(error: AppError, context: Context) {
        val message = when (error) {
            is AppError.NetworkError -> error.message
            is AppError.DataError -> error.message
            is AppError.ValidationError -> error.message
            is AppError.AuthenticationError -> error.message
            is AppError.UnknownError -> error.message
        }
        
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
    
    fun getErrorMessage(error: AppError): String {
        return when (error) {
            is AppError.NetworkError -> error.message
            is AppError.DataError -> error.message
            is AppError.ValidationError -> error.message
            is AppError.AuthenticationError -> error.message
            is AppError.UnknownError -> error.message
        }
    }
    
    fun isNetworkError(error: Throwable): Boolean {
        return error is AppError.NetworkError || 
               error is ConnectException || 
               error is SocketTimeoutException || 
               error is UnknownHostException
    }
    
    fun isRetryableError(error: Throwable): Boolean {
        return when (error) {
            is AppError.NetworkError -> true
            is AppError.DataError -> false
            is AppError.ValidationError -> false
            is AppError.AuthenticationError -> false
            is AppError.UnknownError -> false
            is ConnectException, is SocketTimeoutException, is UnknownHostException -> true
            else -> false
        }
    }
}

// Extension function for Result to handle errors
inline fun <T> Result<T>.handleError(
    context: Context,
    onSuccess: (T) -> Unit,
    noinline onError: ((AppError) -> Unit)? = null,
    showToast: Boolean = true
) {
    this.fold(
        onSuccess = onSuccess,
        onFailure = { error ->
            ErrorHandler.handleError(error, context, showToast)
            onError?.invoke(error as? AppError ?: AppError.UnknownError("Unknown error occurred"))
        }
    )
}

// Extension function for safe API calls
suspend fun <T> safeApiCall(
    context: Context,
    apiCall: suspend () -> T,
    onError: ((AppError) -> Unit)? = null
): Result<T> {
    return try {
        Result.success(apiCall())
    } catch (error: Throwable) {
        ErrorHandler.handleError(error, context, false)
        onError?.invoke(error as? AppError ?: AppError.UnknownError("Unknown error occurred"))
        Result.failure(error)
    }
} 