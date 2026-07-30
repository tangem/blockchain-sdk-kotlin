package com.tangem.blockchain.transaction.staking

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import org.junit.Test

class StakingTransactionRecognizerTest {

    private val recognizer = StakingTransactionRecognizer

    // Cosmos: hex of "\n#/cosmos.staking.v1beta1.MsgDelegate" bytes — contains "/cosmos.staking."
    private val cosmosDelegateHex =
        "0a232f636f736d6f732e7374616b696e672e763162657461312e4d736744656c6567617465"

    // Cosmos: hex containing "/cosmos.bank." — a non-staking message
    private val cosmosBankHex = "0a0d2f636f736d6f732e62616e6b2e"

    // Cosmos: hex of an Any typeUrl "/cosmos.distribution.v1beta1.MsgWithdrawDelegatorReward" — the
    // reward-claim message, served by the distribution module (not staking).
    private val cosmosWithdrawRewardHex =
        "0a372f636f736d6f732e646973747269627574696f6e2e763162657461312e4d736757697468647261774" +
            "4656c656761746f72526577617264"

    // Cardano CBOR: [ { 4: [] } ]  → body has certificates key (4)
    private val cardanoCertHex = "81a10480"

    // Cardano CBOR: [ { 5: {} } ]  → body has withdrawals key (5)
    private val cardanoWithdrawalHex = "81a105a0"

    // Cardano CBOR: [ { 0: [], 1: [], 2: 0 } ]  → inputs/outputs/fee only, no 4/5
    private val cardanoTransferHex = "81a3008001800200"

    // region Tron
    @Test
    fun `GIVEN tron unfreeze tx WHEN recognize THEN true`() {
        val json = """{"raw_data":{"contract":[{"type":"UnfreezeBalanceV2Contract"}]}}"""
        assertThat(recognizer.isRecognizedStakingTransaction(Blockchain.Tron, json)).isTrue()
    }

    @Test
    fun `GIVEN tron freeze tx WHEN recognize THEN true`() {
        val json = """{"raw_data":{"contract":[{"type":"FreezeBalanceV2Contract"}]}}"""
        assertThat(recognizer.isRecognizedStakingTransaction(Blockchain.Tron, json)).isTrue()
    }

    @Test
    fun `GIVEN tron delegate resource tx WHEN recognize THEN true`() {
        val json = """{"raw_data":{"contract":[{"type":"DelegateResourceContract"}]}}"""
        assertThat(recognizer.isRecognizedStakingTransaction(Blockchain.Tron, json)).isTrue()
    }

    @Test
    fun `GIVEN tron vote witness tx WHEN recognize THEN true`() {
        val json = """{"raw_data":{"contract":[{"type":"VoteWitnessContract"}]}}"""
        assertThat(recognizer.isRecognizedStakingTransaction(Blockchain.Tron, json)).isTrue()
    }

    @Test
    fun `GIVEN tron trigger smart contract WHEN recognize THEN false`() {
        val json = """{"raw_data":{"contract":[{"type":"TriggerSmartContract"}]}}"""
        assertThat(recognizer.isRecognizedStakingTransaction(Blockchain.Tron, json)).isFalse()
    }

    @Test
    fun `GIVEN tron undelegate resource tx WHEN recognize THEN true`() {
        val json = """{"raw_data":{"contract":[{"type":"UnDelegateResourceContract"}]}}"""
        assertThat(recognizer.isRecognizedStakingTransaction(Blockchain.Tron, json)).isTrue()
    }

    @Test
    fun `GIVEN tron withdraw balance reward claim tx WHEN recognize THEN true`() {
        val json = """{"raw_data":{"contract":[{"type":"WithdrawBalanceContract"}]}}"""
        assertThat(recognizer.isRecognizedStakingTransaction(Blockchain.Tron, json)).isTrue()
    }

    @Test
    fun `GIVEN tron multi-contract all staking WHEN recognize THEN true`() {
        val json =
            """{"raw_data":{"contract":[{"type":"FreezeBalanceV2Contract"},{"type":"VoteWitnessContract"}]}}"""
        assertThat(recognizer.isRecognizedStakingTransaction(Blockchain.Tron, json)).isTrue()
    }

    @Test
    fun `GIVEN tron multi-contract with one non-staking WHEN recognize THEN false`() {
        val json =
            """{"raw_data":{"contract":[{"type":"FreezeBalanceV2Contract"},{"type":"TriggerSmartContract"}]}}"""
        assertThat(recognizer.isRecognizedStakingTransaction(Blockchain.Tron, json)).isFalse()
    }

    @Test
    fun `GIVEN tron empty contract list WHEN recognize THEN false`() {
        val json = """{"raw_data":{"contract":[]}}"""
        assertThat(recognizer.isRecognizedStakingTransaction(Blockchain.Tron, json)).isFalse()
    }

    @Test
    fun `GIVEN tron malformed json WHEN recognize THEN false`() {
        assertThat(recognizer.isRecognizedStakingTransaction(Blockchain.Tron, "not-json")).isFalse()
    }
    // endregion

