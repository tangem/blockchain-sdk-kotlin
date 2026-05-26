package com.tangem.blockchain.blockchains.bitcoin

import android.util.Base64
import android.util.Log
import android.util.Log.e
import com.tangem.blockchain.blockchains.bitcoin.address.BitcoinWalletAddressProvider
import com.tangem.blockchain.blockchains.bitcoin.messagesigning.BitcoinMessageSigner
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinAddressInfo
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinFee
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkProvider
import com.tangem.blockchain.blockchains.bitcoin.network.UsedAddress
import com.tangem.blockchain.blockchains.bitcoin.psbt.BitcoinPsbtProvider
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.DynamicAddressesManager
import com.tangem.blockchain.common.logging.Logger
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.crypto.NetworkType
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.common.transaction.TransactionSendResult
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.transactionhistory.DefaultTransactionHistoryProvider
import com.tangem.blockchain.transactionhistory.TransactionHistoryProvider
import com.tangem.blockchain.yieldsupply.DefaultYieldSupplyProvider
import com.tangem.blockchain.yieldsupply.YieldSupplyProvider
import com.tangem.common.CompletionResult
import com.tangem.common.extensions.toCompressedPublicKey
import com.tangem.common.extensions.toHexString
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.operations.sign.SignData
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.math.BigDecimal

