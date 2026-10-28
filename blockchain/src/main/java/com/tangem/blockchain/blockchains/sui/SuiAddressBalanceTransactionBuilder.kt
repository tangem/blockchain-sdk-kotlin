package com.tangem.blockchain.blockchains.sui

import com.tangem.blockchain.blockchains.sui.bcs.SuiArgument
import com.tangem.blockchain.blockchains.sui.bcs.SuiCallArg
import com.tangem.blockchain.blockchains.sui.bcs.SuiCommand
import com.tangem.blockchain.blockchains.sui.bcs.SuiObjectRef
import com.tangem.blockchain.blockchains.sui.bcs.SuiTransactionData
import com.tangem.blockchain.blockchains.sui.bcs.SuiTransactionExpiration
import com.tangem.blockchain.blockchains.sui.bcs.SuiTypeTag
import com.tangem.blockchain.blockchains.sui.bcs.toU64Bits
import com.tangem.blockchain.blockchains.sui.model.SuiCoin
import com.tangem.blockchain.blockchains.sui.model.SuiWalletInfo
import com.tangem.blockchain.blockchains.sui.network.SuiConstants.COIN_TYPE
import com.tangem.blockchain.blockchains.sui.network.SuiConstants.SUI_GAS_BUDGET_MAX_VALUE
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.successOr
import okio.ByteString.Companion.toByteString
import java.math.BigDecimal
import kotlin.random.Random

/**
 * Builds transfers that spend funds held in the address balance (SIP-58). WalletCore only takes `Coin<T>` objects as
 * inputs, so these transactions are assembled here: the address balance is withdrawn with a `FundsWithdrawal` input
 * and turned into a coin by `0x2::coin::redeem_funds`, and gas is paid either by the coin objects or, when most of
 * SUI sits in the address balance, by the address balance itself.
 *
 * Transfers the coin objects cover alone are left to WalletCore: the `buildFor*` entry points return null for them.
 * Amounts are in the smallest units (MIST for SUI).
 */