    // region Cosmos
    @Test
    fun `GIVEN cosmos delegate tx WHEN recognize THEN true`() {
        assertThat(recognizer.isRecognizedStakingTransaction(Blockchain.Cosmos, cosmosDelegateHex)).isTrue()
    }

    @Test
    fun `GIVEN cosmos withdraw delegator reward tx WHEN recognize THEN true`() {
        assertThat(recognizer.isRecognizedStakingTransaction(Blockchain.Cosmos, cosmosWithdrawRewardHex)).isTrue()
    }

    @Test
    fun `GIVEN cosmos bank tx WHEN recognize THEN false`() {
        assertThat(recognizer.isRecognizedStakingTransaction(Blockchain.Cosmos, cosmosBankHex)).isFalse()
    }

    @Test
    fun `GIVEN cosmos invalid hex WHEN recognize THEN false`() {
        assertThat(recognizer.isRecognizedStakingTransaction(Blockchain.Cosmos, "zz")).isFalse()
    }
    // endregion

    // region Cardano
    @Test
    fun `GIVEN cardano tx with certificates key WHEN recognize THEN true`() {
        assertThat(recognizer.isRecognizedStakingTransaction(Blockchain.Cardano, cardanoCertHex)).isTrue()
    }

    @Test
    fun `GIVEN cardano tx with withdrawals key WHEN recognize THEN true`() {
        assertThat(recognizer.isRecognizedStakingTransaction(Blockchain.Cardano, cardanoWithdrawalHex)).isTrue()
    }

    @Test
    fun `GIVEN cardano plain transfer WHEN recognize THEN false`() {
        assertThat(recognizer.isRecognizedStakingTransaction(Blockchain.Cardano, cardanoTransferHex)).isFalse()
    }

    @Test
    fun `GIVEN cardano invalid cbor WHEN recognize THEN false`() {
        assertThat(recognizer.isRecognizedStakingTransaction(Blockchain.Cardano, "00")).isFalse()
    }

    // [ 258(<<{4:[]}>>), {} ] — body map wrapped in CBOR tag 258 (Conway-era set tag)
    @Test
    fun `GIVEN cardano body tagged 258 with certificates WHEN recognize THEN true`() {
        assertThat(recognizer.isRecognizedStakingTransaction(Blockchain.Cardano, "82d90102a10480a0")).isTrue()
    }
    // endregion

    // region Solana
    @Test
    fun `GIVEN solana staking tx with stake program in account keys WHEN recognize THEN true`() {
        val hex = solanaTx(accountCountHex = "02", accountKeysHex = "11".repeat(ACCOUNT_KEY_BYTES) + SOLANA_STAKE_KEY)
        assertThat(recognizer.isRecognizedStakingTransaction(Blockchain.Solana, hex)).isTrue()
    }

    @Test
    fun `GIVEN solana tx without stake program WHEN recognize THEN false`() {
        val hex = solanaTx(
            accountCountHex = "02",
            accountKeysHex = "11".repeat(ACCOUNT_KEY_BYTES) + "22".repeat(ACCOUNT_KEY_BYTES),
        )
        assertThat(recognizer.isRecognizedStakingTransaction(Blockchain.Solana, hex)).isFalse()
    }

    // Regression: the Stake program id appears only in instruction data, not among the account keys.
    // A substring scan wrongly accepted this; the account-keys parser must reject it.
    @Test
    fun `GIVEN solana stake program only in instruction data WHEN recognize THEN false`() {
        val accountKeys = "11".repeat(ACCOUNT_KEY_BYTES) + "22".repeat(ACCOUNT_KEY_BYTES)
        val instructionData = "33".repeat(ACCOUNT_KEY_BYTES) + SOLANA_STAKE_KEY
        val hex = solanaTx(accountCountHex = "02", accountKeysHex = accountKeys) + instructionData
        assertThat(recognizer.isRecognizedStakingTransaction(Blockchain.Solana, hex)).isFalse()
    }

    // 130 account keys → the shortvec length spans two bytes (0x82 0x01); stake key is the last one.
    @Test
    fun `GIVEN solana tx with many account keys and stake WHEN recognize THEN true`() {
        val keys = "aa".repeat(ACCOUNT_KEY_BYTES).repeat(FILLER_KEY_COUNT) + SOLANA_STAKE_KEY
        val hex = SIGNATURE_AND_HEADER_HEX + "8201" + keys
        assertThat(recognizer.isRecognizedStakingTransaction(Blockchain.Solana, hex)).isTrue()
    }

    // Declares 4 account keys but contains only one → malformed → false.
    @Test
    fun `GIVEN solana truncated account keys WHEN recognize THEN false`() {
        val hex = SIGNATURE_AND_HEADER_HEX + "04" + "11".repeat(ACCOUNT_KEY_BYTES)
        assertThat(recognizer.isRecognizedStakingTransaction(Blockchain.Solana, hex)).isFalse()
    }

    // Legacy Solana layout: 1 signature + 3-byte header + shortvec(account count) + account keys.
    private fun solanaTx(accountCountHex: String, accountKeysHex: String): String =
        SIGNATURE_AND_HEADER_HEX + accountCountHex + accountKeysHex
    // endregion

