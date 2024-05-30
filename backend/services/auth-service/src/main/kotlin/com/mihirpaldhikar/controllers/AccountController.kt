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

import com.mihirpaldhikar.commons.dto.NewAccount
import com.mihirpaldhikar.database.mongodb.entities.Account
import com.mihirpaldhikar.database.mongodb.repository.AccountRepository
import com.mihirpaldhikar.enums.AuthenticationStrategy
import com.mihirpaldhikar.enums.ResponseCode
import com.mihirpaldhikar.security.core.Hash
import com.mihirpaldhikar.utils.Result
import io.ktor.http.*
import org.bson.types.ObjectId

class AccountController(
    private val accountRepository: AccountRepository
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
            password = Hash().generateSaltedHash(newAccount.password),
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

        return Result.Success(
            statusCode = HttpStatusCode.Created,
            code = ResponseCode.ACCOUNT_CREATED,
            data = hashMapOf("message" to "Account created.")
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
}