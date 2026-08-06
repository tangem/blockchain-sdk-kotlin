package com.tangem.blockchain.blockchains.tron

import com.google.common.truth.Truth
import com.tangem.blockchain.blockchains.tron.network.BlockHeader
import com.tangem.blockchain.blockchains.tron.network.RawData
import com.tangem.blockchain.blockchains.tron.network.TronBlock
import com.tangem.blockchain.blockchains.tron.tokenmethods.TronTransferTokenCallData
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.smartcontract.CompiledSmartContractCallData
import com.tangem.blockchain.extensions.decodeBase58
import com.tangem.common.extensions.hexToBytes
import okio.ByteString.Companion.toByteString
import org.junit.Test
import org.tron.protos.Transaction
import org.tron.protos.contract.TransferContract
import org.tron.protos.contract.TriggerSmartContract
import java.math.BigDecimal

class TronTransactionTest {

    private val blockchain = Blockchain.TronTestnet
    private val transactionBuilder = TronTransactionBuilder()

    val tronBlock = TronBlock(
        blockHeader = BlockHeader(
            RawData(
                number = 3111739,
                txTrieRoot = "64288c2db0641316762a99dbb02ef7c90f968b60f9f2e410835980614332f86d",
                witnessAddress = "415863f6091b8e71766da808b1dd3159790f61de7d",
                parentHash = "00000000002f7b3af4f5f8b9e23a30c530f719f165b742e7358536b280eead2d",
                version = 3,
                timestamp = 1539295479000,
            ),
        ),
    )

    @Test
    fun textTransactionTransfer() {
        val transactionRaw = transactionBuilder.buildForSign(
            amount = Amount(BigDecimal.valueOf(1), blockchain),
            source = "TU1BRXbr6EmKmrLL4Kymv7Wp18eYFkRfAF",
            destination = "TXXxc9NsHndfQ2z9kMKyWpYa5T3QbhKGwn",
            block = tronBlock,
            extras = null,
        )
        val signature = (
            "6b5de85a80b2f4f02351f691593fb0e49f14c5cb42451373485357e42d7890cd77ad7bfcb733555c098b992da79dabe5050f5e2" +
                "db77d9d98f199074222de037701"
            ).hexToBytes()
        val transaction = transactionBuilder.buildForSend(transactionRaw, signature).encode()

        val expectedTransaction = (
            "0a85010a027b3b2208b21ace8d6ac20e7e40d8abb9bae62c5a67080112630a2d747970652e676f6f676c65617069732e636f6d2" +
                "f70726f746f636f6c2e5472616e73666572436f6e747261637412320a1541c5d1c75825b30bb2e2e655798209d56448eb6b" +
                "5e121541ec8c5a0fcbb28f14418eed9cf582af0d77e4256e18c0843d70d889a4a9e62c12416b5de85a80b2f4f02351f6915" +
                "93fb0e49f14c5cb42451373485357e42d7890cd77ad7bfcb733555c098b992da79dabe5050f5e2db77d9d98f199074222de" +
                "037701"
            ).hexToBytes()

        Truth.assertThat(transaction).isEqualTo(expectedTransaction)
    }

    @Test
    fun testTrc20Transfer() {
        val token = Token(
            name = "Tether",
            symbol = "USDT",
            contractAddress = "TXLAQ63Xg1NAzckPwKHvzw7CSEmLMEqcdj",
            decimals = 6,
        )
        val transactionRaw = transactionBuilder.buildForSign(
            amount = Amount(token, BigDecimal.ONE),
            source = "TU1BRXbr6EmKmrLL4Kymv7Wp18eYFkRfAF",
            destination = "TXXxc9NsHndfQ2z9kMKyWpYa5T3QbhKGwn",
            block = tronBlock,
            extras = TronTransactionExtras(
                TronTransferTokenCallData(
                    destination = "TXXxc9NsHndfQ2z9kMKyWpYa5T3QbhKGwn",
                    amount = Amount(token, BigDecimal.ONE),
                ),
            ),
        )
        val signature = (
            "6b5de85a80b2f4f02351f691593fb0e49f14c5cb42451373485357e42d7890cd77ad7bfcb733555c098b992da79dabe5050f5e2" +
                "db77d9d98f199074222de037701"
            ).hexToBytes()
        val transactionData = transactionBuilder.buildForSend(transactionRaw, signature).encode()

        val expectedTransactionData = (
            "0ad3010a027b3b2208b21ace8d6ac20e7e40d8abb9bae62c5aae01081f12a9010a31747970652e676f6f676c65617069732e636" +
                "f6d2f70726f746f636f6c2e54726967676572536d617274436f6e747261637412740a1541c5d1c75825b30bb2e2e6557982" +
                "09d56448eb6b5e121541ea51342dabbb928ae1e576bd39eff8aaf070a8c62244a9059cbb000000000000000000000041ec8" +
                "c5a0fcbb28f14418eed9cf582af0d77e4256e00000000000000000000000000000000000000000000000000000000000f42" +
                "4070d889a4a9e62c900180c2d72f12416b5de85a80b2f4f02351f691593fb0e49f14c5cb42451373485357e42d7890cd77a" +
                "d7bfcb733555c098b992da79dabe5050f5e2db77d9d98f199074222de037701"
            ).hexToBytes()

        Truth.assertThat(transactionData).isEqualTo(expectedTransactionData)
    }

