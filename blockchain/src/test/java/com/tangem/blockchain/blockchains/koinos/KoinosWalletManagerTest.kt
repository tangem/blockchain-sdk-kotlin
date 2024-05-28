package com.tangem.blockchain.blockchains.koinos

import com.google.common.truth.Truth
import com.tangem.blockchain.blockchains.koinos.network.KoinosNetworkService
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.address.Address
import com.tangem.blockchain.common.transaction.Fee.Common
import io.mockk.mockk
import org.junit.Test

internal class KoinosWalletManagerTest {

    private val walletManager = KoinosWalletManager(
        wallet = com.tangem.blockchain.common.Wallet(
            blockchain = Blockchain.Koinos,
            addresses = setOf(Address("1AYz8RCnoafLnifMjJbgNb2aeW5CbZj8Tp")),
            publicKey = mockk(),
            tokens = emptySet(),
        ),
        transactionBuilder = KoinosTransactionBuilder(isTestnet = false),
        transactionHistoryProvider = mockk(),
        networkService = KoinosNetworkService(listOf(mockk())),
    )

    @Test
    fun transactionValidationTest_smoke() {
        walletManager.wallet.apply {
            setBalance(100f)
            setMana(100f)
        }

        val errors = walletManager.validate(
            transactionData(10f, 0.3f),
        )

        Truth.assertThat(errors.isSuccess)
            .isTrue()
    }

    @Test
    fun transactionValidationTest_not_enough_mana() {
        walletManager.wallet.apply {
            setBalance(100f)
            setMana(0.2f)
        }

        val errors = walletManager.validate(
            transactionData(10f, 0.3f),
        )

        Truth.assertThat(errors.isFailure).isTrue()
        Truth.assertThat(errors.exceptionOrNull()).isInstanceOf(BlockchainSdkError.Koinos.InsufficientMana::class.java)
    }

    @Test
    fun transactionValidationTest_amount_exceeds_mana_balance() {
        walletManager.wallet.apply {
            setBalance(100f)
            setMana(50f)
        }

        val errors = walletManager.validate(
            transactionData(51f, 0.3f),
        )

        Truth.assertThat(errors.isFailure).isTrue()
        Truth.assertThat(errors.exceptionOrNull())
            .isInstanceOf(BlockchainSdkError.Koinos.ManaFeeExceedsBalance::class.java)
    }

    @Test
    fun transactionValidationTest_coin_balance_does_not_cover_fee() {
        walletManager.wallet.apply {
            setBalance(0.2f)
            setMana(0.2f)
        }

        val errors = walletManager.validate(
            transactionData(0.2f, 0.3f),
        )

        Truth.assertThat(errors.isFailure).isTrue()
        Truth.assertThat(errors.exceptionOrNull())
            .isInstanceOf(BlockchainSdkError.Koinos.InsufficientBalance::class.java)
    }

    // ====Legacy

    @Test
    fun transactionValidationLegacyTest_smoke() {
        walletManager.wallet.apply {
            setBalance(100f)
            setMana(100f)
        }

        val errors = walletManager.validateTransaction(
            coinAmount(10f),
            manaAmount(0.3f),
        )

        Truth.assertThat(errors).isEmpty()
    }

    @Test
    fun transactionValidationLegacyTest_not_enough_mana() {
        walletManager.wallet.apply {
            setBalance(100f)
            setMana(0.2f)
        }

        val errors = walletManager.validateTransaction(
            coinAmount(10f),
            manaAmount(0.3f),
        )

        Truth.assertThat(errors).contains(
            TransactionError.FeeExceedsBalance,
        )
    }

    @Test
    fun transactionValidationLegacyTest_amount_exceeds_mana_balance() {
        walletManager.wallet.apply {
            setBalance(100f)
            setMana(50f)
        }

        val errors = walletManager.validateTransaction(
            coinAmount(51f),
            manaAmount(0.3f),
        )

        Truth.assertThat(errors).contains(
            TransactionError.AmountExceedsBalance,
        )
    }

    @Test
    fun transactionValidationLegacyTest_all_koinos_errors() {
        walletManager.wallet.apply {
            setBalance(100f)
            setMana(0.2f)
        }

        val errors = walletManager.validateTransaction(
            coinAmount(51f),
            manaAmount(0.3f),
        )

        Truth.assertThat(errors).containsExactly(
            TransactionError.AmountExceedsBalance,
            TransactionError.FeeExceedsBalance,
        )
    }

    private fun transactionData(amount: Float, fee: Float): TransactionData {
        return TransactionData(
            amount = coinAmount(amount),
            fee = Common(manaAmount(fee)),
            sourceAddress = "",
            destinationAddress = "",
        )
    }

    private fun coinAmount(amount: Float): Amount {
        return Amount(
            value = amount.toBigDecimal().setScale(Blockchain.Koinos.decimals()),
            blockchain = Blockchain.Koinos,
            type = AmountType.Coin,
        )
    }

    private fun manaAmount(amount: Float, maxAmount: Float? = null): Amount {
        return Amount(
            value = amount.toBigDecimal().setScale(Blockchain.Koinos.decimals()),
            maxValue = maxAmount?.toBigDecimal()?.setScale(Blockchain.Koinos.decimals()),
            blockchain = Blockchain.Koinos,
            type = AmountType.FeeResource(),
        )
    }

    private fun Wallet.setBalance(balance: Float) {
        setAmount(
            value = balance.toBigDecimal().setScale(Blockchain.Koinos.decimals()),
            amountType = AmountType.Coin,
        )
    }

    private fun Wallet.setMana(mana: Float) {
        setAmount(
            value = mana.toBigDecimal().setScale(Blockchain.Koinos.decimals()),
            maxValue = amounts[AmountType.Coin]!!.value,
            amountType = AmountType.FeeResource(),
        )
    }
}