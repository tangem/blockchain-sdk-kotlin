package com.tangem.blockchain.transaction.staking

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.cosmos.proto.CosmosProtoMessage
import com.tangem.blockchain.blockchains.cosmos.proto.CosmosProtoMessage.CosmosMessageDelegate
import com.tangem.blockchain.blockchains.cosmos.proto.CosmosProtoMessage.CosmosMessageDelegateContainer
import com.tangem.blockchain.blockchains.cosmos.proto.CosmosProtoMessage.CosmosMessageFeeAndKeyContainer
import com.tangem.blockchain.blockchains.cosmos.proto.CosmosProtoMessage.CosmosMessageFeeContainer
import com.tangem.blockchain.blockchains.cosmos.proto.CosmosProtoMessage.CosmosMessagePublicKey
import com.tangem.blockchain.blockchains.cosmos.proto.CosmosProtoMessage.CosmosMessagePublicKeyContainer
import com.tangem.blockchain.blockchains.cosmos.proto.CosmosProtoMessage.CosmosMessagePublicKeyParam
import com.tangem.blockchain.blockchains.cosmos.proto.CosmosProtoMessage.CosmosMessagePublicKeyParamWrapper
import com.tangem.blockchain.blockchains.cosmos.proto.CosmosProtoMessage.CosmosMessagePublicKeyWrapper
import com.tangem.blockchain.blockchains.cosmos.proto.CosmosProtoMessage.DelegateAmount
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.extensions.toHexString
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import org.junit.Test
import org.tron.protos.Transaction

class StakingTransactionRecognizerTest {

    private val recognizer = StakingTransactionRecognizer

    // Cardano CBOR: [ { 4: [] } ]  → body has certificates key (4)
    private val cardanoCertHex = "81a10480"

    // Cardano CBOR: [ { 5: {} } ]  → body has withdrawals key (5)
    private val cardanoWithdrawalHex = "81a105a0"

    // Cardano CBOR: [ { 0: [], 1: [], 2: 0 } ]  → inputs/outputs/fee only, no 4/5
    private val cardanoTransferHex = "81a3008001800200"

    // region Tron
    @Test
    fun `GIVEN tron unfreeze tx WHEN recognize THEN true`() {
        assertThat(recognizer.isRecognizedStakingTransaction(Blockchain.Tron, tronJson(UNFREEZE_V2))).isTrue()
    }

    @Test
    fun `GIVEN tron freeze tx WHEN recognize THEN true`() {
        assertThat(recognizer.isRecognizedStakingTransaction(Blockchain.Tron, tronJson(FREEZE_V2))).isTrue()
    }

    @Test
    fun `GIVEN tron delegate resource tx WHEN recognize THEN true`() {
        assertThat(recognizer.isRecognizedStakingTransaction(Blockchain.Tron, tronJson(DELEGATE))).isTrue()
    }

    @Test
    fun `GIVEN tron vote witness tx WHEN recognize THEN true`() {
        assertThat(recognizer.isRecognizedStakingTransaction(Blockchain.Tron, tronJson(VOTE))).isTrue()
    }

    @Test
    fun `GIVEN tron undelegate resource tx WHEN recognize THEN true`() {
        assertThat(recognizer.isRecognizedStakingTransaction(Blockchain.Tron, tronJson(UNDELEGATE))).isTrue()
    }

    @Test
    fun `GIVEN tron withdraw balance reward claim tx WHEN recognize THEN true`() {
        assertThat(recognizer.isRecognizedStakingTransaction(Blockchain.Tron, tronJson(WITHDRAW_BALANCE))).isTrue()
    }

    @Test
    fun `GIVEN tron trigger smart contract WHEN recognize THEN false`() {
        assertThat(recognizer.isRecognizedStakingTransaction(Blockchain.Tron, tronJson(TRIGGER))).isFalse()
    }

    @Test
    fun `GIVEN tron multi-contract all staking WHEN recognize THEN true`() {
        assertThat(recognizer.isRecognizedStakingTransaction(Blockchain.Tron, tronJson(FREEZE_V2, VOTE))).isTrue()
    }

    @Test
    fun `GIVEN tron multi-contract with one non-staking WHEN recognize THEN false`() {
        assertThat(recognizer.isRecognizedStakingTransaction(Blockchain.Tron, tronJson(FREEZE_V2, TRIGGER))).isFalse()
    }

    @Test
    fun `GIVEN tron empty contract list WHEN recognize THEN false`() {
        assertThat(recognizer.isRecognizedStakingTransaction(Blockchain.Tron, tronJson())).isFalse()
    }

    @Test
    fun `GIVEN tron missing raw_data_hex WHEN recognize THEN false`() {
        val json = """{"raw_data":{"contract":[{"type":"FreezeBalanceV2Contract"}]}}"""
        assertThat(recognizer.isRecognizedStakingTransaction(Blockchain.Tron, json)).isFalse()
    }

    @Test
    fun `GIVEN tron malformed json WHEN recognize THEN false`() {
        assertThat(recognizer.isRecognizedStakingTransaction(Blockchain.Tron, "not-json")).isFalse()
    }

    @Test
    fun `GIVEN tron malformed raw_data_hex WHEN recognize THEN false`() {
        assertThat(recognizer.isRecognizedStakingTransaction(Blockchain.Tron, """{"raw_data_hex":"zzzz"}""")).isFalse()
    }

