package com.tangem.blockchain.blockchains.koinos

import com.tangem.blockchain.blockchains.koinos.network.KoinosNetworkService
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.common.transaction.TransactionSendResult
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.successOr
import com.tangem.blockchain.transactionhistory.TransactionHistoryProvider
import java.math.BigDecimal
import java.util.EnumSet

internal class KoinosWalletManager(
    wallet: Wallet,
    transactionHistoryProvider: TransactionHistoryProvider,
    private val networkService: KoinosNetworkService,
    private val transactionBuilder: KoinosTransactionBuilder,
) : WalletManager(wallet = wallet, transactionHistoryProvider = transactionHistoryProvider),
    FeeResourceAmountProvider,
    TransactionValidator {

    override val currentHost: String
        get() = networkService.baseUrl

    private val contractIdHolder = KoinosContractIdHolder { networkService.getContractId() }

    override suspend fun updateInternal() {
        val accountInfo = networkService.getInfo(wallet.address, contractIdHolder)
            .successOr { return }

        val unconfirmedOutgoingTransaction = wallet.recentTransactions.firstOrNull {
            it.status == TransactionStatus.Unconfirmed
        }

        if (unconfirmedOutgoingTransaction != null && wallet.getCoinAmount().value != accountInfo.koinBalance) {
            val recentTransactions = networkService.getTransactionHistory(
                address = wallet.address,
                pageSize = 20,
                sequenceNum = 0,
            ).successOr { null }

            if (recentTransactions != null && recentTransactions.any { it.id == unconfirmedOutgoingTransaction.hash }) {
                unconfirmedOutgoingTransaction.status = TransactionStatus.Confirmed
            }
        }

        wallet.setAmount(
            value = accountInfo.koinBalance,
            amountType = AmountType.Coin,
        )
        wallet.setAmount(
            value = accountInfo.mana,
            maxValue = accountInfo.maxMana,
            amountType = AmountType.FeeResource(),
        )
    }

    @Deprecated("Will be removed in the future. Use TransactionValidator instead")
    override fun validateTransaction(amount: Amount, fee: Amount?): EnumSet<TransactionError> {
        val errors = EnumSet.noneOf(TransactionError::class.java)

        if (fee?.value != null && amount.value != null) {
            val currentMana = wallet.amounts[AmountType.FeeResource()]?.value ?: BigDecimal.ZERO
            val availableBalanceForTransfer = currentMana - fee.value

            if (amount.value > availableBalanceForTransfer) {
                errors.add(TransactionError.AmountExceedsBalance)
            }
            if (currentMana < fee.value) {
                errors.add(TransactionError.FeeExceedsBalance)
            }
        }

        return super.validateTransaction(amount, fee).apply { addAll(errors) }
    }

    override suspend fun validate(transactionData: TransactionData): kotlin.Result<Unit> {
        transactionData.requireUncompiled()

        val fee = transactionData.fee?.amount?.value
            ?: return kotlin.Result.failure(BlockchainSdkError.FailedToLoadFee)
        val currentMana = wallet.amounts[AmountType.FeeResource()]?.value ?: BigDecimal.ZERO
        val amount = transactionData.amount.value
            ?: return kotlin.Result.failure(BlockchainSdkError.FailedToLoadFee)
        val availableBalanceForTransfer = currentMana - fee
        val balance = wallet.amounts[AmountType.Coin]?.value ?: BigDecimal.ZERO

        return when {
            balance < fee -> {
                kotlin.Result.failure(BlockchainSdkError.Koinos.InsufficientBalance)
            }
            currentMana < fee -> {
                val maxMana = wallet.amounts[AmountType.FeeResource()]?.maxValue ?: BigDecimal.ZERO
                kotlin.Result.failure(
                    BlockchainSdkError.Koinos.InsufficientMana(manaBalance = currentMana, maxMana = maxMana),
                )
            }
            amount > availableBalanceForTransfer -> {
                kotlin.Result.failure(BlockchainSdkError.Koinos.ManaFeeExceedsBalance(availableBalanceForTransfer))
            }
            else -> kotlin.Result.success(Unit)
        }
    }

    override suspend fun send(
        transactionData: TransactionData,
        signer: TransactionSigner,
    ): Result<TransactionSendResult> {
        transactionData.requireUncompiled()

        validate(transactionData).onFailure {
            Result.Failure(it as? BlockchainSdkError ?: BlockchainSdkError.FailedToBuildTx)
        }

        val manaLimit = transactionData.fee?.amount?.value
            ?: return Result.Failure(BlockchainSdkError.FailedToBuildTx)

        val nonce = networkService.getCurrentNonce(wallet.address)
            .successOr { return Result.Failure(it.error) }

        val transactionDataWithMana = transactionData.copy(extras = KoinosTransactionExtras(manaLimit))

        val (transaction, hashToSign) = transactionBuilder.buildToSign(
            transactionData = transactionDataWithMana,
            currentNonce = nonce,
            contractIdHolder = contractIdHolder,
        ).successOr { return Result.Failure(it.error) }

        val signature = signer.sign(hashToSign, wallet.publicKey)
            .successOr { return Result.fromTangemSdkError(it.error) }

        val signedTransaction = transactionBuilder.buildToSend(
            transaction = transaction,
            signature = signature,
            hashToSign = hashToSign,
            publicKey = wallet.publicKey,
        )

        val transactionRes = networkService.submitTransaction(signedTransaction)
            .successOr { return it }

        val hash = transactionRes.id
        transactionData.hash = hash
        wallet.addOutgoingTransaction(
            transactionData.copy(
                hash = hash,
            ),
        )

        return Result.Success(TransactionSendResult(hash))
    }

    override suspend fun getFee(amount: Amount, destination: String): Result<TransactionFee> {
        val rcLimit = networkService.getRCLimit()
            .successOr { return it }

        val feeAmount = Fee.Common(
            amount = Amount(
                value = rcLimit,
                blockchain = Blockchain.Koinos,
                type = AmountType.FeeResource(),
                currencySymbol = "Mana",
            ),
        )

        return Result.Success(TransactionFee.Single(normal = feeAmount))
    }

    override fun getFeeResource(): FeeResourceAmountProvider.FeeResource {
        val amount = wallet.amounts[AmountType.FeeResource()]
        return FeeResourceAmountProvider.FeeResource(
            value = amount?.value ?: BigDecimal.ZERO,
            maxValue = amount?.maxValue ?: BigDecimal.ZERO,
        )
    }

    override fun getFeeResourceByName(name: String): FeeResourceAmountProvider.FeeResource = getFeeResource()

    override suspend fun isFeeEnough(amount: BigDecimal, feeName: String?): Boolean {
        val rcLimit = networkService.getRCLimit()
            .successOr { return false }
        val currentMana = wallet.amounts[AmountType.FeeResource()]?.value ?: BigDecimal.ZERO

        return amount < currentMana - rcLimit && currentMana >= rcLimit
    }

    override fun isFeeSubtractableFromAmount(): Boolean = true
}