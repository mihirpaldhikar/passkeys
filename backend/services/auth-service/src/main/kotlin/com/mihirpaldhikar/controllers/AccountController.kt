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

import com.mihirpaldhikar.commons.dto.AuthenticationCredentials
import com.mihirpaldhikar.commons.dto.NewAccount
import com.mihirpaldhikar.database.mongodb.entities.Account
import com.mihirpaldhikar.database.mongodb.repository.AccountRepository
import com.mihirpaldhikar.enums.AuthenticationStrategy
import com.mihirpaldhikar.enums.ResponseCode
import com.mihirpaldhikar.enums.TokenType
import com.mihirpaldhikar.security.core.Hash
import com.mihirpaldhikar.security.dao.SecurityToken
import com.mihirpaldhikar.security.jwt.JsonWebToken
import com.mihirpaldhikar.utils.Result
import io.ktor.http.*
import org.bson.types.ObjectId

class AccountController(
    private val accountRepository: AccountRepository,
    private val jsonWebToken: JsonWebToken,
    private val passkeyController: PasskeyController
) {
    suspend fun createAccount(newAccount: NewAccount): Result {
        if (accountRepository.getAccount(newAccount.username) != null || accountRepository.getAccount(newAccount.email) != null) {
            return Result.Error(
                statusCode = HttpStatusCode.Conflict,
                errorCode = ResponseCode.ACCOUNT_EXISTS,
                message = "Account already exists."
            )
        }

        val account = Account(
            uuid = ObjectId(),
            username = newAccount.username,
            email = newAccount.email,
            displayName = newAccount.displayName,
            password = if (newAccount.password == null) null else Hash().generateSaltedHash(newAccount.password),
            fidoCredential = mutableSetOf()
        )

        val accountCreated = accountRepository.createAccount(account)
        if (!accountCreated) {
            return Result.Error(
                statusCode = HttpStatusCode.InternalServerError,
                errorCode = ResponseCode.ACCOUNT_CREATION_ERROR,
                message = "Account creation failed."
            )
        }

        if (newAccount.authenticationStrategy === AuthenticationStrategy.PASSKEY) {
            val result = passkeyController.startPasskeyRegistration(account.username)
            return result;
        }

        val securityTokens = SecurityToken(
            message = "Account Created.",
            authorizationToken = jsonWebToken.generateAuthorizationToken(account.uuid.toHexString(), "null"),
            refreshToken = jsonWebToken.generateRefreshToken(account.uuid.toHexString(), "null"),
        )

        return Result.Success(
            statusCode = HttpStatusCode.Created,
            code = ResponseCode.ACCOUNT_CREATED,
            data = securityTokens
        )
    }


    suspend fun getAuthenticationStrategy(identifier: String): Result {
        val account = accountRepository.getAccount(identifier) ?: return Result.Error(
            statusCode = HttpStatusCode.NotFound,
            errorCode = ResponseCode.ACCOUNT_NOT_FOUND,
            message = "Account not found."
        )

        if (account.fidoCredential.isEmpty()) {
            return Result.Success(
                statusCode = HttpStatusCode.OK,
                code = ResponseCode.OK,
                data = hashMapOf("authenticationStrategy" to AuthenticationStrategy.PASSWORD)
            )
        }

        return Result.Success(
            statusCode = HttpStatusCode.OK,
            code = ResponseCode.OK,
            data = hashMapOf("authenticationStrategy" to AuthenticationStrategy.PASSKEY)
        )
    }

    suspend fun authenticate(credentials: AuthenticationCredentials): Result {
        val account = accountRepository.getAccount(credentials.identifier) ?: return Result.Error(
            statusCode = HttpStatusCode.NotFound,
            errorCode = ResponseCode.ACCOUNT_NOT_FOUND,
            message = "Account not found."
        )

        if (account.fidoCredential.isNotEmpty() && account.password == null) {
            val result = passkeyController.startPasskeyChallenge(credentials.identifier)
            return result
        }

        if (credentials.password == null || account.password == null) {
            return Result.Error(
                statusCode = HttpStatusCode.BadRequest,
                errorCode = ResponseCode.NULL_PASSWORD,
                message = "Please provide password."
            )
        }
        val isPasswordVerified = Hash().verify(credentials.password, account.password)

        if (!isPasswordVerified) {
            return Result.Error(
                statusCode = HttpStatusCode.Forbidden,
                errorCode = ResponseCode.INVALID_PASSWORD,
                message = "Password do not match."
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
    }

    suspend fun accountDetails(securityToken: SecurityToken): Result {
        if (securityToken.authorizationToken == null) {
            return Result.Error(
                statusCode = HttpStatusCode.Unauthorized,
                errorCode = ResponseCode.INVALID_OR_NULL_TOKEN,
                message = "Authorization token is invalid or expired."
            )
        }
        val jwtResult =
            jsonWebToken.verifySecurityToken(securityToken.authorizationToken, TokenType.AUTHORIZATION_TOKEN)
        if (jwtResult is Result.Error) {
            return jwtResult
        }
        val account =
            accountRepository.getAccount(jwtResult.responseData.toString().replace("\"", "")) ?: return Result.Error(
                statusCode = HttpStatusCode.NotFound,
                errorCode = ResponseCode.ACCOUNT_NOT_FOUND,
                message = "Account not found."
            )
        return Result.Success(
            statusCode = HttpStatusCode.OK,
            code = ResponseCode.OK,
            data = account
        )
    }

    fun generateNewSecurityTokens(securityToken: SecurityToken): Result {
        return jsonWebToken.generateSecurityTokenFromRefreshToken(securityToken)
    }
}