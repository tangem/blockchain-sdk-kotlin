package com.tangem.blockchain.blockchains.tron

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.tron.network.BlockHeader
import com.tangem.blockchain.blockchains.tron.network.RawData
import com.tangem.blockchain.blockchains.tron.network.TronBlock
import com.tangem.blockchain.blockchains.tron.network.TronChainParameters
import com.tangem.blockchain.blockchains.tron.network.TronGetAccountResourceResponse
import com.tangem.blockchain.blockchains.tron.network.TronNetworkService
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.address.Address
import com.tangem.blockchain.common.smartcontract.CompiledSmartContractCallData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.extensions.Result
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toHexString
import com.tangem.crypto.CryptoUtils
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

/**
 * A native-value DEX swap (Coin + call data) must have its energy estimated from the raw
 * call data against the router (the new `getMaxEnergyUseForCallData` path), while a plain coin
 * transfer must keep the original behavior — no contract energy at all.
 */
internal class TronWalletManagerFeeTest {

    private val walletAddress = "TU1BRXbr6EmKmrLL4Kymv7Wp18eYFkRfAF"
    private val router = "TXXxc9NsHndfQ2z9kMKyWpYa5T3QbhKGwn"

    private val networkService: TronNetworkService = mockk()

    private lateinit var walletManager: TronWalletManager

    private val tronBlock = TronBlock(
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

    @Before
    fun setup() {
        // getFee signs a dummy transaction to measure its size for the bandwidth part of the fee.
        // The signer is TronWalletManager's own (non-injectable) DummySigner doing real secp256k1,
        // so we register the real crypto provider — as the other blockchain WalletManager tests do —
        // rather than mocking the signer.
        CryptoUtils.initCrypto()

        every { networkService.host } returns "https://api.tron"
        coEvery { networkService.getNowBlock() } returns Result.Success(tronBlock)
        coEvery { networkService.checkIfAccountExists(any()) } returns true
        coEvery { networkService.getAccountResource(any()) } returns Result.Success(
            TronGetAccountResourceResponse(
                freeNetUsed = 0,
                freeNetLimit = 5000,
                energyLimit = 0,
                energyUsed = 0,
            ),
        )
        coEvery { networkService.getChainParameters() } returns Result.Success(
            TronChainParameters(
                sunPerEnergyUnit = 280,
                dynamicEnergyMaxFactor = 1500,
                dynamicIncreaseFactor = 0,
                memoFee = MEMO_FEE_SUN,
            ),
        )

        walletManager = TronWalletManager(
            wallet = Wallet(
                blockchain = Blockchain.Tron,
                addresses = setOf(Address(walletAddress)),
                publicKey = mockk(),
                tokens = emptySet(),
            ),
            transactionHistoryProvider = mockk(),
            transactionBuilder = TronTransactionBuilder(),
            networkService = networkService,
        )
    }

    @Test
    fun `GIVEN native-value swap WHEN getFee THEN energy is estimated from raw call data`() = runTest {
        // Arrange
        val callData = CompiledSmartContractCallData(
            "a9059cbb00000000000000000000000000000000000000000000000000000000000f4240".hexToBytes(),
        )
        coEvery {
            networkService.getMaxEnergyUseForCallData(
                address = any(),
                contractAddress = any(),
                callDataHex = any(),
                callValue = any(),
            )
        } returns Result.Success(1000L)

        // Act
        val result = walletManager.getFee(
            amount = Amount(BigDecimal.valueOf(5), Blockchain.Tron),
            destination = router,
            callData = callData,
        )

        // Assert
        val fee = ((result as Result.Success).data as TransactionFee.Single).normal as Fee.Tron
        assertThat(fee.feeEnergy).isEqualTo(1000L)
        coVerify(exactly = 1) {
            networkService.getMaxEnergyUseForCallData(
                address = walletAddress,
                contractAddress = router,
                callDataHex = callData.data.toHexString(),
                callValue = 5_000_000L,
            )
        }
    }

    /**
     * A non-empty `raw_data.data` makes the network burn the MEMO_FEE chain parameter on top of
     * bandwidth and energy — 1 TRX on mainnet. It applies to a plain transfer too, which needs no
     * energy estimation, so the parameter has to be fetched on that path as well.
     */
    @Test
    fun `GIVEN transfer with memo WHEN getFee THEN memo fee is added`() = runTest {
        // Arrange
        val withoutMemo = walletManager.feeValue(memo = null)

        // Act
        val withMemo = walletManager.feeValue(memo = "swap-id".encodeToByteArray())

        // Assert
        assertThat(withMemo - withoutMemo).isEquivalentAccordingToCompareTo(BigDecimal.ONE)
    }

    @Test
    fun `GIVEN transfer without memo WHEN getFee THEN chain parameters are not fetched`() = runTest {
        // Act
        walletManager.feeValue(memo = null)

        // Assert
        // A plain transfer must keep its request count — the memo fee is the only reason to ask.
        coVerify(exactly = 0) { networkService.getChainParameters() }
    }

    @Test
    fun `GIVEN memo and inactive destination WHEN getFee THEN memo fee is added to activation fee`() = runTest {
        // Arrange
        coEvery { networkService.checkIfAccountExists(any()) } returns false

        // Act
        val actual = walletManager.feeValue(memo = "swap-id".encodeToByteArray())

        // Assert
        assertThat(actual).isEquivalentAccordingToCompareTo(BigDecimal.valueOf(1.1) + BigDecimal.ONE)
    }

    /** Fee of a plain TRX transfer built as a [TransactionData], which is what carries a memo. */
    private suspend fun TronWalletManager.feeValue(memo: ByteArray?): BigDecimal {
        val result = getFee(
            TransactionData.Uncompiled(
                amount = Amount(BigDecimal.valueOf(5), Blockchain.Tron),
                fee = null,
                sourceAddress = walletAddress,
                destinationAddress = router,
                extras = TronTransactionExtras(memo = memo),
            ),
        )
        return ((result as Result.Success).data as TransactionFee.Single).normal.amount.value!!
    }

    @Test
    fun `GIVEN plain coin transfer WHEN getFee THEN contract energy is not estimated`() = runTest {
        // Act
        val result = walletManager.getFee(
            amount = Amount(BigDecimal.valueOf(5), Blockchain.Tron),
            destination = router,
            callData = null,
        )

        // Assert
        val fee = ((result as Result.Success).data as TransactionFee.Single).normal as Fee.Tron
        assertThat(fee.feeEnergy).isEqualTo(0L)
        coVerify(exactly = 0) {
            networkService.getMaxEnergyUseForCallData(
                address = any(),
                contractAddress = any(),
                callDataHex = any(),
                callValue = any(),
            )
        }
    }

    private companion object {
        /** Mainnet MEMO_FEE: 1 TRX in sun. */
        const val MEMO_FEE_SUN = 1_000_000L
    }
}