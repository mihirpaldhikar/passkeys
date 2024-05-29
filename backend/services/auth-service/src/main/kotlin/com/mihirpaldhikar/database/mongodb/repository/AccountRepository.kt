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

package com.mihirpaldhikar.database.mongodb.repository

import com.mihirpaldhikar.database.mongodb.datasource.AccountDatasource
import com.mihirpaldhikar.database.mongodb.entities.Account
import com.mihirpaldhikar.security.dao.FidoCredential
import com.mihirpaldhikar.utils.RegexValidator
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.firstOrNull
import org.bson.types.ObjectId

class AccountRepository(
    database: MongoDatabase,
) : AccountDatasource {

    private val accountCollection = database.getCollection<Account>("accounts");

    override suspend fun createAccount(account: Account): Boolean {
        return accountCollection.withDocumentClass<Account>().insertOne(account).wasAcknowledged()
    }

    override suspend fun getAccount(identifier: String): Account? {
        return if (RegexValidator.isValidEmail(identifier)) {
            accountCollection.withDocumentClass<Account>().find(eq(Account::email.name, identifier)).firstOrNull()

        } else if (RegexValidator.isValidUserName(identifier)) {
            accountCollection.withDocumentClass<Account>().find(eq(Account::username.name, identifier)).firstOrNull()

        } else {
            accountCollection.withDocumentClass<Account>().find(eq("_id", ObjectId(identifier))).firstOrNull()
        }
    }

    override suspend fun addFidoCredentials(uuid: String, fidoCredential: FidoCredential): Boolean {
        val account =
            accountCollection.withDocumentClass<Account>().find(eq("_id", ObjectId(uuid))).firstOrNull() ?: return false

        val fidoCredentialSet = account.fidoCredential
        fidoCredentialSet.add(fidoCredential)

        return accountCollection.withDocumentClass<Account>().updateOne(
            eq("_id", ObjectId(uuid)),
            Updates.combine(
                Updates.set(Account::fidoCredential.name, fidoCredentialSet),
            )
        ).wasAcknowledged()
    }
}