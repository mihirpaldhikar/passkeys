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

import com.google.gson.annotations.Expose
import com.mihirpaldhikar.enums.ResponseCode
import io.ktor.http.*

sealed class Result(
    val httpStatusCode: HttpStatusCode? = HttpStatusCode.OK,
    val responseCode: ResponseCode,
    val responseData: Any,
) {
    data class Error(
        val statusCode: HttpStatusCode? = HttpStatusCode.BadRequest,
        @Expose val errorCode: ResponseCode,
        @Expose val message: String,
    ) : Result(
        httpStatusCode = statusCode, responseCode = errorCode, responseData = message
    )

    data class Success(
        val statusCode: HttpStatusCode? = HttpStatusCode.OK,
        val code: ResponseCode = ResponseCode.OK,
        @Expose val data: Any,
        val encodeStringAsJSON: Boolean? = false
    ) : Result(
        httpStatusCode = statusCode, responseCode = code, responseData = data

    )
}