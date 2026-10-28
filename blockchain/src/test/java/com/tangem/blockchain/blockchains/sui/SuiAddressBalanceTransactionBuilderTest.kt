package com.tangem.blockchain.blockchains.sui

import com.google.common.truth.Truth
import com.tangem.blockchain.blockchains.sui.bcs.SuiArgument
import com.tangem.blockchain.blockchains.sui.bcs.SuiCallArg
import com.tangem.blockchain.blockchains.sui.bcs.SuiCommand
import com.tangem.blockchain.blockchains.sui.bcs.SuiObjectRef
import com.tangem.blockchain.blockchains.sui.bcs.SuiTransactionExpiration
import com.tangem.blockchain.blockchains.sui.bcs.SuiTypeTag
import com.tangem.blockchain.blockchains.sui.model.SuiCoin
import com.tangem.blockchain.blockchains.sui.model.SuiWalletInfo
import com.tangem.blockchain.blockchains.sui.network.SuiConstants
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.extensions.Result
import com.tangem.common.extensions.toHexString
import org.junit.Test
import java.math.BigDecimal

class SuiAddressBalanceTransactionBuilderTest {

    private val builder = SuiAddressBalanceTransactionBuilder(
        walletAddress = SENDER,
        chainIdentifier = SuiConstants.chainIdentifierOf(Blockchain.Sui),
        nonceProvider = { NONCE },
    )

    private val gas = SuiAddressBalanceTransactionBuilder.SuiGas(budget = mist(100), price = GAS_PRICE, epoch = EPOCH)

    private val validDuring = SuiTransactionExpiration.ValidDuring(
        minEpoch = EPOCH,
        maxEpoch = EPOCH + 1,
        chainIdentifier = SuiConstants.chainIdentifierOf(Blockchain.Sui),
        nonce = NONCE,
    )

    @Test
    fun `GIVEN no address balance WHEN coins don't cover the transfer THEN address balance is not needed`() {
        val info = walletInfo(suiCoins = listOf(1_000L), suiAddressBalance = 0)

        Truth.assertThat(builder.isAddressBalanceNeededForCoin(info, mist(5_000), mist(100))).isFalse()
    }

    @Test
    fun `GIVEN address balance WHEN coins cover amount and gas THEN address balance is not needed`() {
        val info = walletInfo(suiCoins = listOf(10_000L), suiAddressBalance = 5_000)

        Truth.assertThat(builder.isAddressBalanceNeededForCoin(info, mist(5_000), mist(100))).isFalse()
    }

    @Test
    fun `GIVEN address balance WHEN coins don't cover amount and gas THEN address balance is needed`() {
        val info = walletInfo(suiCoins = listOf(5_000L), suiAddressBalance = 5_000)

        Truth.assertThat(builder.isAddressBalanceNeededForCoin(info, mist(5_000), mist(100))).isTrue()
    }

    @Test
    fun `GIVEN token coins covering amount and SUI in coins WHEN token transfer THEN address balance is not needed`() {
        val info = walletInfo(
            suiCoins = listOf(10_000L),
            suiAddressBalance = 0,
            tokenCoins = listOf(500L),
            tokenAddressBalance = 1_000,
        )

        Truth.assertThat(builder.isAddressBalanceNeededForToken(info, TOKEN_TYPE, mist(500))).isFalse()
    }

    @Test
    fun `GIVEN token coins short of amount WHEN token transfer THEN address balance is needed`() {
        val info = walletInfo(
            suiCoins = listOf(10_000L),
            suiAddressBalance = 0,
            tokenCoins = listOf(100L),
            tokenAddressBalance = 1_000,
        )

        Truth.assertThat(builder.isAddressBalanceNeededForToken(info, TOKEN_TYPE, mist(500))).isTrue()
    }

    @Test
    fun `GIVEN SUI mostly in address balance WHEN token transfer THEN address balance is needed for gas`() {
        val info = walletInfo(
            suiCoins = listOf(100L),
            suiAddressBalance = 10_000,
            tokenCoins = listOf(1_000L),
            tokenAddressBalance = 0,
        )

        Truth.assertThat(builder.isAddressBalanceNeededForToken(info, TOKEN_TYPE, mist(500))).isTrue()
    }

