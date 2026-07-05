package com.tangem.blockchain.blockchains.tron.network

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import org.junit.Test

/**
 * [REDACTED_TASK_KEY]: the two contract-call request shapes must serialize to their own wire formats and never
 * leak the other's keys:
 * - [TronTriggerSmartContractRequest.Function] → `function_selector` + `parameter`;
 * - [TronTriggerSmartContractRequest.CallData] → a raw `data` field.
 *
 * The `CallData` shape is what lets a DEX swap be energy-estimated by raw call data, so its wire
 * format is part of the contract with the node.
 */
class TronTriggerSmartContractRequestTest {

    private val moshi = Moshi.Builder().build()

    @Test
    fun `GIVEN function request WHEN serialized THEN it has function_selector and parameter but no data`() {
        val json = moshi.adapter(TronTriggerSmartContractRequest.Function::class.java).toJson(
            TronTriggerSmartContractRequest.Function(
                ownerAddress = "TU1BRXbr6EmKmrLL4Kymv7Wp18eYFkRfAF",
                contractAddress = "TXLAQ63Xg1NAzckPwKHvzw7CSEmLMEqcdj",
                functionSelector = "transfer(address,uint256)",
                parameter = "abcdef",
                visible = true,
            ),
        )

        assertThat(json).contains("\"function_selector\":\"transfer(address,uint256)\"")
        assertThat(json).contains("\"parameter\":\"abcdef\"")
        assertThat(json).doesNotContain("\"data\":")
    }

    @Test
    fun `GIVEN call data request WHEN serialized THEN it has raw data but no function_selector or parameter`() {
        val callDataHex = "a9059cbb00000000000000000000000000000000000000000000000000000000000f4240"

        val json = moshi.adapter(TronTriggerSmartContractRequest.CallData::class.java).toJson(
            TronTriggerSmartContractRequest.CallData(
                ownerAddress = "TU1BRXbr6EmKmrLL4Kymv7Wp18eYFkRfAF",
                contractAddress = "TXXxc9NsHndfQ2z9kMKyWpYa5T3QbhKGwn",
                data = callDataHex,
                visible = true,
            ),
        )

        assertThat(json).contains("\"data\":\"$callDataHex\"")
        assertThat(json).doesNotContain("function_selector")
        assertThat(json).doesNotContain("parameter")
    }
}