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

package com.mihirpaldhikar.security.passkey

import com.mihirpaldhikar.database.mongodb.entities.Account
import com.mihirpaldhikar.database.mongodb.repository.AccountRepository
import com.mihirpaldhikar.security.dao.FidoCredential
import com.mihirpaldhikar.utils.PasskeyUtils
import com.yubico.webauthn.CredentialRepository
import com.yubico.webauthn.RegisteredCredential
import com.yubico.webauthn.data.ByteArray
import com.yubico.webauthn.data.PublicKeyCredentialDescriptor
import com.yubico.webauthn.data.PublicKeyCredentialType
import com.yubico.webauthn.data.exception.Base64UrlException
import kotlinx.coroutines.runBlocking
import org.bson.types.ObjectId
import java.util.*
import java.util.stream.Collectors

class PasskeyCredentialRepository(
    private val accountRepository: AccountRepository,
) : CredentialRepository {
    override fun getCredentialIdsForUsername(username: String): MutableSet<PublicKeyCredentialDescriptor> {
        val account: Account?
        runBlocking {
            account = accountRepository.getAccount(username)
        }
        if (account == null) {
            return mutableSetOf()
        }

        return account.fidoCredential.stream().map {
            toPublicKeyCredentialDescriptor(
                it
            )
        }.collect(Collectors.toSet())
    }

    override fun getUserHandleForUsername(username: String): Optional<ByteArray> {
        val account: Account?
        runBlocking {
            account = accountRepository.getAccount(username)
        }
        if (account == null) {
            return Optional.empty()
        }

        return Optional.of(PasskeyUtils.toByteArray(account.uuid))
    }

    override fun getUsernameForUserHandle(userHandle: ByteArray): Optional<String> {
        val account: Account?
        runBlocking {
            account = accountRepository.getAccount(PasskeyUtils.toObjectId(userHandle).toHexString())
        }
        if (account == null) {
            return Optional.empty()
        }

        return Optional.of(account.username)
    }

    override fun lookup(credentialId: ByteArray, userHandle: ByteArray): Optional<RegisteredCredential> {
        val account: Account?
        runBlocking {
            account = accountRepository.getAccount(PasskeyUtils.toObjectId(userHandle).toHexString())
        }
        if (account == null) {
            return Optional.empty()
        }
        val fidoCredential = account.fidoCredential.firstOrNull { credentialId == ByteArray.fromBase64Url(it.keyId) }
            ?: return Optional.empty()

        return Optional.of(
            toRegisteredCredential(
                uuid = account.uuid,
                fidoCredential = fidoCredential
            )
        )
    }

    override fun lookupAll(credentialId: ByteArray): MutableSet<RegisteredCredential> {
        return mutableSetOf()
    }

    private fun toRegisteredCredential(uuid: ObjectId, fidoCredential: FidoCredential): RegisteredCredential {
        try {
            return RegisteredCredential.builder()
                .credentialId(ByteArray.fromBase64Url(fidoCredential.keyId))
                .userHandle(PasskeyUtils.toByteArray(uuid))
                .publicKeyCose(ByteArray.fromBase64Url(fidoCredential.publicKeyCose))
                .build()
        } catch (e: Base64UrlException) {
            throw java.lang.RuntimeException(e)
        }
    }

    private fun toPublicKeyCredentialDescriptor(
        cred: FidoCredential
    ): PublicKeyCredentialDescriptor {
        try {
            return PublicKeyCredentialDescriptor.builder()
                .id(ByteArray.fromBase64Url(cred.keyId))
                .type(PublicKeyCredentialType.valueOf(cred.keyType))
                .build()
        } catch (e: Base64UrlException) {
            throw RuntimeException(e)
        }
    }
}