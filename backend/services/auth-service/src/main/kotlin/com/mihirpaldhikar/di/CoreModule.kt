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

package com.mihirpaldhikar.di

import com.mihirpaldhikar.Environment
import com.mihirpaldhikar.database.mongodb.repository.AccountRepository
import com.mihirpaldhikar.security.jwt.JsonWebToken
import com.mihirpaldhikar.security.passkey.PasskeyRelyingParty
import com.yubico.webauthn.RelyingParty
import org.koin.dsl.module

object CoreModule {
    val init = module {
        single<Environment> {
            Environment()
        }

        single<RelyingParty> {
            PasskeyRelyingParty(
                accountRepository = get<AccountRepository>(),
                developmentMode = get<Environment>().developmentMode
            ).getRelyingParty()
        }

        single<JsonWebToken> {
            JsonWebToken(
                jwtMetadata = get<Environment>().jwtMetadata
            )
        }
    }
}