    @Test
    fun `GIVEN coins hold most SUI WHEN coin transfer THEN gas coin is topped up from address balance`() {
        // Arrange
        val info = walletInfo(suiCoins = listOf(3_000L, 2_000L), suiAddressBalance = 1_000)

        // Act
        val transaction = builder.buildCoinTransfer(
            walletInfo = info,
            amount = mist(5_500),
            recipient = RECIPIENT,
            gas = gas,
        )

        // Assert
        Truth.assertThat(transaction!!.inputs).containsExactly(
            SuiCallArg.BalanceWithdrawal(amount = 600, coinType = SuiTypeTag.parse(SuiConstants.COIN_TYPE)),
            SuiCallArg.PureU64(5_500),
            SuiCallArg.PureAddress(RECIPIENT),
        ).inOrder()
        Truth.assertThat(transaction.commands).containsExactly(
            redeem(SuiConstants.COIN_TYPE, input = 0),
            SuiCommand.MergeCoins(SuiArgument.GasCoin, listOf(SuiArgument.Result(0))),
            SuiCommand.SplitCoins(SuiArgument.GasCoin, listOf(SuiArgument.Input(1))),
            SuiCommand.TransferObjects(listOf(SuiArgument.NestedResult(2, 0)), SuiArgument.Input(2)),
        ).inOrder()
        Truth.assertThat(transaction.gasPayment).containsExactly(objectRef(0), objectRef(1)).inOrder()
        Truth.assertThat(transaction.expiration).isEqualTo(SuiTransactionExpiration.None)
    }

    @Test
    fun `GIVEN only address balance WHEN coin transfer THEN redeemed coin is sent whole and gas paid by address`() {
        // Arrange
        val info = walletInfo(suiCoins = emptyList(), suiAddressBalance = 10_000)

        // Act
        val transaction = builder.buildCoinTransfer(
            walletInfo = info,
            amount = mist(5_000),
            recipient = RECIPIENT,
            gas = gas,
        )

        // Assert
        Truth.assertThat(transaction!!.inputs).containsExactly(
            SuiCallArg.BalanceWithdrawal(amount = 5_000, coinType = SuiTypeTag.parse(SuiConstants.COIN_TYPE)),
            SuiCallArg.PureAddress(RECIPIENT),
        ).inOrder()
        Truth.assertThat(transaction.commands).containsExactly(
            redeem(SuiConstants.COIN_TYPE, input = 0),
            SuiCommand.TransferObjects(listOf(SuiArgument.Result(0)), SuiArgument.Input(1)),
        ).inOrder()
        Truth.assertThat(transaction.gasPayment).isEmpty()
        Truth.assertThat(transaction.expiration).isEqualTo(validDuring)
    }

    @Test
    fun `GIVEN dust coins and SUI in address balance WHEN coin transfer THEN coins are merged with withdrawal`() {
        // Arrange
        val info = walletInfo(suiCoins = listOf(40L), suiAddressBalance = 10_000)

        // Act
        val transaction = builder.buildCoinTransfer(
            walletInfo = info,
            amount = mist(5_000),
            recipient = RECIPIENT,
            gas = gas,
        )

        // Assert
        Truth.assertThat(transaction!!.inputs).containsExactly(
            SuiCallArg.BalanceWithdrawal(amount = 4_960, coinType = SuiTypeTag.parse(SuiConstants.COIN_TYPE)),
            SuiCallArg.OwnedObject(objectRef(0)),
            SuiCallArg.PureU64(5_000),
            SuiCallArg.PureAddress(RECIPIENT),
        ).inOrder()
        Truth.assertThat(transaction.commands).containsExactly(
            redeem(SuiConstants.COIN_TYPE, input = 0),
            SuiCommand.MergeCoins(SuiArgument.Input(1), listOf(SuiArgument.Result(0))),
            SuiCommand.SplitCoins(SuiArgument.Input(1), listOf(SuiArgument.Input(2))),
            SuiCommand.TransferObjects(listOf(SuiArgument.NestedResult(2, 0)), SuiArgument.Input(3)),
        ).inOrder()
        Truth.assertThat(transaction.gasPayment).isEmpty()
        Truth.assertThat(transaction.expiration).isEqualTo(validDuring)
    }

    @Test
    fun `GIVEN address balance short of amount and gas WHEN coin transfer THEN nothing is built`() {
        val info = walletInfo(suiCoins = emptyList(), suiAddressBalance = 5_000)

        val transaction = builder.buildCoinTransfer(
            walletInfo = info,
            amount = mist(5_000),
            recipient = RECIPIENT,
            gas = gas,
        )

        Truth.assertThat(transaction).isNull()
    }

