/*
 * Copyright (c) Mihir Paldhikar
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the “Software”), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.mihirpaldhikar.utils

import com.mihirpaldhikar.Environment
import com.mihirpaldhikar.enums.ResponseCode
import com.mihirpaldhikar.security.dao.SecurityToken
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.util.date.*
import io.ktor.util.pipeline.*
import org.koin.java.KoinJavaComponent


object CookieName {
    const val AUTHORIZATION_TOKEN_COOKIE = "__at__"
    const val REFRESH_TOKEN_COOKIE = "__rt__"
}

val environment: Environment by KoinJavaComponent.inject(Environment::class.java)


suspend fun PipelineContext<Unit, ApplicationCall>.sendResponse(
    repositoryResult: Result
) {
    if (repositoryResult is Result.Success && repositoryResult.encodeStringAsJSON == true) {
        call.response.headers.append("Content-Type", "application/json")
    }
    call.respond(
        status = repositoryResult.httpStatusCode ?: HttpStatusCode.OK,
        message = if (repositoryResult is Result.Success) {
            repositoryResult.responseData
        } else {
            repositoryResult
        },
    )
}

fun PipelineContext<Unit, ApplicationCall>.setCookie(
    name: String,
    value: String,
    expiresAt: Long,
    httpOnly: Boolean = true,
) {
    call.response.cookies.append(
        Cookie(
            name = name,
            value = value,
            expires = GMTDate(System.currentTimeMillis() + expiresAt),
            httpOnly = httpOnly,
            domain = if (environment.developmentMode) "localhost" else ".mihirpaldhikar.com",
            path = "/",
            secure = !environment.developmentMode,
            extensions = hashMapOf("SameSite" to "lax")
        )
    )
}

suspend fun PipelineContext<Unit, ApplicationCall>.setAccountCookies(result: Result) {
    if (result is Result.Success &&
        result.responseData is SecurityToken &&
        result.responseData.refreshToken != null &&
        result.responseData.authorizationToken != null
    ) {
        setCookie(
            name = CookieName.AUTHORIZATION_TOKEN_COOKIE,
            value = result.responseData.authorizationToken,
            expiresAt = 3600000
        )
        setCookie(
            name = CookieName.REFRESH_TOKEN_COOKIE,
            value = result.responseData.refreshToken,
            expiresAt = 2629800000
        )
        if (result.responseData.message != null) {
            return sendResponse(
                Result.Success(
                    data = mapOf("message" to result.responseData.message),
                )
            )
        }
    }
    return sendResponse(result)
}

fun PipelineContext<Unit, ApplicationCall>.getSecurityTokens(): SecurityToken {
    val authorizationToken = call.request.cookies[CookieName.AUTHORIZATION_TOKEN_COOKIE]
    val refreshToken = call.request.cookies[CookieName.REFRESH_TOKEN_COOKIE]

    return SecurityToken(
        authorizationToken = authorizationToken,
        refreshToken = refreshToken
    )
}


