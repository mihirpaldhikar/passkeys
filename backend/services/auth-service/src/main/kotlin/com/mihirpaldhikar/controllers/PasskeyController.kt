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

package com.mihirpaldhikar.controllers

import com.google.common.cache.CacheBuilder
import com.mihirpaldhikar.database.mongodb.entities.Account
import com.mihirpaldhikar.database.mongodb.repository.AccountRepository
import com.mihirpaldhikar.enums.ResponseCode
import com.mihirpaldhikar.security.dao.FidoCredential
import com.mihirpaldhikar.security.dao.SecurityToken
import com.mihirpaldhikar.security.jwt.JsonWebToken
import com.mihirpaldhikar.utils.PasskeyUtils
import com.mihirpaldhikar.utils.Result
import com.yubico.webauthn.*
import com.yubico.webauthn.data.*
import com.yubico.webauthn.exception.AssertionFailedException
import io.ktor.http.*
import java.util.concurrent.TimeUnit

class PasskeyController(
    private val rp: RelyingParty,
    private val accountRepository: AccountRepository,
    private val jsonWebToken: JsonWebToken
) {
    private val pkcCache = CacheBuilder.newBuilder().expireAfterAccess(5, TimeUnit.MINUTES)
        .build<String, PublicKeyCredentialCreationOptions>()

    private val assertionCache = CacheBuilder.newBuilder().expireAfterAccess(5, TimeUnit.MINUTES)
        .build<String, AssertionRequest>()

    suspend fun startPasskeyRegistration(identifier: String): Result {
        val account = accountRepository.getAccount(identifier) ?: return Result.Error(
            statusCode = HttpStatusCode.NotFound,
            errorCode = ResponseCode.ACCOUNT_NOT_FOUND,
            message = "Account not found"
        )

        val pkcOptions = createPublicKeyCredentialCreationOptions(account)
        pkcCache.put(identifier, pkcOptions)

        return Result.Success(
            data = pkcOptions.toCredentialsCreateJson(),
            encodeStringAsJSON = true
        )
    }

    suspend fun validatePasskeyRegistration(identifier: String, credentials: String): Result {
        val pkc: PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> =
            PublicKeyCredential.parseRegistrationResponseJson(credentials)

        val result = rp.finishRegistration(
            FinishRegistrationOptions.builder()
                .request(pkcCache.getIfPresent(identifier))
                .response(pkc)
                .build()
        )
        val account = accountRepository.getAccount(identifier) ?: return Result.Error(
            statusCode = HttpStatusCode.NotFound,
            errorCode = ResponseCode.ACCOUNT_NOT_FOUND,
            message = "Account not found"
        )

        val fidoCredentialAdded = accountRepository.addFidoCredentials(
            account.uuid.toHexString(), FidoCredential(
                keyId = result.keyId.id.base64Url,
                publicKeyCose = result.publicKeyCose.base64Url,
                keyType = result.keyId.type.name,
            )
        )

        if (!fidoCredentialAdded) {
            return Result.Error(
                statusCode = HttpStatusCode.InternalServerError,
                errorCode = ResponseCode.REQUEST_NOT_COMPLETED,
                message = "Cannot register Passkey at the moment."
            )
        }

        return Result.Success(
            statusCode = HttpStatusCode.Created,
            code = ResponseCode.OK,
            data = hashMapOf("message" to "Passkey registered.")
        )
    }


    fun startPasskeyChallenge(identifier: String): Result {
        val request = rp.startAssertion(
            StartAssertionOptions.builder()
                .username(identifier)
                .build()
        )
        assertionCache.put(identifier, request)
        return Result.Success(
            data = request.toCredentialsGetJson(),
            encodeStringAsJSON = true
        )
    }

    suspend fun validatePasskeyChallenge(identifier: String, credentials: String): Result {

        val account = accountRepository.getAccount(identifier) ?: return Result.Error(
            statusCode = HttpStatusCode.NotFound,
            errorCode = ResponseCode.ACCOUNT_NOT_FOUND,
            message = "Account not found."
        )

        val pkc =
            PublicKeyCredential.parseAssertionResponseJson(credentials)
        try {
            val result = rp.finishAssertion(
                FinishAssertionOptions.builder()
                    .request(assertionCache.getIfPresent(identifier))
                    .response(pkc)
                    .build()
            )

            if (!result.isSuccess) {
                return Result.Error(
                    statusCode = HttpStatusCode.InternalServerError,
                    errorCode = ResponseCode.REQUEST_NOT_COMPLETED,
                    message = "Passkey challenge failed."
                )
            }

            val securityTokens = SecurityToken(
                message = "Authenticated Successfully.",
                authorizationToken = jsonWebToken.generateAuthorizationToken(account.uuid.toHexString(), "null"),
                refreshToken = jsonWebToken.generateRefreshToken(account.uuid.toHexString(), "null"),
            )
            return Result.Success(
                statusCode = HttpStatusCode.OK,
                code = ResponseCode.OK,
                data = securityTokens
            )
        } catch (error: AssertionFailedException) {
            return Result.Error(
                statusCode = HttpStatusCode.InternalServerError,
                errorCode = ResponseCode.REQUEST_NOT_COMPLETED,
                message = error.message ?: "Passkey challenge failed."
            )
        }
    }

    private fun createPublicKeyCredentialCreationOptions(
        user: Account
    ): PublicKeyCredentialCreationOptions {
        val userIdentity =
            UserIdentity.builder()
                .name(user.email)
                .displayName(user.displayName)
                .id(PasskeyUtils.toByteArray(user.uuid))
                .build()

        val authenticatorSelectionCriteria =
            AuthenticatorSelectionCriteria.builder()
                .userVerification(UserVerificationRequirement.REQUIRED)
                .build()

        val startRegistrationOptions =
            StartRegistrationOptions.builder()
                .user(userIdentity)
                .timeout(30000)
                .authenticatorSelection(authenticatorSelectionCriteria)
                .build()

        val options: PublicKeyCredentialCreationOptions =
            rp.startRegistration(startRegistrationOptions)

        return options
    }
}