@Suppress("LargeClass")
internal open class BitcoinWalletManager(
    wallet: Wallet,
    transactionHistoryProvider: TransactionHistoryProvider = DefaultTransactionHistoryProvider,
    protected val transactionBuilder: BitcoinTransactionBuilder,
    private val networkProvider: BitcoinNetworkProvider,
    private val feesCalculator: BitcoinFeesCalculator,
    yieldSupplyProvider: YieldSupplyProvider = DefaultYieldSupplyProvider,
) : WalletManager(
    wallet = wallet,
    transactionHistoryProvider = transactionHistoryProvider,
    yieldSupplyProvider = yieldSupplyProvider,
    messageSigner = BitcoinMessageSigner(wallet),
    psbtProvider = BitcoinPsbtProvider(wallet, networkProvider),
    addressProvider = BitcoinWalletAddressProvider(wallet),
),
    SignatureCountValidator,
    UtxoBlockchainManager,
    DynamicAddressesManager,
    MessageSigner {

    protected open val messageMagic: String? = null

    protected val blockchain = wallet.blockchain

    override val dustValue: BigDecimal = feesCalculator.minimalFeePerKb

    override val allowConsolidation: Boolean = true

    override val currentHost: String
        get() = networkProvider.baseUrl

    // ========== DynamicAddressesManager implementation ==========

    private var xpub: String? = null
    private var dynamicAddressesManager: BitcoinDynamicAddressesManager? = null
    private var xpubUsedAddresses: List<UsedAddress> = emptyList()

    override val isDynamicAddressesEnabled: Boolean
        get() = xpub != null && dynamicAddressesManager != null

    override val usedAddresses: List<UsedAddress>
        get() = xpubUsedAddresses

    override fun enableDynamicAddresses(xpub: String) {
        val networkType = if (blockchain.isTestnet()) NetworkType.Testnet else NetworkType.Mainnet
        val extendedPublicKey = ExtendedPublicKey.from(xpub, networkType)
        this.xpub = xpub
        this.dynamicAddressesManager = BitcoinDynamicAddressesManager(extendedPublicKey, blockchain)
    }

    override fun disableDynamicAddresses() {
        this.xpub = null
        this.dynamicAddressesManager = null
        this.xpubUsedAddresses = emptyList()
    }

    override fun findFirstUnusedReceiveAddress(): DynamicAddressesManager.DerivedAddress? {
        return dynamicAddressesManager?.findFirstUnusedReceiveAddress(xpubUsedAddresses)
    }

    override fun findFirstUnusedChangeAddress(): DynamicAddressesManager.DerivedAddress? {
        return dynamicAddressesManager?.findFirstUnusedChangeAddress(xpubUsedAddresses)
    }

    override suspend fun probeHasFundsOnNonBaseAddresses(xpub: String): Result<Boolean> {
        val descriptor = buildDescriptor(xpub)
        return when (val response = networkProvider.getInfoByXpub(descriptor)) {
            is Result.Success -> {
                val hasFunds = response.data.usedAddresses.any { it.balance > BigDecimal.ZERO && !it.isBase() }
                Result.Success(hasFunds)
            }
            is Result.Failure -> response
        }
    }

    private fun UsedAddress.isBase(): Boolean {
        val nodes = runCatching { DerivationPath(derivationPath).nodes }.getOrNull() ?: return false
        return nodes.size >= 2 &&
            nodes[nodes.size - 2].index == 0L &&
            nodes.last().index == 0L
    }

    /**
     * Build the BlockBook descriptor for XPUB queries.
     * SegWit networks (BTC, LTC) use wpkh() wrapper; legacy P2PKH networks use pkh().
     */
    fun buildDescriptor(xpub: String): String {
        return when (blockchain) {
            Blockchain.Bitcoin, Blockchain.BitcoinTestnet, Blockchain.Litecoin -> "wpkh($xpub)"
            else -> "pkh($xpub)" // Legacy P2PKH: DOGE, DASH, RVN, BCH, etc.
        }
    }

    override suspend fun updateInternal() {
        val currentXpub = xpub
        if (currentXpub != null && dynamicAddressesManager != null) {
            updateFromXpub(currentXpub)
        } else {
            updateFromSingleAddress()
        }
    }

    private suspend fun updateFromSingleAddress() {
        coroutineScope {
            val addressInfos = mutableListOf<BitcoinAddressInfo>()
            val responsesDeferred =
                wallet.addresses.map { async { networkProvider.getInfo(it.value) } }

            responsesDeferred.forEach {
                when (val response = it.await()) {
                    is Result.Success -> addressInfos.add(response.data)
                    is Result.Failure -> {
                        updateError(response.error)
                        return@coroutineScope
                    }
                }
            }
            updateWallet(addressInfos.merge())
        }
    }

    private suspend fun updateFromXpub(xpub: String) {
        val descriptor = buildDescriptor(xpub)
        when (val response = networkProvider.getInfoByXpub(descriptor)) {
            is Result.Success -> {
                val xpubInfo = response.data
                xpubUsedAddresses = xpubInfo.usedAddresses

                // Enrich UTXOs with derived public keys from DynamicAddressManager
                val enrichedUtxos = enrichUtxosWithPublicKeys(xpubInfo.unspentOutputs)

                val addressInfo = BitcoinAddressInfo(
                    balance = xpubInfo.balance,
                    unspentOutputs = enrichedUtxos,
                    recentTransactions = xpubInfo.recentTransactions,
                    hasUnconfirmed = xpubInfo.hasUnconfirmed,
                )
                updateWallet(addressInfo)
            }
            is Result.Failure -> updateError(response.error)
        }
    }

    /**
     * Enrich each UTXO with the derived public key based on its derivation path.
     * This is needed for [BitcoinTransactionBuilder.buildToSignMultiAddress] to use
     * per-input public keys for script creation.
     */
    private fun enrichUtxosWithPublicKeys(utxos: List<BitcoinUnspentOutput>): List<BitcoinUnspentOutput> {
        val manager = dynamicAddressesManager ?: return utxos
        return utxos.map { utxo ->
            if (utxo.publicKey != null || utxo.derivationPath == null) return@map utxo
            try {
                val (chain, index) = BitcoinDynamicAddressesManager.parseChainAndIndex(utxo.derivationPath)
                val derivedPubKey = manager.derivePublicKey(chain, index)
                BitcoinUnspentOutput(
                    amount = utxo.amount,
                    outputIndex = utxo.outputIndex,
                    transactionHash = utxo.transactionHash,
                    outputScript = utxo.outputScript,
                    address = utxo.address,
                    derivationPath = utxo.derivationPath,
                    publicKey = derivedPubKey,
                )
            } catch (e: Exception) {
                Logger.logNetwork("Failed to derive pubkey for UTXO path=${utxo.derivationPath}: ${e.message}")
                utxo
            }
        }
    }

    private fun List<BitcoinAddressInfo>.merge(): BitcoinAddressInfo {
        var balance = BigDecimal.ZERO
        val unspentOutputs = mutableListOf<BitcoinUnspentOutput>()
        val recentTransactions = mutableListOf<BasicTransactionData>()
        var hasUnconfirmed: Boolean? = false

        this.forEach {
            balance += it.balance
            unspentOutputs.addAll(it.unspentOutputs)
            recentTransactions.addAll(it.recentTransactions)
            hasUnconfirmed = if (hasUnconfirmed == null || it.hasUnconfirmed == null) {
                null
            } else {
                hasUnconfirmed!! || it.hasUnconfirmed
            }
        }

        // merge same transaction on different addresses
        val transactionsByHash = mutableMapOf<String, List<BasicTransactionData>>()
        recentTransactions.forEach { transaction ->
            val sameHashTransactions = recentTransactions.filter { it.hash == transaction.hash }
            transactionsByHash[transaction.hash] = sameHashTransactions
        }
        val finalTransactions = transactionsByHash.map {
            BasicTransactionData(
                balanceDif = it.value.sumOf { transaction -> transaction.balanceDif },
                hash = it.value[0].hash,
                date = it.value[0].date,
                isConfirmed = it.value[0].isConfirmed,
                destination = it.value[0].destination,
                source = it.value[0].source,
            )
        }
        return BitcoinAddressInfo(balance, unspentOutputs, finalTransactions, hasUnconfirmed)
    }

    internal fun updateWallet(response: BitcoinAddressInfo) {
        Log.d(this::class.java.simpleName, "Balance is ${response.balance}")

        wallet.changeAmountValue(AmountType.Coin, response.balance)
        transactionBuilder.unspentOutputs = response.unspentOutputs
        outputsCount = response.unspentOutputs.size

        if (response.recentTransactions.isNotEmpty()) {
            updateRecentTransactionsBasic(response.recentTransactions)
        } else {
            when (response.hasUnconfirmed) {
                true -> wallet.addTransactionDummy()
                else -> wallet.recentTransactions.clear()
            }
        }
    }

    internal fun updateError(error: BlockchainError) {
        Log.e(this::class.java.simpleName, error.customMessage)
        (error as? BlockchainSdkError)?.let { throw it }
    }

    override suspend fun send(
        transactionData: TransactionData,
        signer: TransactionSigner,
    ): Result<TransactionSendResult> {
        val utxos = transactionBuilder.unspentOutputs
        val utxoWithPath = utxos?.count { it.derivationPath != null && it.publicKey != null } ?: 0
        val isMultiAddressMode = xpub != null &&
            dynamicAddressesManager != null &&
            utxoWithPath > 0

        return if (isMultiAddressMode) {
            sendMultiAddress(transactionData, signer)
        } else {
            sendSingleAddress(transactionData, signer)
        }
    }

    private suspend fun sendSingleAddress(
        transactionData: TransactionData,
        signer: TransactionSigner,
    ): Result<TransactionSendResult> {
        when (val buildTransactionResult = transactionBuilder.buildToSign(transactionData, dustValue)) {
            is Result.Failure -> return buildTransactionResult
            is Result.Success -> {
                val hashes = buildTransactionResult.data
                return when (val signerResult = signer.sign(hashes, wallet.publicKey)) {
                    is CompletionResult.Success -> {
                        val transactionToSend = transactionBuilder.buildToSend(
                            signerResult.data.reduce { acc, bytes -> acc + bytes },
                        )
                        val sendResult = networkProvider.sendTransaction(transactionToSend.toHexString())

                        return when (sendResult) {
                            is SimpleResult.Success -> {
                                val txHash = transactionBuilder.getTransactionHash().toHexString()
                                wallet.addOutgoingTransaction(transactionData = transactionData, txHash = txHash)
                                Result.Success(TransactionSendResult(txHash))
                            }
                            is SimpleResult.Failure -> return Result.Failure(sendResult.error)
                        }
                    }
                    is CompletionResult.Failure -> Result.fromTangemSdkError(signerResult.error)
                }
            }
        }
    }

    /**
     * Send a transaction using multi-address signing. Each UTXO input may have a different
     * derived public key and derivation path.
     *
     * Uses [TransactionSigner.multiSign] which returns a Map keyed by per-input public key.
     * Note: if multiple UTXOs share the same derived address (same publicKey), the Map will
     * only retain the last signature. This is acceptable because in dynamic address mode,
     * UTXO selection naturally picks UTXOs from different addresses.
     */
    private suspend fun sendMultiAddress(
        transactionData: TransactionData,
        signer: TransactionSigner,
    ): Result<TransactionSendResult> {
        val changeAddress = dynamicAddressesManager
            ?.findFirstUnusedChangeAddress(xpubUsedAddresses)
            ?.address

        when (val buildResult = transactionBuilder.buildToSignMultiAddress(transactionData, dustValue, changeAddress)) {
            is Result.Failure -> return buildResult
            is Result.Success -> {
                val signatureInfos = buildResult.data

                val orderedSignatures = when (val signResult = signMultiAddressInputs(signatureInfos, signer)) {
                    is Result.Failure -> return signResult
                    is Result.Success -> signResult.data
                }

                val transactionToSend = transactionBuilder.buildToSendMultiAddress(
                    orderedSignatures,
                    signatureInfos,
                )
                val sendResult = networkProvider.sendTransaction(transactionToSend.toHexString())
                return when (sendResult) {
                    is SimpleResult.Success -> {
                        val txHash = transactionBuilder.getTransactionHash().toHexString()
                        wallet.addOutgoingTransaction(transactionData = transactionData, txHash = txHash)
                        Result.Success(TransactionSendResult(txHash))
                    }
                    is SimpleResult.Failure -> Result.Failure(sendResult.error)
                }
            }
        }
    }

    private suspend fun signMultiAddressInputs(
        signatureInfos: List<MultiAddressSignatureInfo>,
        signer: TransactionSigner,
    ): Result<List<ByteArray>> {
        val signDataList = signatureInfos.map { info ->
            SignData(
                derivationPath = info.derivationPath,
                hash = info.hash,
                publicKey = info.publicKey,
            )
        }

        return when (val signerResult = signer.multiSign(signDataList, wallet.publicKey)) {
            is CompletionResult.Success -> {
                // MultipleSignCommand processes SignData using ArrayDeque.removeLastOrNull(), so we should reverse
                // order manually
                val orderedSignatures = signerResult.data.values.toList().reversed()

                if (orderedSignatures.size != signatureInfos.size) {
                    return Result.Failure(
                        BlockchainSdkError.CustomError(
                            "Signature count mismatch: expected ${signatureInfos.size}, got ${orderedSignatures.size}",
                        ),
                    )
                }
                Result.Success(orderedSignatures)
            }
            is CompletionResult.Failure -> Result.fromTangemSdkError(signerResult.error)
        }
    }

    override suspend fun getFee(amount: Amount, destination: String): Result<TransactionFee> {
        try {
            when (val feeResult = getBitcoinFeePerKb()) {
                is Result.Failure -> return feeResult
                is Result.Success -> {
                    val feeValue = BigDecimal.ONE.movePointLeft(blockchain.decimals())

                    val newAmount = amount.copy(value = amount.value!! - feeValue)

                    val sizeResult = transactionBuilder.getEstimateSize(
                        transactionData = TransactionData.Uncompiled(
                            amount = newAmount,
                            fee = Fee.Common(Amount(newAmount, feeValue)),
                            sourceAddress = wallet.address,
                            destinationAddress = destination,
                        ),
                        dustValue = dustValue,
                    )

                    return when (sizeResult) {
                        is Result.Failure -> sizeResult
                        is Result.Success -> {
                            val fees = feesCalculator.calculateFees(
                                sizeResult.data.toBigDecimal(),
                                feeResult.data,
                            )
                            Result.Success(fees)
                        }
                    }
                }
            }
        } catch (exception: Exception) {
            return Result.Failure(exception.toBlockchainSdkError())
        }
    }

    override suspend fun validateSignatureCount(signedHashes: Int): SimpleResult {
        return when (val result = networkProvider.getSignatureCount(wallet.address)) {
            is Result.Success -> if (result.data == signedHashes) {
                SimpleResult.Success
            } else {
                SimpleResult.Failure(BlockchainSdkError.SignatureCountNotMatched)
            }
            is Result.Failure -> SimpleResult.Failure(result.error)
        }
    }

    protected open suspend fun getBitcoinFeePerKb(): Result<BitcoinFee> = networkProvider.getFee()

    override suspend fun signMessage(message: String, signer: TransactionSigner): CompletionResult<String> {
        val magic = messageMagic
            ?: return CompletionResult.Failure(BlockchainSdkError.CustomError("Message signing is not supported"))

        val messageHash = BitcoinMessageSignUtil.createMessageHash(
            message = message,
            messageMagic = magic,
        )

        return when (val signResult = signer.sign(messageHash, wallet.publicKey)) {
            is CompletionResult.Success -> {
                val signatureBytes = signResult.data
                val publicKey = wallet.publicKey.blockchainKey.toCompressedPublicKey()

                val recoverableSignature = BitcoinMessageSignUtil.createRecoverableSignature(
                    signatureBytes = signatureBytes,
                    publicKey = publicKey,
                    messageHash = messageHash,
                )

                val base64Signature = Base64.encodeToString(recoverableSignature, Base64.NO_WRAP)
                CompletionResult.Success(base64Signature)
            }
            is CompletionResult.Failure -> CompletionResult.Failure(signResult.error)
        }
    }

    // ========== Consolidation ==========

    /**
     * Create a consolidation transaction: all UTXOs → single output to base address.
     * @param fee The fee to use for the consolidation transaction.
     * @return TransactionData for signing and sending, or error if balance < fee.
     */
    override fun createConsolidationTransaction(fee: Fee): Result<TransactionData.Uncompiled> {
        val utxos = transactionBuilder.unspentOutputs
        if (utxos.isNullOrEmpty()) {
            return Result.Failure(BlockchainSdkError.CustomError("No UTXOs available for consolidation"))
        }

        val totalBalance = utxos.sumOf { it.amount }
        val feeAmount = fee.amount.value
            ?: return Result.Failure(BlockchainSdkError.CustomError("Fee amount is null"))

        if (totalBalance <= feeAmount) {
            return Result.Failure(BlockchainSdkError.CustomError("Balance too low to cover consolidation fee"))
        }

        val sendAmount = totalBalance - feeAmount
        val baseAddress = wallet.address // on m/.../0/0 derivation

        return Result.Success(
            TransactionData.Uncompiled(
                amount = Amount(sendAmount, blockchain, AmountType.Coin),
                fee = fee,
                sourceAddress = baseAddress,
                destinationAddress = baseAddress,
            ),
        )
    }

    companion object {
        // Synced value with iOS
        const val DEFAULT_MINIMAL_FEE_PER_KB = 0.00001
    }
}