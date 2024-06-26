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

package com.mihirpaldhikar.security.jwt

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTDecodeException
import com.auth0.jwt.exceptions.TokenExpiredException
import com.mihirpaldhikar.enums.ResponseCode
import com.mihirpaldhikar.enums.TokenType
import com.mihirpaldhikar.security.dao.JWTMetadata
import com.mihirpaldhikar.security.dao.SecurityToken
import com.mihirpaldhikar.utils.Result
import io.ktor.http.*
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.PublicKey
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.util.*

class JsonWebToken(
    private val jwtMetadata: JWTMetadata
) {
    private data class Keys(
        val authorizationTokenPublicKey: PublicKey,
        val authorizationTokenPrivateKey: PrivateKey,
        val refreshTokenPublicKey: PublicKey,
        val refreshTokenPrivateKey: PrivateKey,
    )

    private fun keys(): Keys {
        val authorizationTokenPublicKey = jwtMetadata.provider.get(jwtMetadata.authorizationTokenKeyId).publicKey
        val authorizationTokenKeySpec =
            PKCS8EncodedKeySpec(Base64.getDecoder().decode(jwtMetadata.authorizationTokenPrivateKey))
        val authorizationTokenPrivateKey = KeyFactory.getInstance("RSA").generatePrivate(authorizationTokenKeySpec)

        val refreshTokenPublicKey = jwtMetadata.provider.get(jwtMetadata.authorizationTokenKeyId).publicKey
        val refreshTokenKeySpec =
            PKCS8EncodedKeySpec(Base64.getDecoder().decode(jwtMetadata.refreshTokenPrivateKey))
        val refreshTokenPrivateKey = KeyFactory.getInstance("RSA").generatePrivate(refreshTokenKeySpec)

        return Keys(
            authorizationTokenPublicKey = authorizationTokenPublicKey,
            authorizationTokenPrivateKey = authorizationTokenPrivateKey,
            refreshTokenPublicKey = refreshTokenPublicKey,
            refreshTokenPrivateKey = refreshTokenPrivateKey
        )
    }

    private fun jwtVerifier(tokenType: TokenType): JWTVerifier {
        val keys = keys()
        return JWT.require(
            Algorithm.RSA256(
                if (tokenType == TokenType.REFRESH_TOKEN) keys.refreshTokenPublicKey as RSAPublicKey
                else keys.authorizationTokenPublicKey as RSAPublicKey,
                if (tokenType == TokenType.REFRESH_TOKEN) keys.refreshTokenPrivateKey as RSAPrivateKey
                else keys.authorizationTokenPrivateKey as RSAPrivateKey
            )
        ).withIssuer(jwtMetadata.issuer).build()
    }

    fun generateAuthorizationToken(uuid: String, sessionId: String): String {

        val currentTime = System.currentTimeMillis()
        val tokenCreatedAt = Date(currentTime)
        val tokenExpiresAt = Date(currentTime + 3600000)
        val keys = keys()
        return JWT.create().withAudience(jwtMetadata.audience).withIssuer(
            jwtMetadata.issuer
        ).withClaim(UUID_CLAIM, uuid).withClaim(
            SESSION_ID_CLAIM, sessionId
        ).withIssuedAt(tokenCreatedAt).withExpiresAt(tokenExpiresAt)
            .withSubject(API_AUTHORIZATION_SUBJECT).sign(
                Algorithm.RSA256(
                    keys.authorizationTokenPublicKey as RSAPublicKey,
                    keys.authorizationTokenPrivateKey as RSAPrivateKey
                )
            )
    }

    fun generateRefreshToken(uuid: String, sessionId: String): String {
        val currentTime = System.currentTimeMillis()
        val tokenCreatedAt = Date(currentTime)
        val tokenExpiresAt = Date(currentTime + 31556952000)
        val keys = keys()
        return JWT.create().withAudience(jwtMetadata.audience).withIssuer(
            jwtMetadata.issuer
        ).withClaim(
            SESSION_ID_CLAIM, sessionId
        ).withClaim(
            UUID_CLAIM, uuid
        ).withIssuedAt(tokenCreatedAt).withExpiresAt(tokenExpiresAt)
            .withSubject(API_AUTHORIZATION_SUBJECT).sign(
                Algorithm.RSA256(
                    keys.authorizationTokenPublicKey as RSAPublicKey,
                    keys.authorizationTokenPrivateKey as RSAPrivateKey
                )
            )
    }

    fun verifySecurityToken(
        authorizationToken: String,
        tokenType: TokenType,
        claim: String = UUID_CLAIM
    ): Result {
        return try {
            val jwtVerifier = jwtVerifier(tokenType)

            Result.Success(
                data = jwtVerifier.verify(authorizationToken).getClaim(claim)
            )
        } catch (e: TokenExpiredException) {
            Result.Error(
                errorCode = ResponseCode.TOKEN_EXPIRED, message = "Token is expired."
            )
        }
    }

    fun generateSecurityTokenFromRefreshToken(securityToken: SecurityToken): Result {
        return try {
            val jwtVerifier = jwtVerifier(TokenType.REFRESH_TOKEN)
            val uuid = jwtVerifier.verify(securityToken.refreshToken).getClaim(UUID_CLAIM).asString()
            val sessionId =
                jwtVerifier.verify(securityToken.refreshToken).getClaim(SESSION_ID_CLAIM).asString()

            Result.Success(
                data = SecurityToken(
                    authorizationToken = generateAuthorizationToken(uuid, sessionId),
                    refreshToken = generateRefreshToken(uuid, sessionId)
                )
            )
        } catch (e: TokenExpiredException) {
            Result.Error(
                statusCode = HttpStatusCode.BadRequest,
                errorCode = ResponseCode.TOKEN_EXPIRED, message = "Refresh Token is expired."
            )
        } catch (e: JWTDecodeException) {
            Result.Error(
                statusCode = HttpStatusCode.BadRequest,
                errorCode = ResponseCode.INVALID_OR_NULL_TOKEN, message = "Refresh Token is invalid or null."
            )
        }
    }

    companion object {
        const val UUID_CLAIM = "uuid"
        const val USERNAME_CLAIM = "username"
        const val API_AUTHORIZATION_SUBJECT = "iam.puconvocation.com"
        const val SESSION_ID_CLAIM = "session"
    }
}