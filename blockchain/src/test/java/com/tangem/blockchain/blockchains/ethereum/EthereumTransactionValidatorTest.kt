package com.tangem.blockchain.blockchains.ethereum

import com.google.common.truth.Truth
import com.tangem.blockchain.blockchains.ethereum.tokenmethods.TransferERC20TokenCallData
import com.tangem.blockchain.blockchains.ethereum.txbuilder.EthereumTransactionValidator
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.transaction.Fee
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.math.BigDecimal
import java.math.BigInteger

class EthereumTransactionValidatorTest {

    private val ethereumTransactionValidator = EthereumTransactionValidator

    private val tokenAddress = "0xc63763572D45171e4C25cA0818b44E5Dd7F5c15B"

    private val coinAmount = Amount(
        value = BigDecimal.ONE,
        currencySymbol = "ETH",
        blockchain = Blockchain.Ethereum,
        type = AmountType.Coin,
    )

    val tokenAmount = coinAmount.copy(
        type = AmountType.Token(
            token = Token(
                contractAddress = tokenAddress,
                decimals = 18,
                symbol = "TKN",
                name = "Token Name",
                id = "token-id",
            ),
        ),
    )

    val yieldAmount = coinAmount.copy(
        type = AmountType.TokenYieldSupply(
            token = Token(
                contractAddress = tokenAddress,
                decimals = 18,
                symbol = "yTKN",
                name = "Yield Token Name",
                id = "yield-token-id",
            ),
            isActive = true,
            isInitialized = true,
            isAllowedToSpend = true,
            effectiveProtocolBalance = BigDecimal.ZERO,
        ),
    )

    private val transactionData = TransactionData.Uncompiled(
        amount = coinAmount,
        fee = Fee.Ethereum.EIP1559(
            gasLimit = 21000.toBigInteger(),
            amount = coinAmount,
            maxFeePerGas = BigInteger.ONE,
            priorityFee = BigInteger.ONE,
        ),
        sourceAddress = "0xc63763572D45171e4C25cA0818b44E5Dd7F5c15B",
        destinationAddress = "0xc63763572D45171e4C25cA0818b44E5Dd7F5c15B",
    )

    private val extras = EthereumTransactionExtras(
        callData = TransferERC20TokenCallData(
            destination = "0xc63763572D45171e4C25cA0818b44E5Dd7F5c15B",
            amount = tokenAmount,
        ),
    )

    @Test
    fun `Correct Compiled tx data`() = runTest {
        // Since compiled transactions are always valid, we can just assert that the validation passes.
        val result = ethereumTransactionValidator.validate(
            transactionData = TransactionData.Compiled(
                value = TransactionData.Compiled.Data.RawString(""),
            ),
        )

        Truth.assertThat(result.isSuccess).isTrue()
    }