    // region EVM
    @Test
    fun `GIVEN polygon tx to stakekit contract WHEN recognize THEN true`() {
        val json = """{"to":"0x5e3Ef299fDDf15eAa0432E6e66473ace8c13D908","data":"0xe4457a8a"}"""
        assertThat(recognizer.isRecognizedStakingTransaction(Blockchain.Polygon, json)).isTrue()
    }

    @Test
    fun `GIVEN polygon tx to other contract WHEN recognize THEN false`() {
        val json = """{"to":"0x0000000000000000000000000000000000000000"}"""
        assertThat(recognizer.isRecognizedStakingTransaction(Blockchain.Polygon, json)).isFalse()
    }

    @Test
    fun `GIVEN polygon approve to POL token with stakekit spender WHEN recognize THEN true`() {
        val data = "0x095ea7b3" +
            "0000000000000000000000005e3Ef299fDDf15eAa0432E6e66473ace8c13D908" +
            "0000000000000000000000000000000000000000000000000de0b6b3a7640000"
        val json = """{"to":"0x455e53CBB86018Ac2B8092FdCd39d8444aFFC3F6","data":"$data"}"""
        assertThat(recognizer.isRecognizedStakingTransaction(Blockchain.Polygon, json)).isTrue()
    }

    @Test
    fun `GIVEN polygon approve to POL token with wrong spender WHEN recognize THEN false`() {
        val data = "0x095ea7b3" +
            "00000000000000000000000087870bca3f3fd6335c3f4ce8392d69350b4fa4e2" +
            "0000000000000000000000000000000000000000000000000de0b6b3a7640000"
        val json = """{"to":"0x455e53CBB86018Ac2B8092FdCd39d8444aFFC3F6","data":"$data"}"""
        assertThat(recognizer.isRecognizedStakingTransaction(Blockchain.Polygon, json)).isFalse()
    }

    // Regression guard: the old StakeKit contract (0x467585...) must NOT be accepted as spender.
    @Test
    fun `GIVEN polygon approve to POL token with OLD stakekit spender WHEN recognize THEN false`() {
        val data = "0x095ea7b3" +
            "000000000000000000000000467585AaEa860F9D8B3B43bb994E4Da8A93788a7" +
            "0000000000000000000000000000000000000000000000000de0b6b3a7640000"
        val json = """{"to":"0x455e53CBB86018Ac2B8092FdCd39d8444aFFC3F6","data":"$data"}"""
        assertThat(recognizer.isRecognizedStakingTransaction(Blockchain.Polygon, json)).isFalse()
    }

    @Test
    fun `GIVEN polygon tx to POL token but not approve method WHEN recognize THEN false`() {
        val data = "0xa9059cbb" +
            "0000000000000000000000005e3Ef299fDDf15eAa0432E6e66473ace8c13D908" +
            "0000000000000000000000000000000000000000000000000de0b6b3a7640000"
        val json = """{"to":"0x455e53CBB86018Ac2B8092FdCd39d8444aFFC3F6","data":"$data"}"""
        assertThat(recognizer.isRecognizedStakingTransaction(Blockchain.Polygon, json)).isFalse()
    }

    @Test
    fun `GIVEN polygon approve to POL token with too short data WHEN recognize THEN false`() {
        val json = """{"to":"0x455e53CBB86018Ac2B8092FdCd39d8444aFFC3F6","data":"0x095ea7b3"}"""
        assertThat(recognizer.isRecognizedStakingTransaction(Blockchain.Polygon, json)).isFalse()
    }

    @Test
    fun `GIVEN bsc tx to stakehub WHEN recognize THEN true`() {
        val json = """{"to":"0x0000000000000000000000000000000000002002"}"""
        assertThat(recognizer.isRecognizedStakingTransaction(Blockchain.BSC, json)).isTrue()
    }

    @Test
    fun `GIVEN bsc tx to other contract WHEN recognize THEN false`() {
        val json = """{"to":"0x1111111111111111111111111111111111111111"}"""
        assertThat(recognizer.isRecognizedStakingTransaction(Blockchain.BSC, json)).isFalse()
    }
    // endregion

    // region Unsupported
    @Test
    fun `GIVEN unsupported network ethereum WHEN recognize THEN false`() {
        assertThat(recognizer.isRecognizedStakingTransaction(Blockchain.Ethereum, "deadbeef")).isFalse()
    }
    // endregion

    private companion object {
        const val SOLANA_STAKE_KEY = "06a1d8179137542a983437bdfe2a7ab2557f535c8a78722b68a49dc000000000"
        const val ACCOUNT_KEY_BYTES = 32
        const val SIGNATURE_BYTES = 64
        const val FILLER_KEY_COUNT = 129

        // 1 signature (0x01 + 64 zero bytes) + 3-byte message header (0x01 0x00 0x01).
        val SIGNATURE_AND_HEADER_HEX = "01" + "00".repeat(SIGNATURE_BYTES) + "010001"
    }
}