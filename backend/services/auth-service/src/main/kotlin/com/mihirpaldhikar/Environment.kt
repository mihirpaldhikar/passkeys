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

package com.mihirpaldhikar

import com.auth0.jwk.JwkProviderBuilder
import com.mihirpaldhikar.security.dao.JWTMetadata
import java.util.concurrent.TimeUnit

class Environment {
    val developmentMode: Boolean = System.getenv("DEVELOPMENT_MODE").toBoolean()

    val mongoDBConnectionURL = System.getenv("MONGO_DB_CONNECTION_URL").toString()
    val mongoDBName = System.getenv("MONGO_DB_NAME").toString()

    val jwtMetadata: JWTMetadata = JWTMetadata(
        authorizationTokenPrivateKey = System.getenv("AUTHORIZATION_TOKEN_PRIVATE_KEY"),
        refreshTokenPrivateKey = System.getenv("REFRESH_TOKEN_PRIVATE_KEY"),
        authorizationTokenKeyId = System.getenv("AUTHORIZATION_TOKEN_KEY_ID"),
        refreshTokenKeyId = System.getenv("REFRESH_TOKEN_KEY_ID"),
        audience = System.getenv("API_AUDIENCE"),
        issuer = System.getenv("CREDENTIALS_AUTHORITY"),
        realm = System.getenv("CREDENTIALS_REALM"),
        provider = JwkProviderBuilder(System.getenv("CREDENTIALS_AUTHORITY")).cached(10, 24, TimeUnit.HOURS)
            .rateLimited(10, 1, TimeUnit.MINUTES).build(),
    )
}