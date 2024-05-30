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

package com.mihirpaldhikar.routes

import com.mihirpaldhikar.commons.dto.AuthenticationCredentials
import com.mihirpaldhikar.commons.dto.NewAccount
import com.mihirpaldhikar.controllers.AccountController
import com.mihirpaldhikar.controllers.PasskeyController
import com.mihirpaldhikar.utils.sendResponse
import com.mihirpaldhikar.utils.setAccountCookies
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.routing.*

fun Routing.accountRoute(
    accountController: AccountController,
    passkeyController: PasskeyController
) {
    route("/accounts") {

        post("/authenticationStrategy") {
            val credentials = call.receive<AuthenticationCredentials>()

            val result = accountController.getAuthenticationStrategy(credentials.identifier)
            sendResponse(result)
        }

        post("/new") {
            val newAccount = call.receive<NewAccount>()
            val result = accountController.createAccount(newAccount)
            setAccountCookies(result)
        }


        post("/authenticate") {
            val credentials = call.receive<AuthenticationCredentials>()

            val result = accountController.authenticate(credentials)
            setAccountCookies(result)
        }

        post("/passkeys/register") {
            val credentials = call.receive<AuthenticationCredentials>()
            val result = passkeyController.startPasskeyRegistration(
                identifier = credentials.identifier
            )
            sendResponse(result)
        }

        post("/passkeys/validateRegistrationChallenge") {
            val credentials = call.receive<AuthenticationCredentials>()
            val result =
                passkeyController.validatePasskeyRegistration(
                    identifier = credentials.identifier,
                    credentials = credentials.passkeyCredentials!!
                )

            sendResponse(result)
        }

        post("/passkeys/validatePasskeyChallenge") {
            val credentials = call.receive<AuthenticationCredentials>()
            val result = passkeyController.validatePasskeyChallenge(
                identifier = credentials.identifier,
                credentials = credentials.passkeyCredentials!!
            )

            setAccountCookies(result)
        }
    }
}