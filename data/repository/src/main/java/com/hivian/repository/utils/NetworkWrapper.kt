package com.hivian.repository.utils

import retrofit2.HttpException
import java.io.IOException
import java.net.HttpURLConnection


sealed class NetworkWrapper {
    data class Success(var isEmpty: Boolean): NetworkWrapper()
    data class Error(val error: ErrorEntity): NetworkWrapper()
}

sealed class ErrorEntity {

    object Network : ErrorEntity()

    object NotFound : ErrorEntity()

    object AccessDenied : ErrorEntity()

    object ServiceUnavailable : ErrorEntity()

    object Unknown : ErrorEntity()
}

interface ErrorHandler<in T, out R> {

    operator fun invoke(throwable: T): R
}

class ErrorHandlerImpl : ErrorHandler<Throwable, ErrorEntity> {

    override operator fun invoke(throwable: Throwable): ErrorEntity {
        return when (throwable) {
            is IOException -> ErrorEntity.Network
            is HttpException -> {
                when (throwable.code()) {
                    // not found
                    HttpURLConnection.HTTP_NOT_FOUND -> ErrorEntity.NotFound

                    // access denied
                    HttpURLConnection.HTTP_FORBIDDEN -> ErrorEntity.AccessDenied

                    // unavailable service
                    HttpURLConnection.HTTP_UNAVAILABLE -> ErrorEntity.ServiceUnavailable

                    // all the others will be treated as unknown error
                    else -> ErrorEntity.Unknown
                }
            }
            else -> ErrorEntity.Unknown
        }
    }
}