    @Test
    fun `GIVEN token coins short of amount WHEN token transfer THEN coins are topped up and gas paid by coins`() {
        // Arrange
        val info = walletInfo(
            suiCoins = listOf(10_000L),
            suiAddressBalance = 0,
            tokenCoins = listOf(100L),
            tokenAddressBalance = 1_000,
        )

        // Act
        val transaction = builder.buildTokenTransfer(
            walletInfo = info,
            coinType = TOKEN_TYPE,
            amount = mist(500),
            recipient = RECIPIENT,
            gas = gas,
        )

        // Assert
        Truth.assertThat(transaction!!.inputs).containsExactly(
            SuiCallArg.BalanceWithdrawal(amount = 400, coinType = SuiTypeTag.parse(TOKEN_TYPE)),
            SuiCallArg.OwnedObject(objectRef(1)),
            SuiCallArg.PureU64(500),
            SuiCallArg.PureAddress(RECIPIENT),
        ).inOrder()
        Truth.assertThat(transaction.commands).containsExactly(
            redeem(TOKEN_TYPE, input = 0),
            SuiCommand.MergeCoins(SuiArgument.Input(1), listOf(SuiArgument.Result(0))),
            SuiCommand.SplitCoins(SuiArgument.Input(1), listOf(SuiArgument.Input(2))),
            SuiCommand.TransferObjects(listOf(SuiArgument.NestedResult(2, 0)), SuiArgument.Input(3)),
        ).inOrder()
        Truth.assertThat(transaction.gasPayment).containsExactly(objectRef(0))
        Truth.assertThat(transaction.expiration).isEqualTo(SuiTransactionExpiration.None)
    }

    @Test
    fun `GIVEN token and SUI only in address balance WHEN token transfer THEN redeemed token is sent whole`() {
        // Arrange
        val info = walletInfo(
            suiCoins = emptyList(),
            suiAddressBalance = 10_000,
            tokenCoins = emptyList(),
            tokenAddressBalance = 1_000,
        )

        // Act
        val transaction = builder.buildTokenTransfer(
            walletInfo = info,
            coinType = TOKEN_TYPE,
            amount = mist(500),
            recipient = RECIPIENT,
            gas = gas,
        )

        // Assert
        Truth.assertThat(transaction!!.commands).containsExactly(
            redeem(TOKEN_TYPE, input = 0),
            SuiCommand.TransferObjects(listOf(SuiArgument.Result(0)), SuiArgument.Input(1)),
        ).inOrder()
        Truth.assertThat(transaction.gasPayment).isEmpty()
        Truth.assertThat(transaction.expiration).isEqualTo(validDuring)
    }

    @Test
    fun `GIVEN gas paid by address balance WHEN pay-all simulated THEN half of address balance is withdrawn`() {
        val info = walletInfo(suiCoins = listOf(40L), suiAddressBalance = 10_001)

        Truth.assertThat(builder.payAllSimulationAmount(info)).isEqualTo(mist(5_040))
    }

    @Test
    fun `GIVEN gas paid by address balance WHEN max budget THEN it is half of balance left after withdrawal`() {
        val info = walletInfo(suiCoins = listOf(40L), suiAddressBalance = 10_000)

        Truth.assertThat(builder.maxGasBudgetForCoin(info, mist(5_040))).isEqualTo(mist(2_500))
    }

    @Test
    fun `GIVEN more SUI coins than gas payment takes WHEN coin transfer THEN the rest is merged into gas coin`() {
        // Arrange
        val info = walletInfo(suiCoins = List(size = 300) { 100L }, suiAddressBalance = 10)

        // Act
        val transaction = builder.buildCoinTransfer(
            walletInfo = info,
            amount = mist(29_000),
            recipient = RECIPIENT,
            gas = gas,
        )

        // Assert
        Truth.assertThat(transaction!!.gasPayment).isEqualTo(List(size = 256) { objectRef(it) })
        Truth.assertThat(transaction.commands.first()).isEqualTo(
            SuiCommand.MergeCoins(SuiArgument.GasCoin, List(size = 44) { SuiArgument.Input(it) }),
        )
    }