    // region Uncompiled coin
    @Test
    fun `Correct FOR coin tx data`() = runTest {
        val result = ethereumTransactionValidator.validate(transactionData)

        Truth.assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `Empty destination address FOR coin tx data`() = runTest {
        val result = ethereumTransactionValidator.validate(
            transactionData.copy(
                destinationAddress = "",
            ),
        )

        Truth.assertThat(result.isSuccess).isFalse()
    }

    @Test
    fun `Short destination address FOR coin tx data`() = runTest {
        val result = ethereumTransactionValidator.validate(
            transactionData.copy(
                destinationAddress = "0x",
            ),
        )

        Truth.assertThat(result.isSuccess).isFalse()
    }

    @Test
    fun `Invalid destination address FOR coin tx data`() = runTest {
        val result = ethereumTransactionValidator.validate(
            transactionData.copy(
                destinationAddress = "0xc63763572D45171e4C25cA0818bf4E5Dd7F5c15B",
            ),
        )

        Truth.assertThat(result.isSuccess).isFalse()
    }

    @Test
    fun `Empty fee FOR coin tx data`() = runTest {
        val result = ethereumTransactionValidator.validate(
            transactionData.copy(
                fee = null,
            ),
        )

        Truth.assertThat(result.isSuccess).isFalse()
    }
    // endregion

    // region Uncompiled token
    @Test
    fun `Correct FOR token tx data`() = runTest {
        val result = ethereumTransactionValidator.validate(
            transactionData.copy(
                destinationAddress = tokenAddress,
                amount = tokenAmount,
                extras = extras,
            ),
        )

        Truth.assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `Empty destination address FOR token tx data`() = runTest {
        val result = ethereumTransactionValidator.validate(
            transactionData.copy(
                destinationAddress = "",
                amount = tokenAmount,
                extras = extras,
            ),
        )

        Truth.assertThat(result.isSuccess).isFalse()
    }

    @Test
    fun `Short destination address FOR token tx data`() = runTest {
        val result = ethereumTransactionValidator.validate(
            transactionData.copy(
                destinationAddress = "0x",
                amount = tokenAmount,
                extras = extras,
            ),
        )

        Truth.assertThat(result.isSuccess).isFalse()
    }

    @Test
    fun `Invalid destination address FOR token tx data`() = runTest {
        val result = ethereumTransactionValidator.validate(
            transactionData.copy(
                destinationAddress = "0xc63763572D45171e4C25cA0818bf4E5Dd7F5c15B",
                amount = tokenAmount,
                extras = extras,
            ),
        )

        Truth.assertThat(result.isSuccess).isFalse()
    }

    @Test
    fun `Invalid extras FOR token tx data`() = runTest {
        val result = ethereumTransactionValidator.validate(
            transactionData.copy(
                amount = tokenAmount,
                extras = null,
            ),
        )

        Truth.assertThat(result.isSuccess).isFalse()
    }

    @Test
    fun `Empty fee FOR token tx data`() = runTest {
        val result = ethereumTransactionValidator.validate(
            transactionData.copy(
                extras = extras,
                amount = tokenAmount,
                fee = null,
            ),
        )

        Truth.assertThat(result.isSuccess).isFalse()
    }
    // endregion

    // region Uncompiled yield token
    @Test
    fun `Correct FOR yield token tx data`() = runTest {
        val result = ethereumTransactionValidator.validate(
            transactionData.copy(
                destinationAddress = tokenAddress,
                amount = yieldAmount,
                extras = extras,
            ),
        )

        Truth.assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `Empty destination address FOR yield token tx data`() = runTest {
        val result = ethereumTransactionValidator.validate(
            transactionData.copy(
                destinationAddress = "",
                amount = yieldAmount,
                extras = extras,
            ),
        )

        Truth.assertThat(result.isSuccess).isFalse()
    }

    @Test
    fun `Short destination address FOR yield token tx data`() = runTest {
        val result = ethereumTransactionValidator.validate(
            transactionData.copy(
                destinationAddress = "0x",
                amount = yieldAmount,
                extras = extras,
            ),
        )

        Truth.assertThat(result.isSuccess).isFalse()
    }

    @Test
    fun `Invalid destination address FOR yield token tx data`() = runTest {
        val result = ethereumTransactionValidator.validate(
            transactionData.copy(
                destinationAddress = "0xc63763572D45171e4C25cA0818bf4E5Dd7F5c15B",
                amount = yieldAmount,
                extras = extras,
            ),
        )

        Truth.assertThat(result.isSuccess).isFalse()
    }

    @Test
    fun `Invalid extras FOR yield token tx data`() = runTest {
        val result = ethereumTransactionValidator.validate(
            transactionData.copy(
                amount = yieldAmount,
                extras = null,
            ),
        )

        Truth.assertThat(result.isSuccess).isFalse()
    }

    @Test
    fun `Empty fee FOR yield token tx data`() = runTest {
        val result = ethereumTransactionValidator.validate(
            transactionData.copy(
                extras = extras,
                amount = yieldAmount,
                fee = null,
            ),
        )

        Truth.assertThat(result.isSuccess).isFalse()
    }
    // endregion
}