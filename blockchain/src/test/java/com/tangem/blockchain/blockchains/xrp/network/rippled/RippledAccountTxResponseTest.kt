package com.tangem.blockchain.blockchains.xrp.network.rippled

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.xrp.network.XrpTransactionMarker
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.network.moshi
import org.junit.Test

class RippledAccountTxResponseTest {

    private val adapter = moshi.adapter(RippledAccountTxResponse::class.java)

    private fun transactions(json: String): List<RippledTransactionInfo> {
        return requireNotNull(adapter.fromJson(json)?.result?.transactions)
    }

    @Test
    fun `native payment amount is parsed as drops`() {
        val transaction = transactions(RESPONSE)[0]
        val tx = transaction.tx

        assertThat(tx.hash).isEqualTo("A1")
        assertThat(tx.account).isEqualTo("rSender")
        assertThat(tx.destination).isEqualTo("rReceiver")
        assertThat(tx.fee).isEqualTo("12")
        assertThat(tx.transactionType).isEqualTo("Payment")
        assertThat(tx.date).isEqualTo(808_924_800L)
        assertThat(tx.amount).isEqualTo(RippledTransactionAmount.Drops("1500000"))
        assertThat(transaction.isValidated).isTrue()
        assertThat(transaction.meta?.transactionResult).isEqualTo("tesSUCCESS")
    }

    @Test
    fun `issued currency payment amount is parsed as an object`() {
        assertThat(transactions(RESPONSE)[1].tx.amount).isEqualTo(
            RippledTransactionAmount.IssuedCurrency(
                RippledIssuedCurrencyAmount(currency = "USD", issuer = "rIssuer", value = "12.34"),
            ),
        )
    }

    @Test
    fun `trust set limit amount is parsed`() {
        val tx = transactions(RESPONSE)[2].tx

        assertThat(tx.amount).isNull()
        assertThat(tx.limitAmount)
            .isEqualTo(RippledIssuedCurrencyAmount(currency = "USD", issuer = "rIssuer", value = "100"))
    }

    @Test
    fun `marker is parsed`() {
        val result = requireNotNull(adapter.fromJson(RESPONSE)?.result)

        assertThat(result.marker).isEqualTo(RippledMarker(ledger = 98_765, seq = 4))
    }

    @Test
    fun `incomplete marker does not fail the page parsing`() {
        val result = requireNotNull(adapter.fromJson(INCOMPLETE_MARKER_RESPONSE)?.result)

        assertThat(result.marker).isEqualTo(RippledMarker(ledger = null, seq = null))
        assertThat(result.transactions).hasSize(1)
    }

    @Test
    fun `absent marker means the last page`() {
        val result = requireNotNull(adapter.fromJson(EMPTY_RESPONSE)?.result)

        assertThat(result.marker).isNull()
        assertThat(result.transactions).isEmpty()
    }

    @Test
    fun `successful response is mapped into the page`() {
        val result = requireNotNull(adapter.fromJson(RESPONSE)).toDomain()

        val page = (result as Result.Success).data
        assertThat(page.transactions.map { it.hash }).containsExactly("A1", "A2", "A3").inOrder()
        assertThat(page.marker).isEqualTo(XrpTransactionMarker(ledger = 98_765, seq = 4))
    }

    @Test
    fun `missing account is mapped into an empty history`() {
        val result = requireNotNull(adapter.fromJson(ACCOUNT_NOT_FOUND_RESPONSE)).toDomain()

        val page = (result as Result.Success).data
        assertThat(page.transactions).isEmpty()
        assertThat(page.marker).isNull()
    }

    @Test
    fun `any other ledger error fails the request`() {
        val result = requireNotNull(adapter.fromJson(NO_PERMISSION_RESPONSE)).toDomain()

        val error = (result as Result.Failure).error
        assertThat(error).isEqualTo(
            BlockchainSdkError.Xrp.Api(
                errorCode = 6,
                errorName = "noPermission",
                errorMessage = "You don't have permission for this command.",
            ),
        )
    }