    // Regression: the human-readable raw_data JSON claims a staking op, but the signed raw_data_hex
    // encodes a TransferContract. Validation must follow the signed bytes, not the description.
    @Test
    fun `GIVEN tron tampered raw_data_hex not matching raw_data WHEN recognize THEN false`() {
        val tamperedHex = Transaction.raw(
            contract = listOf(Transaction.Contract(type = TRANSFER)),
        ).encode().toHexString()
        val json = """{"raw_data":{"contract":[{"type":"VoteWitnessContract"}]},"raw_data_hex":"$tamperedHex"}"""
        assertThat(recognizer.isRecognizedStakingTransaction(Blockchain.Tron, json)).isFalse()
    }

    // Builds the StakeKit-shaped payload the recognizer receives: a JSON object whose raw_data_hex is the
    // protobuf-encoded Transaction.raw that actually gets signed.
    private fun tronJson(vararg types: Transaction.Contract.ContractType): String {
        val raw = Transaction.raw(contract = types.map { Transaction.Contract(type = it) })
        return """{"raw_data_hex":"${raw.encode().toHexString()}"}"""
    }
    // endregion

    // region Cosmos
    @Test
    fun `GIVEN cosmos delegate tx WHEN recognize THEN true`() {
        val hex = cosmosHex(messageType = "/cosmos.staking.v1beta1.MsgDelegate")
        assertThat(recognizer.isRecognizedStakingTransaction(Blockchain.Cosmos, hex)).isTrue()
    }

    @Test
    fun `GIVEN cosmos undelegate tx WHEN recognize THEN true`() {
        val hex = cosmosHex(messageType = "/cosmos.staking.v1beta1.MsgUndelegate")
        assertThat(recognizer.isRecognizedStakingTransaction(Blockchain.Cosmos, hex)).isTrue()
    }

    @Test
    fun `GIVEN cosmos withdraw delegator reward tx WHEN recognize THEN true`() {
        val hex = cosmosHex(messageType = "/cosmos.distribution.v1beta1.MsgWithdrawDelegatorReward")
        assertThat(recognizer.isRecognizedStakingTransaction(Blockchain.Cosmos, hex)).isTrue()
    }

    @Test
    fun `GIVEN cosmos bank send tx WHEN recognize THEN false`() {
        val hex = cosmosHex(messageType = "/cosmos.bank.v1beta1.MsgSend")
        assertThat(recognizer.isRecognizedStakingTransaction(Blockchain.Cosmos, hex)).isFalse()
    }

    // Regression: a non-staking message must not be accepted just because the staking marker appears in
    // another field (here the stakingProvider string). Recognition reads the dedicated messageType field.
    @Test
    fun `GIVEN cosmos bank send with staking marker in provider field WHEN recognize THEN false`() {
        val hex = cosmosHex(
            messageType = "/cosmos.bank.v1beta1.MsgSend",
            stakingProvider = "spoof /cosmos.staking. marker",
        )
        assertThat(recognizer.isRecognizedStakingTransaction(Blockchain.Cosmos, hex)).isFalse()
    }

    @Test
    fun `GIVEN cosmos invalid hex WHEN recognize THEN false`() {
        assertThat(recognizer.isRecognizedStakingTransaction(Blockchain.Cosmos, "zz")).isFalse()
    }

    // Builds a full CosmosProtoMessage (the same structure the signer decodes) protobuf-encoded to hex.
    @OptIn(ExperimentalSerializationApi::class)
    private fun cosmosHex(messageType: String, stakingProvider: String = "via StakeKit CID-1009"): String {
        val message = CosmosProtoMessage(
            delegateContainer = CosmosMessageDelegateContainer(
                delegate = CosmosMessageDelegate(messageType = messageType, delegateData = ByteArray(size = 0)),
                stakingProvider = stakingProvider,
            ),
            feeAndKeyContainer = CosmosMessageFeeAndKeyContainer(
                publicKeyContainer = CosmosMessagePublicKeyContainer(
                    publicKeyWrapper = CosmosMessagePublicKeyWrapper(
                        publicKeyType = "/cosmos.crypto.secp256k1.PubKey",
                        publicKey = CosmosMessagePublicKey(publicKey = ByteArray(size = 0)),
                    ),
                    publicKeyParamWrapper = CosmosMessagePublicKeyParamWrapper(
                        publicKeyParam = CosmosMessagePublicKeyParam(param = 1),
                    ),
                ),
                feeContainer = CosmosMessageFeeContainer(
                    feeAmount = DelegateAmount(denomination = "uatom", amount = "6565"),
                    gas = 656461L,
                ),
            ),
            chainId = "cosmoshub-4",
            accountNumber = 1,
        )
        return ProtoBuf.encodeToByteArray(message).toHexString()
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

        val FREEZE_V2 = Transaction.Contract.ContractType.FreezeBalanceV2Contract
        val UNFREEZE_V2 = Transaction.Contract.ContractType.UnfreezeBalanceV2Contract
        val DELEGATE = Transaction.Contract.ContractType.DelegateResourceContract
        val UNDELEGATE = Transaction.Contract.ContractType.UnDelegateResourceContract
        val VOTE = Transaction.Contract.ContractType.VoteWitnessContract
        val WITHDRAW_BALANCE = Transaction.Contract.ContractType.WithdrawBalanceContract
        val TRIGGER = Transaction.Contract.ContractType.TriggerSmartContract
        val TRANSFER = Transaction.Contract.ContractType.TransferContract
    }
}