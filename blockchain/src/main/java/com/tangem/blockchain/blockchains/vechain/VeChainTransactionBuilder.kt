package com.tangem.blockchain.blockchains.vechain

import com.google.protobuf.ByteString
import com.tangem.blockchain.blockchains.ethereum.tokenmethods.TransferERC20TokenMethod
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.extensions.trustWalletCoinType
import com.tangem.common.extensions.toDecompressedPublicKey
import org.kethereum.crypto.api.ec.ECDSASignature
import org.kethereum.crypto.determineRecId
import org.kethereum.crypto.impl.ec.canonicalise
import org.kethereum.extensions.removeLeadingZero
import org.kethereum.model.PublicKey
import org.kethereum.model.SignatureData
import wallet.core.jni.DataVector
import wallet.core.jni.TransactionCompiler
import wallet.core.jni.proto.Common
import wallet.core.jni.proto.TransactionCompiler.PreSigningOutput
import wallet.core.jni.proto.VeChain
import java.math.BigInteger

class VeChainTransactionBuilder(blockchain: Blockchain, private val publicKey: Wallet.PublicKey) {

    /**
     * “Chain tag is the last byte of the genesis block ID”.
     * Testnet blockId: 0x000000000b2bce3c70bc649a02749e8687721b09ed2e15997f466536b20bb127
     * Mainnet blockId: 0x00000000851caf3cfdb6e899cf5958bfb1ac3413d346d43539627e6be7ec1b4a
     */
    private val chainTag = if (blockchain.isTestnet()) 0x27 else 0x4a
    private val coinType = blockchain.trustWalletCoinType

    fun constructFee(amount: Amount, destination: String, vmGas: Long): TransactionFee {
        val toClause = buildClause(amount, destination)
        val gas = intrinsicGas(toClause, vmGas)
        return TransactionFee.Choosable(
            minimum = Fee.VeChain(
                amount = Amount(
                    token = VeChainWalletManager.VTHO_TOKEN,
                    value = gas.toBigDecimal().movePointLeft(Fee.VeChain.GAS_TO_VET_DECIMAL),
                ),
                gasPriceCoef = Fee.VeChain.MINIMUM_GAS_PRICE_COEFFICIENT,
                gasLimit = gas,
            ),
            normal = Fee.VeChain(
                amount = Amount(
                    token = VeChainWalletManager.VTHO_TOKEN,
                    value = (gas * Fee.VeChain.NORMAL_FEE_COEFFICIENT)
                        .toBigDecimal()
                        .movePointLeft(Fee.VeChain.GAS_TO_VET_DECIMAL),
                ),
                gasPriceCoef = Fee.VeChain.NORMAL_GAS_PRICE_COEFFICIENT,
                gasLimit = gas,
            ),
            priority = Fee.VeChain(
                amount = Amount(
                    token = VeChainWalletManager.VTHO_TOKEN,
                    value = (gas * Fee.VeChain.PRIORITY_FEE_COEFFICIENT)
                        .toBigDecimal()
                        .movePointLeft(Fee.VeChain.GAS_TO_VET_DECIMAL),
                ),
                gasPriceCoef = Fee.VeChain.PRIORITY_GAS_PRICE_COEFFICIENT,
                gasLimit = gas,
            ),
        )
    }

    fun buildForSign(transactionData: TransactionData, blockInfo: VeChainBlockInfo, nonce: Long): ByteArray {
        val fee = transactionData.fee as? Fee.VeChain ?: throw BlockchainSdkError.FailedToBuildTx
        val input =
            createSigningInput(transactionData.amount, fee, transactionData.destinationAddress, blockInfo, nonce)
        val preImageHashes = TransactionCompiler.preImageHashes(coinType, input.toByteArray())
        val preSigningOutput = PreSigningOutput.parseFrom(preImageHashes)

        if (preSigningOutput.error != Common.SigningError.OK) {
            throw BlockchainSdkError.FailedToBuildTx
        }

        return preSigningOutput.dataHash.toByteArray()
    }