    @Test
    fun `ledger error without an error code fails the request`() {
        val result = requireNotNull(adapter.fromJson(ERROR_WITHOUT_CODE_RESPONSE)).toDomain()

        val error = (result as Result.Failure).error
        assertThat(error).isEqualTo(
            BlockchainSdkError.Xrp.Api(errorCode = null, errorName = "lgrIdxsInvalid", errorMessage = null),
        )
    }

    @Test
    fun `response without a result fails the request`() {
        val result = requireNotNull(adapter.fromJson("{}")).toDomain()

        assertThat(result).isInstanceOf(Result.Failure::class.java)
    }

    private companion object {

        val RESPONSE = """
            {
              "result": {
                "account": "rSender",
                "ledger_index_max": 99999999,
                "ledger_index_min": 32570,
                "limit": 20,
                "marker": { "ledger": 98765, "seq": 4 },
                "status": "success",
                "transactions": [
                  {
                    "meta": { "TransactionResult": "tesSUCCESS", "TransactionIndex": 0 },
                    "tx": {
                      "Account": "rSender",
                      "Amount": "1500000",
                      "Destination": "rReceiver",
                      "Fee": "12",
                      "Sequence": 1406,
                      "TransactionType": "Payment",
                      "date": 808924800,
                      "hash": "A1",
                      "ledger_index": 98765
                    },
                    "validated": true
                  },
                  {
                    "meta": { "TransactionResult": "tesSUCCESS" },
                    "tx": {
                      "Account": "rSender",
                      "Amount": { "currency": "USD", "issuer": "rIssuer", "value": "12.34" },
                      "Destination": "rReceiver",
                      "Fee": "12",
                      "TransactionType": "Payment",
                      "date": 808924801,
                      "hash": "A2"
                    },
                    "validated": true
                  },
                  {
                    "meta": { "TransactionResult": "tesSUCCESS" },
                    "tx": {
                      "Account": "rSender",
                      "LimitAmount": { "currency": "USD", "issuer": "rIssuer", "value": "100" },
                      "Fee": "12",
                      "TransactionType": "TrustSet",
                      "date": 808924802,
                      "hash": "A3"
                    },
                    "validated": true
                  }
                ]
              }
            }
        """.trimIndent()

        val INCOMPLETE_MARKER_RESPONSE = """
            {
              "result": {
                "account": "rSender",
                "marker": { "unexpected": "shape" },
                "status": "success",
                "transactions": [
                  {
                    "meta": { "TransactionResult": "tesSUCCESS" },
                    "tx": {
                      "Account": "rSender",
                      "Amount": "1500000",
                      "Destination": "rReceiver",
                      "Fee": "12",
                      "TransactionType": "Payment",
                      "date": 808924800,
                      "hash": "A1"
                    },
                    "validated": true
                  }
                ]
              }
            }
        """.trimIndent()

        val EMPTY_RESPONSE = """
            {
              "result": {
                "account": "rSender",
                "status": "success",
                "transactions": []
              }
            }
        """.trimIndent()

        val ACCOUNT_NOT_FOUND_RESPONSE = """
            {
              "result": {
                "account": "rSender",
                "error": "actNotFound",
                "error_code": 19,
                "error_message": "Account not found.",
                "status": "error"
              }
            }
        """.trimIndent()

        val ERROR_WITHOUT_CODE_RESPONSE = """
            {
              "result": {
                "error": "lgrIdxsInvalid",
                "status": "error"
              }
            }
        """.trimIndent()

        val NO_PERMISSION_RESPONSE = """
            {
              "result": {
                "error": "noPermission",
                "error_code": 6,
                "error_message": "You don't have permission for this command.",
                "status": "error"
              }
            }
        """.trimIndent()
    }
}