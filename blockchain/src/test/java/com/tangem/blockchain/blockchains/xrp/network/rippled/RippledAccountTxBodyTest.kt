package com.tangem.blockchain.blockchains.xrp.network.rippled

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.xrp.network.XrpAccountTxRequest
import com.tangem.blockchain.blockchains.xrp.network.XrpTransactionMarker
import com.tangem.blockchain.network.moshi
import org.junit.Test

/**
 * The `account_tx` request body is assembled as a `Map<String, Any>`, so its serialization relies on the runtime
 * types of the values. Numbers silently turning into floats would break the request on the ledger side.
 */
class RippledAccountTxBodyTest {

    private val adapter = moshi.adapter(RippledBody::class.java)

    @Test
    fun `first page body has no marker`() {
        val body = makeAccountTxBody(XrpAccountTxRequest(address = "rSender", limit = 20))

        assertThat(adapter.toJson(body)).isEqualTo(
            """{"method":"account_tx","params":[{"account":"rSender","api_version":1,"binary":false,""" +
                """"forward":false,"ledger_index_min":-1,"ledger_index_max":-1,"limit":20}]}""",
        )
    }

    @Test
    fun `marker is serialized with integer fields`() {
        val body = makeAccountTxBody(
            XrpAccountTxRequest(
                address = "rSender",
                limit = 20,
                marker = XrpTransactionMarker(ledger = 98_765, seq = 4),
            ),
        )

        assertThat(adapter.toJson(body)).endsWith("""{"ledger":98765,"seq":4}}]}""")
    }
}