    @Test
    fun `GIVEN malformed token type WHEN dry run THEN failure is returned instead of a crash`() {
        // Arrange
        val info = walletInfo(suiCoins = emptyList(), suiAddressBalance = 10_000)
        val malformedType = "0x1::token"
        val token = Token(symbol = "TKN", contractAddress = malformedType, decimals = 0)

        // Act
        val result = builder.buildForDryRun(
            walletInfo = info.copy(mistAddressBalances = info.mistAddressBalances + (malformedType to mist(1_000))),
            amount = Amount(token, BigDecimal(500)),
            destination = RECIPIENT,
            gasPrice = GAS_PRICE,
            epoch = EPOCH,
        )

        // Assert
        Truth.assertThat(result).isInstanceOf(Result.Failure::class.java)
    }

    @Test
    fun `GIVEN transfer paid by address balance WHEN serialized THEN matches BCS accepted by mainnet`() {
        // Arrange
        val info = walletInfo(suiCoins = emptyList(), suiAddressBalance = 10_000)

        // Act
        val bcs = builder.buildCoinTransfer(
            walletInfo = info,
            amount = mist(5_000),
            recipient = RECIPIENT,
            gas = gas,
        )!!.toBcs()

        // Assert
        Truth.assertThat(bcs.toHexString().lowercase()).isEqualTo(EXPECTED_BCS)
    }

    private fun walletInfo(
        suiCoins: List<Long>,
        suiAddressBalance: Long,
        tokenCoins: List<Long> = emptyList(),
        tokenAddressBalance: Long = 0,
    ): SuiWalletInfo {
        val coins = suiCoins.mapIndexed { index, balance -> coin(index, SuiConstants.COIN_TYPE, balance) } +
            tokenCoins.mapIndexed { index, balance -> coin(suiCoins.size + index, TOKEN_TYPE, balance) }
        return SuiWalletInfo(
            suiCoinsBalance = mist(suiCoins.sum()),
            coins = coins,
            mistAddressBalances = mapOf(
                SuiConstants.COIN_TYPE to mist(suiAddressBalance),
                TOKEN_TYPE to mist(tokenAddressBalance),
            ),
        )
    }

    private fun coin(index: Int, coinType: String, balance: Long) = SuiCoin(
        objectId = objectRef(index).objectId,
        coinType = coinType,
        version = objectRef(index).version,
        digest = objectRef(index).digest,
        mistBalance = mist(balance),
    )

    private fun objectRef(index: Int) = SuiObjectRef(
        objectId = "0x" + "%064x".format(index + 1),
        version = 100L + index,
        digest = DIGEST,
    )

    private fun redeem(coinType: String, input: Int) = SuiCommand.MoveCall(
        packageId = "0x2",
        module = "coin",
        function = "redeem_funds",
        typeArguments = listOf(SuiTypeTag.parse(coinType)),
        arguments = listOf(SuiArgument.Input(input)),
    )

    private fun mist(value: Long) = BigDecimal(value)

    private companion object {
        const val SENDER = "0xf861d2ba5e9a4e590ee0d5a2baba787d9bdf7f10e9a2c0689e2c52a464b28475"
        const val RECIPIENT = "0x0feb54a725aa357ff2f5bc6bb023c05b310285bd861275a30521f339a434ebb3"
        const val TOKEN_TYPE = "0xdba34672e30cb065b1f93e3ab55318768fd6fef66c15942c9f7cb846e2f900e7::usdc::USDC"
        const val DIGEST = "5D2NF3dbYhGqsQsRmRMJxBNQMiPPt6nXX7BZGnsCF6a9"
        const val GAS_PRICE = 100L
        const val EPOCH = 1292L
        const val NONCE = 7L
        const val EXPECTED_BCS = "00000202008813000000000000000700000000000000000000000000000000000000000000000000000000" +
            "000000020373756903535549000000200feb54a725aa357ff2f5bc6bb023c05b310285bd861275a30521f339a434ebb30200000000" +
            "000000000000000000000000000000000000000000000000000000000204636f696e0c72656465656d5f66756e64730107000000" +
            "0000000000000000000000000000000000000000000000000000000002037375690353554900010100000101020000010100f861" +
            "d2ba5e9a4e590ee0d5a2baba787d9bdf7f10e9a2c0689e2c52a464b2847500f861d2ba5e9a4e590ee0d5a2baba787d9bdf7f10e9" +
            "a2c0689e2c52a464b284756400000000000000640000000000000002010c05000000000000010d050000000000000000203583" +
            "4a8ac17ca48fb14ac8f99c17c98747e95dd07294ae41a46b382246a4499b07000000"
    }
}