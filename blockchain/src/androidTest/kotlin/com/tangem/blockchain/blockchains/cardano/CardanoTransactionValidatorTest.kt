package com.tangem.blockchain.blockchains.cardano

import com.google.common.truth.Truth
import com.tangem.blockchain.blockchains.cardano.network.common.models.CardanoUnspentOutput
import com.tangem.blockchain.blockchains.cardano.utils.CardanoTransactionValidatorTestFactory
import com.tangem.blockchain.common.BlockchainFeatureToggles
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.di.DepsContainer
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class CardanoTransactionValidatorTest {

    private val testModelFactory = CardanoTransactionValidatorTestFactory()

    init {
        System.loadLibrary("TrustWalletCore")
    }

    @Before
    fun setup() {
        DepsContainer.onInit(
            config = BlockchainSdkConfig(),
            featureToggles = BlockchainFeatureToggles(
                isYieldSupplyEnabled = false,
            ),
        )
    }

    // region Test withdraw coins 1
    /**
     * User has 5.66 ADA and does not have tokens. Try to withdraw 1 ADA.
     * Result: success
     */
    @Test
    fun test_withdraw_coins_1_1() = runTest {
        val validator = createValidator(model = testModelFactory.create_5_66_ADA())

        val result = validator.validate(
            transactionData = testModelFactory.createCoinTransaction(value = BigDecimal.ONE),
        )

        Truth.assertThat(result).isEqualTo(Result.success(Unit))
    }

    /**
     * User has 5.66 ADA and 0 WMT. Try to withdraw 1 ADA.
     * Result: success
     */
    @Test
    fun test_withdraw_coins_1_2() = runTest {
        val validator = createValidator(model = testModelFactory.create_5_66_ADA_and_WMT(wmtValue = BigDecimal.ZERO))

        val result = validator.validate(
            transactionData = testModelFactory.createCoinTransaction(value = BigDecimal.ONE),
        )

        Truth.assertThat(result).isEqualTo(Result.success(Unit))
    }

    /**
     * User has 5.66 ADA and 10 WMT. Try to withdraw 1 ADA.
     * Result: success
     */
    @Test
    fun test_withdraw_coins_1_3() = runTest {
        val validator = createValidator(model = testModelFactory.create_5_66_ADA_and_WMT())

        val result = validator.validate(
            transactionData = testModelFactory.createCoinTransaction(value = BigDecimal.ONE),
        )

        Truth.assertThat(result).isEqualTo(Result.success(Unit))
    }

    /**
     * User has 5.66 ADA and 10 WMT + 10 AGIX. Try to withdraw 1 ADA.
     * Result: success
     */
    @Test
    fun test_withdraw_coins_1_4() = runTest {
        val validator = createValidator(model = testModelFactory.create_5_66_ADA_and_WMT_and_AGIX())

        val result = validator.validate(
            transactionData = testModelFactory.createCoinTransaction(value = BigDecimal.ONE),
        )

        Truth.assertThat(result).isEqualTo(Result.success(Unit))
    }
    // endregion

    // region Test withdraw coins 2
    /**
     * User has 5.66 ADA and does not have tokens. Try to withdraw 5 ADA.
     * Result: failure. Balance is less than 1 ADA after sending.
     */
    @Test
    fun test_withdraw_coins_2_1() = runTest {
        val validator = createValidator(model = testModelFactory.create_5_66_ADA())

        val result = validator.validate(
            transactionData = testModelFactory.createCoinTransaction(value = BigDecimal(5)),
        )

        result
            .onSuccess { error("Method must throws an exception") }
            .onFailure {
                Truth.assertThat(it)
                    .isInstanceOf(BlockchainSdkError.Cardano.InsufficientRemainingBalance::class.java)
            }
    }

    /**
     * User has 5.66 ADA and 0 WMT. Try to withdraw 5 ADA.
     * Result: failure. Balance is less than 1 ADA after sending.
     */
    @Test
    fun test_withdraw_coins_2_2() = runTest {
        val validator = createValidator(model = testModelFactory.create_5_66_ADA_and_WMT(wmtValue = BigDecimal.ZERO))

        val result = validator.validate(
            transactionData = testModelFactory.createCoinTransaction(value = BigDecimal(5)),
        )

        result
            .onSuccess { error("Method must throws an exception") }
            .onFailure {
                Truth.assertThat(it)
                    .isInstanceOf(BlockchainSdkError.Cardano.InsufficientRemainingBalance::class.java)
            }
    }

    /**
     * User has 5.66 ADA and 10 WMT. Try to withdraw 5 ADA.
     * Result: failure. User has another NOT ZERO tokens.
     */
    @Test
    fun test_withdraw_coins_2_3() = runTest {
        val validator = createValidator(model = testModelFactory.create_5_66_ADA_and_WMT(wmtValue = BigDecimal(10)))

        val result = validator.validate(
            transactionData = testModelFactory.createCoinTransaction(value = BigDecimal(5)),
        )

        result
            .onSuccess { error("Method must throws an exception") }
            .onFailure {
                Truth.assertThat(it)
                    .isInstanceOf(BlockchainSdkError.Cardano.InsufficientRemainingBalanceToWithdrawTokens::class.java)
            }
    }

    /**
     * User has 5.66 ADA and 10 WMT + 10 AGIX. Try to withdraw 5 ADA.
     * Result: failure. User has another NOT ZERO tokens.
     */
    @Test
    fun test_withdraw_coins_2_4() = runTest {
        val validator = createValidator(model = testModelFactory.create_2_ADA_and_WMT_and_AGIX())

        val result = validator.validate(
            transactionData = testModelFactory.createCoinTransaction(value = BigDecimal(5)),
        )

        result
            .onSuccess { error("Method must throws an exception") }
            .onFailure {
                Truth.assertThat(it)
                    .isInstanceOf(BlockchainSdkError.Cardano.InsufficientRemainingBalanceToWithdrawTokens::class.java)
            }
    }
    // endregion

    // region Test withdraw coins 3
    /**
     * User has 5.66 ADA and does not have tokens. Try to withdraw 0.99 ADA.
     * Result: failure. Sending amount is less than minimum.
     */
    @Test
    fun test_withdraw_coins_3_1() = runTest {
        val validator = createValidator(model = testModelFactory.create_5_66_ADA())

        val result = validator.validate(
            transactionData = testModelFactory.createCoinTransaction(value = BigDecimal(0.99)),
        )

        result
            .onSuccess { error("Method must throws an exception") }
            .onFailure {
                Truth.assertThat(it)
                    .isInstanceOf(BlockchainSdkError.Cardano.InsufficientSendingAdaAmount::class.java)
            }
    }

    /**
     * User has 5.66 ADA and 0 WMT. Try to withdraw 0.99 ADA.
     * Result: failure. Sending amount is less than minimum.
     */
    @Test
    fun test_withdraw_coins_3_2() = runTest {
        val validator = createValidator(model = testModelFactory.create_5_66_ADA_and_WMT(wmtValue = BigDecimal.ZERO))

        val result = validator.validate(
            transactionData = testModelFactory.createCoinTransaction(value = BigDecimal(0.99)),
        )

        result
            .onSuccess { error("Method must throws an exception") }
            .onFailure {
                Truth.assertThat(it)
                    .isInstanceOf(BlockchainSdkError.Cardano.InsufficientSendingAdaAmount::class.java)
            }
    }

    /**
     * User has 5.66 ADA and 10 WMT. Try to withdraw 0.99 ADA.
     * Result: failure. Sending amount is less than minimum.
     */
    @Test
    fun test_withdraw_coins_3_3() = runTest {
        val validator = createValidator(model = testModelFactory.create_5_66_ADA_and_WMT(wmtValue = BigDecimal(10)))

        val result = validator.validate(
            transactionData = testModelFactory.createCoinTransaction(value = BigDecimal(0.99)),
        )

        result
            .onSuccess { error("Method must throws an exception") }
            .onFailure {
                Truth.assertThat(it)
                    .isInstanceOf(BlockchainSdkError.Cardano.InsufficientSendingAdaAmount::class.java)
            }
    }

    /**
     * User has 5.66 ADA and 10 WMT + 10 AGIX. Try to withdraw 0.99 ADA.
     * Result: failure. Sending amount is less than minimum.
     */
    @Test
    fun test_withdraw_coins_3_4() = runTest {
        val validator = createValidator(model = testModelFactory.create_5_66_ADA_and_WMT_and_AGIX())

        val result = validator.validate(
            transactionData = testModelFactory.createCoinTransaction(value = BigDecimal(0.99)),
        )

        result
            .onSuccess { error("Method must throws an exception") }
            .onFailure {
                Truth.assertThat(it)
                    .isInstanceOf(BlockchainSdkError.Cardano.InsufficientSendingAdaAmount::class.java)
            }
    }
    // endregion

    // region Test withdraw coins 4
    /**
     * User has 5.66 ADA and does not have tokens. Try to withdraw all ADA.
     * Result: success
     */
    @Test
    fun test_withdraw_coins_4_1() = runTest {
        val validator = createValidator(model = testModelFactory.create_5_66_ADA())

        val result = validator.validate(
            transactionData = testModelFactory.createCoinTransaction(value = BigDecimal(5.66)),
        )

        Truth.assertThat(result).isEqualTo(Result.success(Unit))
    }

    /**
     * User has 5.66 ADA and 0 WMT. Try to withdraw all ADA.
     * Result: success
     */
    @Test
    fun test_withdraw_coins_4_2() = runTest {
        val validator = createValidator(model = testModelFactory.create_5_66_ADA_and_WMT(wmtValue = BigDecimal.ZERO))

        val result = validator.validate(
            transactionData = testModelFactory.createCoinTransaction(value = BigDecimal(5.66)),
        )

        Truth.assertThat(result).isEqualTo(Result.success(Unit))
    }

    /**
     * User has 5.66 ADA and 10 WMT. Try to withdraw all ADA.
     * Result: failure. Impossible to withdraw all ADA if user has another NOT ZERO tokens.
     */
    @Test
    fun test_withdraw_coins_4_3() = runTest {
        val validator = createValidator(model = testModelFactory.create_5_66_ADA_and_WMT(wmtValue = BigDecimal(10)))

        val result = validator.validate(
            transactionData = testModelFactory.createCoinTransaction(value = BigDecimal(5.66)),
        )

        result
            .onSuccess { error("Method must throws an exception") }
            .onFailure {
                Truth.assertThat(it)
                    .isInstanceOf(BlockchainSdkError.Cardano.InsufficientRemainingBalanceToWithdrawTokens::class.java)
            }
    }

    /**
     * User has 5.66 ADA and 10 WMT + 10 AGIX. Try to withdraw all ADA.
     * Result: failure. Impossible to withdraw all ADA if user has another NOT ZERO tokens.
     */
    @Test
    fun test_withdraw_coins_4_4() = runTest {
        val validator = createValidator(model = testModelFactory.create_5_66_ADA_and_WMT_and_AGIX())

        val result = validator.validate(
            transactionData = testModelFactory.createCoinTransaction(value = BigDecimal(5.66)),
        )

        result
            .onSuccess { error("Method must throws an exception") }
            .onFailure {
                Truth.assertThat(it)
                    .isInstanceOf(BlockchainSdkError.Cardano.InsufficientRemainingBalanceToWithdrawTokens::class.java)
            }
    }
    // endregion

    // region Test withdraw coins 5
    /**
     * User has 5.66 ADA. Try to withdraw 4.5 ADA.
     * Result: failure. Balance is less than 1 ADA after sending.
     */
    @Test
    fun test_withdraw_coins_5_1() = runTest {
        val validator = createValidator(model = testModelFactory.create_5_66_ADA())

        val result = validator.validate(
            transactionData = testModelFactory.createCoinTransaction(value = BigDecimal(4.5)),
        )

        result
            .onSuccess { error("Method must throws an exception") }
            .onFailure {
                Truth.assertThat(it)
                    .isInstanceOf(BlockchainSdkError.Cardano.InsufficientRemainingBalance::class.java)
            }
    }

    /**
     * User has 5.66 ADA and 0 WMT. Try to withdraw 4.5 ADA.
     * Result: failure. Balance is less than 1 ADA after sending.
     */
    @Test
    fun test_withdraw_coins_5_2() = runTest {
        val validator = createValidator(model = testModelFactory.create_5_66_ADA_and_WMT(wmtValue = BigDecimal.ZERO))

        val result = validator.validate(
            transactionData = testModelFactory.createCoinTransaction(value = BigDecimal(4.5)),
        )

        result
            .onSuccess { error("Method must throws an exception") }
            .onFailure {
                Truth.assertThat(it)
                    .isInstanceOf(BlockchainSdkError.Cardano.InsufficientRemainingBalance::class.java)
            }
    }

    /**
     * User has 5.66 ADA and 10 WMT. Try to withdraw 4.5 ADA.
     * Result: failure. Impossible to withdraw all ADA if user has another NOT ZERO tokens.
     */
    @Test
    fun test_withdraw_coins_5_3() = runTest {
        val validator = createValidator(model = testModelFactory.create_5_66_ADA_and_WMT(BigDecimal(10)))

        val result = validator.validate(
            transactionData = testModelFactory.createCoinTransaction(value = BigDecimal(4.5)),
        )

        result
            .onSuccess { error("Method must throws an exception") }
            .onFailure {
                Truth.assertThat(it)
                    .isInstanceOf(BlockchainSdkError.Cardano.InsufficientRemainingBalanceToWithdrawTokens::class.java)
            }
    }

    /**
     * User has 5.66 ADA and 10 WMT + 10 AGIX. Try to withdraw 4.5 ADA.
     * Result: failure. Impossible to withdraw all ADA if user has another NOT ZERO tokens.
     */
    @Test
    fun test_withdraw_coins_5_4() = runTest {
        val validator = createValidator(model = testModelFactory.create_5_66_ADA_and_WMT_and_AGIX())

        val result = validator.validate(
            transactionData = testModelFactory.createCoinTransaction(value = BigDecimal(4.5)),
        )

        result
            .onSuccess { error("Method must throws an exception") }
            .onFailure {
                Truth.assertThat(it)
                    .isInstanceOf(BlockchainSdkError.Cardano.InsufficientRemainingBalanceToWithdrawTokens::class.java)
            }
    }
    // endregion

    // region Test withdraw tokens 1
    /**
     * User has 5.66 ADA and 0 WMT. Try to withdraw 5 WMT.
     * Result: failure. Insufficient ADA amount.
     */
    @Test
    fun test_withdraw_tokens_1_1() = runTest {
        val validator = createValidator(model = testModelFactory.create_5_66_ADA_and_WMT(BigDecimal.ZERO))

        val result = validator.validate(
            transactionData = testModelFactory.createWMTTransaction(value = BigDecimal(5)),
        )

        result
            .onSuccess { error("Method must throws an exception") }
            .onFailure {
                Truth.assertThat(it)
                    .isInstanceOf(BlockchainSdkError.Cardano.InsufficientMinAdaBalanceToSendToken::class.java)
            }
    }

    /**
     * User has 5.66 ADA and 10 WMT. Try to withdraw 5 WMT.
     * Result: success
     */
    @Test
    fun test_withdraw_tokens_1_2() = runTest {
        val validator = createValidator(model = testModelFactory.create_5_66_ADA_and_WMT())

        val result = validator.validate(
            transactionData = testModelFactory.createWMTTransaction(value = BigDecimal(5)),
        )

        Truth.assertThat(result).isEqualTo(Result.success(Unit))
    }

    /**
     * User has 5.66 ADA and 10 WMT + 10 AGIX. Try to withdraw 5 WMT.
     * Result: success
     */
    @Test
    fun test_withdraw_tokens_1_3() = runTest {
        val validator = createValidator(model = testModelFactory.create_5_66_ADA_and_WMT_and_AGIX())

        val result = validator.validate(
            transactionData = testModelFactory.createWMTTransaction(value = BigDecimal(5)),
        )

        Truth.assertThat(result).isEqualTo(Result.success(Unit))
    }
    // endregion

    /**
     * User has 2 ADA and 10 WMT. Try to withdraw 10 WMT.
     * Result: success. Due to balance is less than 1 ADA after sending, we will withdraw all ADA.
     */
    @Test
    fun test_withdraw_tokens_2() = runTest {
        val validator = createValidator(model = testModelFactory.create_2_ADA_and_WMT())

        val result = validator.validate(
            transactionData = testModelFactory.createWMTTransaction(value = BigDecimal(10)),
        )

        Truth.assertThat(result).isEqualTo(Result.success(Unit))
    }

    // region Test withdraw tokens 3
    /**
     * User has 2 ADA and 10 WMT. Try to withdraw 5 WMT.
     * Result: failure. Impossible to withdraw part of WMT if user has another NOT ZERO tokens.
     */
    @Test
    fun test_withdraw_tokens_3_1() = runTest {
        val validator = createValidator(model = testModelFactory.create_2_ADA_and_WMT())

        val result = validator.validate(
            transactionData = testModelFactory.createWMTTransaction(value = BigDecimal(5)),
        )

        result
            .onSuccess { error("Method must throws an exception") }
            .onFailure {
                Truth.assertThat(it)
                    .isInstanceOf(BlockchainSdkError.Cardano.InsufficientRemainingBalanceToWithdrawTokens::class.java)
            }
    }

    /**
     * User has 2 ADA and 10 WMT + 0 WMT. Try to withdraw 5 WMT.
     * Result: failure. Impossible to withdraw part of WMT if user has another NOT ZERO tokens.
     */
    @Test
    fun test_withdraw_tokens_3_2() = runTest {
        val validator = createValidator(
            model = testModelFactory.create_2_ADA_and_WMT_and_AGIX(agixValue = BigDecimal.ZERO),
        )

        val result = validator.validate(
            transactionData = testModelFactory.createWMTTransaction(value = BigDecimal(5)),
        )

        result
            .onSuccess { error("Method must throws an exception") }
            .onFailure {
                Truth.assertThat(it)
                    .isInstanceOf(BlockchainSdkError.Cardano.InsufficientRemainingBalanceToWithdrawTokens::class.java)
            }
    }

    /**
     * User has 2 ADA and 10 WMT + 10 WMT. Try to withdraw 5 WMT.
     * Result: failure. Impossible to withdraw part of WMT if user has another NOT ZERO tokens.
     */
    @Test
    fun test_withdraw_tokens_3_3() = runTest {
        val validator = createValidator(model = testModelFactory.create_2_ADA_and_WMT_and_AGIX())

        val result = validator.validate(
            transactionData = testModelFactory.createWMTTransaction(value = BigDecimal(5)),
        )

        result
            .onSuccess { error("Method must throws an exception") }
            .onFailure {
                Truth.assertThat(it)
                    .isInstanceOf(BlockchainSdkError.Cardano.InsufficientRemainingBalanceToWithdrawTokens::class.java)
            }
    }
    // endregion

    /**
     * User has 2 ADA and 10 WMT + 10 AGIX. Try to withdraw 10 WMT.
     * Result: failure. Impossible to withdraw all ADA if user has another NOT ZERO tokens.
     */
    @Test
    fun test_withdraw_tokens_4() = runTest {
        val validator = createValidator(model = testModelFactory.create_2_ADA_and_WMT_and_AGIX())

        val result = validator.validate(
            transactionData = testModelFactory.createWMTTransaction(value = BigDecimal(10)),
        )

        result
            .onSuccess { error("Method must throws an exception") }
            .onFailure {
                Truth.assertThat(it)
                    .isInstanceOf(BlockchainSdkError.Cardano.InsufficientRemainingBalanceToWithdrawTokens::class.java)
            }
    }

    /**
     * User has 1 ADA and 10 WMT. Try to withdraw 5 WMT.
     * Result: failure. Balance is low to cover fee and min-ada-value.
     */
    @Test
    fun test_withdraw_tokens_5() = runTest {
        val validator = createValidator(model = testModelFactory.create_2_ADA_and_WMT())

        val result = validator.validate(
            transactionData = testModelFactory.createWMTTransaction(value = BigDecimal(5)),
        )

        result
            .onSuccess { error("Method must throws an exception") }
            .onFailure {
                Truth.assertThat(it)
                    .isInstanceOf(BlockchainSdkError.Cardano.InsufficientRemainingBalanceToWithdrawTokens::class.java)
            }
    }

    private fun createValidator(model: Model): CardanoTransactionBuilder {
        return CardanoTransactionBuilder(wallet = model.wallet).apply {
            update(model.utxos)
        }
    }

    internal data class Model(
        val wallet: Wallet,
        val utxos: List<CardanoUnspentOutput>,
    )
}