package com.tangem.blockchain.blockchains.polkadot.network

import com.google.common.truth.Truth
import org.junit.Test
import java.math.BigDecimal

class PolkadotJsonRpcProviderTest {

    /**
     * Regression for the Bittensor (TAO) "Unreachable network fee" issue (Polkadot.Api 2001).
     *
     * This is the exact `TransactionPaymentApi_query_info` result captured from the finney runtime.
     * It is 17 bytes long: the `proofSize` weight crossed the 2^14 compact boundary, so it is encoded
     * as a 4-byte compact (instead of 2 bytes), and `partialFee` is a u64 (8 bytes).
     *
     * The previous implementation branched on a hardcoded response length of 15 bytes; for this
     * 17-byte response it fell through to the u128 path and `readUint128()` overran the buffer with an
     * ArrayIndexOutOfBoundsException, which was wrapped as Polkadot.Api (2001).
     */
    @Test
    fun parseFeeBittensorShortenedU64Response() {
        val resultHex = "0x82877af5a6f9010000c6ae080000000000"

        val fee = PolkadotJsonRpcProvider.parseFee(resultHex = resultHex, decimals = BITTENSOR_DECIMALS)

        // partialFee = 0x08AEC6 = 569030 rao -> 0.00056903 TAO
        Truth.assertThat(fee.compareTo(BigDecimal("0.00056903"))).isEqualTo(0)
    }

    /**
     * Standard Substrate response where `partialFee` is a u128 (16 bytes). Ensures the wide path
     * still decodes correctly after removing the fixed-length branch.
     *
     * Layout: refTime=1_000_000_000 (4-byte compact), proofSize=3593 (2-byte compact),
     * dispatchClass=0x00, partialFee=123456789 (u128 little-endian).
     */
    @Test
    fun parseFeeStandardU128Response() {
        val resultHex = "0x02286bee25380015cd5b07000000000000000000000000"

        val fee = PolkadotJsonRpcProvider.parseFee(resultHex = resultHex, decimals = POLKADOT_DECIMALS)

        // partialFee = 0x075BCD15 = 123456789 -> 0.0123456789 DOT
        Truth.assertThat(fee.compareTo(BigDecimal("0.0123456789"))).isEqualTo(0)
    }

    /**
     * Legacy 15-byte response (2-byte `proofSize` compact, u64 `partialFee`) that the original
     * implementation was tuned for. Must keep working.
     *
     * Layout: refTime=1_000_000_000 (4-byte compact), proofSize=100 (2-byte compact),
     * dispatchClass=0x00, partialFee=569030 (u64 little-endian).
     */
    @Test
    fun parseFeeLegacyFifteenByteResponse() {
        val resultHex = "0x02286bee910100c6ae080000000000"

        val fee = PolkadotJsonRpcProvider.parseFee(resultHex = resultHex, decimals = BITTENSOR_DECIMALS)

        Truth.assertThat(fee.compareTo(BigDecimal("0.00056903"))).isEqualTo(0)
    }

    private companion object {
        const val BITTENSOR_DECIMALS = 9
        const val POLKADOT_DECIMALS = 10
    }
}