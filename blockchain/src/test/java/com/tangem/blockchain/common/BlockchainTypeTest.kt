package com.tangem.blockchain.common

import com.google.common.truth.Truth
import org.junit.Test

internal class BlockchainTypeTest {

    @Test
    fun testBlockchainCannotBeBothEvmAndUtxo() {
        Blockchain.entries.forEach { blockchain ->
            val isEvm = blockchain.isEvm()
            val isUTXO = blockchain.isUTXO
            Truth.assertThat(isEvm && isUTXO).isEqualTo(false)
        }
    }
}