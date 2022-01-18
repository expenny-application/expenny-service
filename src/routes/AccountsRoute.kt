package io.ducket.api.routes

import io.ducket.api.domain.controller.account.AccountController
import io.ktor.auth.*
import io.ktor.routing.*

// TODO add delete multiple
fun Route.accounts(accountController: AccountController) {
    authenticate {
        route("/accounts") {
            get { accountController.getAccounts(this.context) }
            post { accountController.createAccount(this.context) }

            route("/{accountId}") {
                get { accountController.getAccountDetails(this.context) }
                put { accountController.updateAccount(this.context) }
                delete { accountController.deleteAccount(this.context) }

                route("/import") {
                    post { accountController.importAccountTransactions(this.context) }
                }
            }
        }
    }
}