    fun buildForSend(
        transactionData: TransactionData,
        hash: ByteArray,
        signature: ByteArray,
        blockInfo: VeChainBlockInfo,
        nonce: Long,
    ): ByteArray {
        val fee = transactionData.fee as? Fee.VeChain ?: throw BlockchainSdkError.FailedToBuildTx
        val inputData = createSigningInput(
            transactionData.amount,
            fee,
            transactionData.destinationAddress,
            blockInfo,
            nonce,
        )

        val publicKeys = DataVector()
        publicKeys.add(publicKey.blockchainKey.toDecompressedPublicKey())

        val signatures = DataVector()
        signatures.add(unmarshalSignature(signature, hash, publicKey))

        val compileWithSignatures = TransactionCompiler.compileWithSignatures(
            coinType,
            inputData.toByteArray(),
            signatures,
            publicKeys,
        )

        val output = VeChain.SigningOutput.parseFrom(compileWithSignatures)
        if (output.error != Common.SigningError.OK) {
            error("something went wrong")
        }

        return output.encoded.toByteArray()
    }

    private fun createSigningInput(
        amount: Amount,
        fee: Fee.VeChain,
        destination: String,
        blockInfo: VeChainBlockInfo,
        nonce: Long,
    ): VeChain.SigningInput {
        val clause = buildClause(amount, destination)

        return VeChain.SigningInput.newBuilder()
            .setChainTag(chainTag)
            .setNonce(nonce)
            .setBlockRef(blockInfo.blockRef)
            .setExpiration(EXPIRATION_BLOCKS)
            .setGas(fee.gasLimit)
            .setGasPriceCoef(fee.gasPriceCoef)
            .addClauses(clause)
            .build()
    }

    private fun intrinsicGas(clause: VeChain.Clause, vmGas: Long): Long {
        val data = clause.data.toStringUtf8()
        val dataCost = calculateDataCost(data)
        val vmInvocationCost = if (dataCost > 0) VM_INVOCATION_COST else 0

        return TX_GAS + CLAUSE_GAS + dataCost + vmInvocationCost + vmGas
    }

    private fun calculateDataCost(data: String): Long {
        return data
            .windowed(2, 2)
            .sumOf { if (it == "00") Z_GAS else NZ_GAS }
    }

    private fun buildClause(amount: Amount, destination: String): VeChain.Clause {
        val value = amount.value?.movePointRight(amount.decimals)?.toBigInteger() ?: BigInteger.ZERO
        return when (val type = amount.type) {
            is AmountType.Token -> {
                val token = type.token
                val data = TransferERC20TokenMethod(destination, value).data
                VeChain.Clause.newBuilder()
                    .setToBytes(ByteString.copyFromUtf8(token.contractAddress))
                    .setValue(ByteString.EMPTY)
                    .setData(ByteString.copyFrom(data))
                    .build()
            }

            AmountType.Coin -> {
                VeChain.Clause.newBuilder()
                    .setToBytes(ByteString.copyFromUtf8(destination))
                    .setValue(ByteString.copyFrom(value.toByteArray()))
                    .setData(ByteString.EMPTY)
                    .build()
            }

            AmountType.Reserve -> error("Not supported")
        }
    }

    private fun unmarshalSignature(signature: ByteArray, hash: ByteArray, publicKey: Wallet.PublicKey): ByteArray {
        val r = BigInteger(1, signature.copyOfRange(fromIndex = 0, toIndex = 32))
        val s = BigInteger(1, signature.copyOfRange(fromIndex = 32, toIndex = 64))

        val ecdsaSignature = ECDSASignature(r, s).canonicalise()

        val recId = ecdsaSignature.determineRecId(
            hash,
            PublicKey(
                publicKey = publicKey.blockchainKey.toDecompressedPublicKey()
                    .sliceArray(1..PUBLIC_KEY_LENGTH),
            ),
        )
        val signatureData = SignatureData(ecdsaSignature.r, ecdsaSignature.s, recId.toBigInteger())

        return signatureData.r.toByteArray().removeLeadingZero() +
            signatureData.s.toByteArray().removeLeadingZero() +
            signatureData.v.toByteArray()
    }

    private companion object {
        const val TX_GAS = 5_000L
        const val CLAUSE_GAS = 16_000L
        const val VM_INVOCATION_COST = 15_000L

        const val Z_GAS = 4L
        const val NZ_GAS = 68L

        const val EXPIRATION_BLOCKS = 180

        const val PUBLIC_KEY_LENGTH = 64
    }
}