internal class SuiAddressBalanceTransactionBuilder(
    private val walletAddress: String,
    private val chainIdentifier: String,
    private val nonceProvider: () -> Long = { Random.nextLong(from = 0, until = MAX_NONCE) },
) {

    /** Returns the base64 transaction to simulate, or null when the coin objects cover the transfer */
    fun buildForDryRun(
        walletInfo: SuiWalletInfo,
        amount: Amount,
        destination: String,
        gasPrice: Long,
        epoch: Long,
    ): Result<String>? = encode(error = BlockchainSdkError.FailedToLoadFee) {
        when (val type = amount.type) {
            AmountType.Coin -> {
                val amountMist = amount.value?.movePointRight(Blockchain.Sui.decimals())
                    ?: return Result.Failure(BlockchainSdkError.FailedToLoadFee)
                val totalBalanceMist = walletInfo.mistBalanceOf(COIN_TYPE)
                val isPayAll = amountMist == totalBalanceMist
                val decisionBudget = minOf(
                    totalBalanceMist - if (isPayAll) BigDecimal.ONE else amountMist,
                    SUI_GAS_BUDGET_MAX_VALUE,
                )
                if (!isAddressBalanceNeededForCoin(walletInfo, amountMist, decisionBudget)) return null

                val simulatedAmount = if (isPayAll) payAllSimulationAmount(walletInfo) else amountMist
                val gasBudget = maxGasBudgetForCoin(walletInfo, simulatedAmount)
                buildCoinTransfer(
                    walletInfo = walletInfo,
                    amount = simulatedAmount,
                    recipient = destination,
                    gas = SuiGas(budget = gasBudget, price = gasPrice, epoch = epoch),
                )
            }
            is AmountType.Token -> {
                val tokenAmount = amount.value?.movePointRight(type.token.decimals)
                    ?: return Result.Failure(BlockchainSdkError.FailedToLoadFee)
                val coinType = type.token.contractAddress
                if (!isAddressBalanceNeededForToken(walletInfo, coinType, tokenAmount)) return null

                buildTokenTransfer(
                    walletInfo = walletInfo,
                    coinType = coinType,
                    amount = tokenAmount,
                    recipient = destination,
                    gas = SuiGas(budget = maxGasBudgetForToken(walletInfo), price = gasPrice, epoch = epoch),
                )
            }
            else -> return null
        }
    }

    /** Returns the base64 transaction to sign, or null when the coin objects cover the transfer */
    suspend fun buildForSend(
        walletInfo: SuiWalletInfo,
        transaction: TransactionData.Uncompiled,
        getEpoch: suspend () -> Result<Long>,
    ): Result<String>? {
        val fee = transaction.fee as? Fee.Sui ?: return null
        val amount = transaction.amount
        val gasBudget = fee.gasBudget.toBigDecimal()
        val epoch = if (isGasPaidByCoins(walletInfo)) null else getEpoch().successOr { return it }

        return encode(error = BlockchainSdkError.FailedToBuildTx) {
            when (val type = amount.type) {
                AmountType.Coin -> {
                    val amountMist = amount.value?.movePointRight(Blockchain.Sui.decimals()) ?: return null
                    if (!isAddressBalanceNeededForCoin(walletInfo, amountMist, gasBudget)) return null

                    buildCoinTransfer(
                        walletInfo = walletInfo,
                        amount = amountMist,
                        recipient = transaction.destinationAddress,
                        gas = SuiGas(budget = gasBudget, price = fee.gasPrice, epoch = epoch),
                    )
                }
                is AmountType.Token -> {
                    val tokenAmount = amount.value?.movePointRight(type.token.decimals) ?: return null
                    val coinType = type.token.contractAddress
                    if (!isAddressBalanceNeededForToken(walletInfo, coinType, tokenAmount)) return null

                    buildTokenTransfer(
                        walletInfo = walletInfo,
                        coinType = coinType,
                        amount = tokenAmount,
                        recipient = transaction.destinationAddress,
                        gas = SuiGas(budget = gasBudget, price = fee.gasPrice, epoch = epoch),
                    )
                }
                else -> return null
            }
        }
    }

    /**
     * The builder is only reached with data the node returned, yet a malformed coin type, object id or digest, or an
     * amount out of the u64 range must fail the transaction rather than crash the app
     */
    private inline fun encode(error: BlockchainSdkError, build: () -> SuiTransactionData?): Result<String> {
        return try {
            val transaction = build() ?: return Result.Failure(error)
            Result.Success(transaction.toBcs().toByteString().base64())
        } catch (e: IllegalArgumentException) {
            Result.Failure(BlockchainSdkError.WrappedThrowable(e))
        } catch (e: ArithmeticException) {
            Result.Failure(BlockchainSdkError.WrappedThrowable(e))
        }
    }

    /** Whether a SUI transfer of [amount] with [gasBudget] can't be covered by the coin objects alone */
    fun isAddressBalanceNeededForCoin(walletInfo: SuiWalletInfo, amount: BigDecimal, gasBudget: BigDecimal): Boolean {
        return walletInfo.mistAddressBalanceOf(COIN_TYPE) > BigDecimal.ZERO &&
            walletInfo.suiCoinsBalanceMist() < amount + gasBudget
    }

    /**
     * Whether a token transfer has to touch the address balance: the token coins can't cover [amount], or most of SUI
     * sits in the address balance, so that no single gas coin is worth more.
     */
    fun isAddressBalanceNeededForToken(walletInfo: SuiWalletInfo, coinType: String, amount: BigDecimal): Boolean {
        val tokenCoinsBalance = walletInfo.coinsOf(coinType).sumOf { it.mistBalance }
        val largestSuiCoin = walletInfo.coinsOf(COIN_TYPE).maxOfOrNull { it.mistBalance } ?: BigDecimal.ZERO

        return walletInfo.mistAddressBalanceOf(coinType) > BigDecimal.ZERO && tokenCoinsBalance < amount ||
            walletInfo.mistAddressBalanceOf(COIN_TYPE) > largestSuiCoin
    }

    /**
     * The gas budget a SUI transfer of [amount] is simulated with. With gas paid by the address balance it is half
     * of what is left after the withdrawal: once the budget reaches that remainder, e.g. because the balance dropped
     * meanwhile, the node simulates with a stand-in gas coin and overcharges its storage.
     */
    fun maxGasBudgetForCoin(walletInfo: SuiWalletInfo, amount: BigDecimal): BigDecimal {
        val coinsBalance = walletInfo.suiCoinsBalanceMist()
        val addressBalance = walletInfo.mistAddressBalanceOf(COIN_TYPE)
        val gasSourceBalance = if (isGasPaidByCoins(walletInfo)) {
            walletInfo.gasCoinsBalanceMist()
        } else {
            (addressBalance - (amount - coinsBalance).coerceAtLeast(BigDecimal.ZERO)).halfOf()
        }

        return minOf(gasSourceBalance, coinsBalance + addressBalance - amount, SUI_GAS_BUDGET_MAX_VALUE)
    }

    /**
     * The amount a pay-all SUI transfer is simulated with. It is sent later as the total minus the fee, so the
     * simulated transfer withdraws from the address balance too and has the same shape.
     */
    fun payAllSimulationAmount(walletInfo: SuiWalletInfo): BigDecimal {
        val coinsBalance = walletInfo.suiCoinsBalanceMist()
        val addressBalance = walletInfo.mistAddressBalanceOf(COIN_TYPE)

        return if (isGasPaidByCoins(walletInfo)) {
            addressBalance + coinsBalance - minOf(walletInfo.gasCoinsBalanceMist(), SUI_GAS_BUDGET_MAX_VALUE)
        } else {
            coinsBalance + addressBalance.halfOf()
        }
    }

    /** The gas budget a token transfer is simulated with, see [maxGasBudgetForCoin] */
    fun maxGasBudgetForToken(walletInfo: SuiWalletInfo): BigDecimal {
        val gasSourceBalance = if (isGasPaidByCoins(walletInfo)) {
            walletInfo.gasCoinsBalanceMist()
        } else {
            walletInfo.mistAddressBalanceOf(COIN_TYPE).halfOf()
        }

        return minOf(gasSourceBalance, SUI_GAS_BUDGET_MAX_VALUE)
    }

    fun buildCoinTransfer(
        walletInfo: SuiWalletInfo,
        amount: BigDecimal,
        recipient: String,
        gas: SuiGas,
    ): SuiTransactionData? {
        if (amount <= BigDecimal.ZERO || gas.budget <= BigDecimal.ZERO) return null

        val coins = walletInfo.suiCoinsLargestFirst()
        val coinsBalance = coins.sumOf { it.mistBalance }
        val addressBalance = walletInfo.mistAddressBalanceOf(COIN_TYPE)

        if (isGasPaidByCoins(walletInfo)) {
            val gasCoins = coins.take(MAX_GAS_PAYMENT_OBJECTS)
            // Only the gas coins are checked against the budget before execution, the rest is merged in later
            val withdrawal = amount + gas.budget - coinsBalance
            if (gasCoins.sumOf { it.mistBalance } < gas.budget || withdrawal > addressBalance) return null

            val ptb = PtbBuilder()
            val restCoins = coins.drop(MAX_GAS_PAYMENT_OBJECTS)
            val restCoinInputs = restCoins.map { ptb.input(SuiCallArg.OwnedObject(it.toObjectRef())) }
            val redeemed = if (withdrawal > BigDecimal.ZERO) ptb.redeem(withdrawal, SUI_TYPE) else null
            val sources = restCoinInputs + listOfNotNull(redeemed)
            if (sources.isNotEmpty()) ptb.command(SuiCommand.MergeCoins(SuiArgument.GasCoin, sources))
            ptb.transfer(coin = SuiArgument.GasCoin, amount = amount, recipient = recipient)

            return ptb.build(gasPayment = gasCoins, gas = gas)
        }

        val withdrawal = (amount - coinsBalance).coerceAtLeast(BigDecimal.ZERO)
        if (withdrawal + gas.budget > addressBalance) return null

        val ptb = PtbBuilder()
        ptb.transferFunds(
            coins = coins,
            withdrawal = withdrawal,
            coinType = SUI_TYPE,
            amount = amount,
            recipient = recipient,
        )

        return ptb.build(gasPayment = emptyList(), gas = gas)
    }

    fun buildTokenTransfer(
        walletInfo: SuiWalletInfo,
        coinType: String,
        amount: BigDecimal,
        recipient: String,
        gas: SuiGas,
    ): SuiTransactionData? {
        if (amount <= BigDecimal.ZERO || gas.budget <= BigDecimal.ZERO) return null

        val tokenCoins = walletInfo.coinsOf(coinType)
        val withdrawal = (amount - tokenCoins.sumOf { it.mistBalance }).coerceAtLeast(BigDecimal.ZERO)
        if (withdrawal > walletInfo.mistAddressBalanceOf(coinType)) return null

        val isGasPaidByCoins = isGasPaidByCoins(walletInfo)
        val gasCoins = walletInfo.suiCoinsLargestFirst().take(MAX_GAS_PAYMENT_OBJECTS)
        val gasSourceBalance = if (isGasPaidByCoins) {
            gasCoins.sumOf { it.mistBalance }
        } else {
            walletInfo.mistAddressBalanceOf(COIN_TYPE)
        }
        if (gasSourceBalance < gas.budget) return null

        val ptb = PtbBuilder()
        ptb.transferFunds(
            coins = tokenCoins,
            withdrawal = withdrawal,
            coinType = SuiTypeTag.parse(coinType),
            amount = amount,
            recipient = recipient,
        )

        return ptb.build(gasPayment = if (isGasPaidByCoins) gasCoins else emptyList(), gas = gas)
    }

    /** Gas comes from wherever most of SUI is, so the choice doesn't depend on the budget being estimated */
    private fun isGasPaidByCoins(walletInfo: SuiWalletInfo): Boolean {
        return walletInfo.suiCoinsBalanceMist() >= walletInfo.mistAddressBalanceOf(COIN_TYPE)
    }

    private fun BigDecimal.halfOf(): BigDecimal = divideToIntegralValue(BigDecimal(2))

    private fun SuiWalletInfo.suiCoinsLargestFirst(): List<SuiCoin> {
        return coinsOf(COIN_TYPE).sortedByDescending { it.mistBalance }
    }

    private fun SuiWalletInfo.suiCoinsBalanceMist(): BigDecimal = coinsOf(COIN_TYPE).sumOf { it.mistBalance }

    /** A transaction takes at most [MAX_GAS_PAYMENT_OBJECTS] gas coins, the largest ones are picked */
    private fun SuiWalletInfo.gasCoinsBalanceMist(): BigDecimal {
        return suiCoinsLargestFirst().take(MAX_GAS_PAYMENT_OBJECTS).sumOf { it.mistBalance }
    }

    private inner class PtbBuilder {

        private val inputs = mutableListOf<SuiCallArg>()
        private val commands = mutableListOf<SuiCommand>()

        fun input(arg: SuiCallArg): SuiArgument {
            inputs.add(arg)
            return SuiArgument.Input(inputs.lastIndex)
        }

        fun command(command: SuiCommand): Int {
            commands.add(command)
            return commands.lastIndex
        }

        fun redeem(amount: BigDecimal, coinType: SuiTypeTag): SuiArgument {
            val withdrawal = input(SuiCallArg.BalanceWithdrawal(amount = amount.toU64Bits(), coinType = coinType))
            val index = command(
                SuiCommand.MoveCall(
                    packageId = SUI_FRAMEWORK,
                    module = COIN_MODULE,
                    function = REDEEM_FUNDS_FUNCTION,
                    typeArguments = listOf(coinType),
                    arguments = listOf(withdrawal),
                ),
            )
            return SuiArgument.Result(index)
        }

        fun transfer(coin: SuiArgument, amount: BigDecimal, recipient: String) {
            val split = command(SuiCommand.SplitCoins(coin, listOf(input(SuiCallArg.PureU64(amount.toU64Bits())))))
            command(
                SuiCommand.TransferObjects(
                    objects = listOf(SuiArgument.NestedResult(split, resultIndex = 0)),
                    recipient = input(SuiCallArg.PureAddress(recipient)),
                ),
            )
        }

        /**
         * Sends [amount] out of [coins] topped up by [withdrawal] from the address balance. A redeemed coin has no
         * `drop`, so it is either sent whole (it holds exactly [amount] when there are no coins) or merged into a coin
         * object, which keeps the change.
         */
        fun transferFunds(
            coins: List<SuiCoin>,
            withdrawal: BigDecimal,
            coinType: SuiTypeTag,
            amount: BigDecimal,
            recipient: String,
        ) {
            val redeemed = if (withdrawal > BigDecimal.ZERO) redeem(withdrawal, coinType) else null
            val coinInputs = coins.map { input(SuiCallArg.OwnedObject(it.toObjectRef())) }

            if (coinInputs.isEmpty()) {
                val coin = requireNotNull(redeemed) { "Nothing to transfer" }
                command(SuiCommand.TransferObjects(listOf(coin), input(SuiCallArg.PureAddress(recipient))))
                return
            }

            val primary = coinInputs.first()
            val sources = coinInputs.drop(1) + listOfNotNull(redeemed)
            if (sources.isNotEmpty()) command(SuiCommand.MergeCoins(primary, sources))
            transfer(coin = primary, amount = amount, recipient = recipient)
        }

        fun build(gasPayment: List<SuiCoin>, gas: SuiGas) = SuiTransactionData(
            inputs = inputs,
            commands = commands,
            sender = walletAddress,
            gasPayment = gasPayment.map { it.toObjectRef() },
            gasPrice = gas.price,
            gasBudget = gas.budget.toU64Bits(),
            expiration = if (gasPayment.isEmpty()) {
                val epoch = requireNotNull(gas.epoch) { "Epoch is required to pay gas from the address balance" }
                // Two epochs keep the transaction valid if the epoch changes while the card is being tapped
                SuiTransactionExpiration.ValidDuring(
                    minEpoch = epoch,
                    maxEpoch = epoch + 1,
                    chainIdentifier = chainIdentifier,
                    nonce = nonceProvider(),
                )
            } else {
                SuiTransactionExpiration.None
            },
        )
    }

    private fun SuiCoin.toObjectRef() = SuiObjectRef(objectId = objectId, version = version, digest = digest)

    /**
     * [budget] is in MIST; [epoch] bounds the validity of a transaction whose gas is paid by the address balance and
     * is not needed otherwise
     */
    data class SuiGas(val budget: BigDecimal, val price: Long, val epoch: Long?)

    private companion object {
        const val SUI_FRAMEWORK = "0x2"
        const val COIN_MODULE = "coin"
        const val REDEEM_FUNDS_FUNCTION = "redeem_funds"
        const val MAX_GAS_PAYMENT_OBJECTS = 256
        const val MAX_NONCE = 1L shl Int.SIZE_BITS

        val SUI_TYPE = SuiTypeTag.parse(COIN_TYPE)
    }
}