    /**
     * A native-value DEX swap (EVM-format tx) built from a [TransactionData] must be a
     * [TriggerSmartContract] to the destination router — with call_value = amount and data = call
     * data — not a plain TransferContract.
     */
    @Test
    fun testSmartContractCallSwap() {
        val callData = "a9059cbb00000000000000000000000000000000000000000000000000000000000f4240".hexToBytes()
        val source = "TU1BRXbr6EmKmrLL4Kymv7Wp18eYFkRfAF"
        val router = "TXXxc9NsHndfQ2z9kMKyWpYa5T3QbhKGwn"
        val amount = Amount(BigDecimal.valueOf(5), blockchain) // native TRX (txValue)

        val transactionRaw = transactionBuilder.buildForSign(
            transaction = TransactionData.Uncompiled(
                amount = amount,
                fee = null,
                sourceAddress = source,
                destinationAddress = router,
                extras = TronTransactionExtras(CompiledSmartContractCallData(callData)),
            ),
            block = tronBlock,
        )

        val contract = transactionRaw.contract.single()
        Truth.assertThat(contract.type).isEqualTo(Transaction.Contract.ContractType.TriggerSmartContract)
        Truth.assertThat(transactionRaw.fee_limit).isEqualTo(TronTransactionBuilder.SMART_CONTRACT_FEE_LIMIT)

        val trigger = TriggerSmartContract.ADAPTER.decode(contract.parameter!!.value)
        Truth.assertThat(trigger.owner_address).isEqualTo(source.decodeBase58(checked = true)!!.toByteString())
        Truth.assertThat(trigger.contract_address).isEqualTo(router.decodeBase58(checked = true)!!.toByteString())
        Truth.assertThat(trigger.call_value).isEqualTo(amount.longValue)
        Truth.assertThat(trigger.data_).isEqualTo(callData.toByteString())
    }

    /**
     * A swap provider may describe a plain deposit transfer as a swap. With no call data to run the
     * transaction must stay a [TransferContract] with no fee limit — a TriggerSmartContract against
     * an ordinary account is rejected by the network.
     */
    @Test
    fun testSwapWithoutCallDataStaysTransfer() {
        val source = "TU1BRXbr6EmKmrLL4Kymv7Wp18eYFkRfAF"
        val destination = "TXXxc9NsHndfQ2z9kMKyWpYa5T3QbhKGwn"
        val amount = Amount(BigDecimal.valueOf(15), blockchain)

        val transactionRaw = transactionBuilder.buildForSign(
            transaction = TransactionData.Uncompiled(
                amount = amount,
                fee = null,
                sourceAddress = source,
                destinationAddress = destination,
                extras = TronTransactionExtras(callData = CompiledSmartContractCallData(byteArrayOf())),
            ),
            block = tronBlock,
        )

        val contract = transactionRaw.contract.single()
        Truth.assertThat(contract.type).isEqualTo(Transaction.Contract.ContractType.TransferContract)
        Truth.assertThat(transactionRaw.fee_limit).isEqualTo(0L)

        val transfer = TransferContract.ADAPTER.decode(contract.parameter!!.value)
        Truth.assertThat(transfer.owner_address).isEqualTo(source.decodeBase58(checked = true)!!.toByteString())
        Truth.assertThat(transfer.to_address).isEqualTo(destination.decodeBase58(checked = true)!!.toByteString())
        Truth.assertThat(transfer.amount).isEqualTo(amount.longValue)
    }

    /** A memo travels in the transaction-level `data` field, whatever the contract shape is. */
    @Test
    fun testMemoIsAttachedToTransfer() {
        val memo = "=:ETH.ETH:0xRecipient".encodeToByteArray()

        val transactionRaw = transactionBuilder.buildForSign(
            transaction = TransactionData.Uncompiled(
                amount = Amount(BigDecimal.valueOf(15), blockchain),
                fee = null,
                sourceAddress = "TU1BRXbr6EmKmrLL4Kymv7Wp18eYFkRfAF",
                destinationAddress = "TXXxc9NsHndfQ2z9kMKyWpYa5T3QbhKGwn",
                extras = TronTransactionExtras(memo = memo),
            ),
            block = tronBlock,
        )

        Truth.assertThat(transactionRaw.contract.single().type)
            .isEqualTo(Transaction.Contract.ContractType.TransferContract)
        Truth.assertThat(transactionRaw.data_).isEqualTo(memo.toByteString())
    }

    @Test
    fun testMemoIsAttachedToSmartContractCall() {
        val memo = "swap-id-42".encodeToByteArray()
        val callData = "a9059cbb".hexToBytes()

        val transactionRaw = transactionBuilder.buildForSign(
            transaction = TransactionData.Uncompiled(
                amount = Amount(BigDecimal.valueOf(5), blockchain),
                fee = null,
                sourceAddress = "TU1BRXbr6EmKmrLL4Kymv7Wp18eYFkRfAF",
                destinationAddress = "TXXxc9NsHndfQ2z9kMKyWpYa5T3QbhKGwn",
                extras = TronTransactionExtras(CompiledSmartContractCallData(callData), memo),
            ),
            block = tronBlock,
        )

        Truth.assertThat(transactionRaw.contract.single().type)
            .isEqualTo(Transaction.Contract.ContractType.TriggerSmartContract)
        Truth.assertThat(transactionRaw.data_).isEqualTo(memo.toByteString())
    }

    /** No memo must encode exactly as before, so existing transactions stay byte-for-byte equal. */
    @Test
    fun testNoMemoLeavesTransactionUnchanged() {
        fun build(extras: TronTransactionExtras?) = transactionBuilder.buildForSign(
            amount = Amount(BigDecimal.valueOf(1), blockchain),
            source = "TU1BRXbr6EmKmrLL4Kymv7Wp18eYFkRfAF",
            destination = "TXXxc9NsHndfQ2z9kMKyWpYa5T3QbhKGwn",
            block = tronBlock,
            extras = extras,
        ).encode()

        Truth.assertThat(build(TronTransactionExtras(memo = null))).isEqualTo(build(null))
    }
}