package com.tangem.blockchain.blockchains.bitcoin

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinAddressInfo
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkProvider
import com.tangem.blockchain.blockchains.bitcoin.network.UsedAddress
import com.tangem.blockchain.blockchains.bitcoin.network.XpubInfoResponse
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.DynamicAddressesManager
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.address.Address
import com.tangem.blockchain.common.address.AddressType
import com.tangem.blockchain.extensions.Result
import com.tangem.crypto.CryptoUtils
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class BitcoinWalletManagerXpubTest {

    private lateinit var networkProvider: BitcoinNetworkProvider
    private lateinit var walletManager: BitcoinWalletManager

    private val testAccountXpub = ExtendedPublicKey(
        publicKey = "0339a36013301597daef41fbe593a02cc513d0b55527ec2df1050e2e8ff49c85c2".hexToByteArray(),
        chainCode = "873dff81c02f525623fd1fe5167eac3a55a049de3d314bb42ee227ffed37d508".hexToByteArray(),
        depth = 3,
    )

    // Serialized from testAccountXpub — generated in setup() after CryptoUtils.initCrypto()
    private lateinit var testXpub: String

    @Before
    fun setup() {
        CryptoUtils.initCrypto()
        testXpub = testAccountXpub.serialize(com.tangem.crypto.NetworkType.Mainnet)
        mockkStatic(android.util.Log::class)
        every { android.util.Log.d(any(), any()) } returns 0
        every { android.util.Log.e(any(), any()) } returns 0

        networkProvider = mockk(relaxed = true)

        val wallet = Wallet(
            blockchain = Blockchain.Bitcoin,
            addresses = setOf(
                Address("bc1qtest000000000000000000000000000000", AddressType.Default),
            ),
            publicKey = Wallet.PublicKey(testAccountXpub.publicKey, null),
            tokens = emptySet(),
        )

        val transactionBuilder = BitcoinTransactionBuilder(
            testAccountXpub.publicKey,
            Blockchain.Bitcoin,
            wallet.addresses,
        )

        walletManager = BitcoinWalletManager(
            wallet = wallet,
            transactionBuilder = transactionBuilder,
            networkProvider = networkProvider,
            feesCalculator = BitcoinFeesCalculator(Blockchain.Bitcoin),
        )
    }

    // ========== updateInternal tests ==========

    @Test
    fun updateInternal_xpubNull_usesSingleAddress() = runTest {
        assertThat(walletManager.isDynamicAddressesEnabled).isFalse()

        coEvery { networkProvider.getInfo(any()) } returns Result.Success(
            BitcoinAddressInfo(
                balance = BigDecimal("0.01"),
                unspentOutputs = emptyList(),
                recentTransactions = emptyList(),
            ),
        )

        walletManager.updateInternal()

        coVerify(exactly = 1) { networkProvider.getInfo(any()) }
        coVerify(exactly = 0) { networkProvider.getInfoByXpub(any()) }
    }

    @Test
    fun updateInternal_xpubSet_callsGetInfoByXpub() = runTest {
        walletManager.enableDynamicAddresses(testXpub)

        coEvery { networkProvider.getInfoByXpub(any()) } returns Result.Success(
            XpubInfoResponse(
                balance = BigDecimal("0.05"),
                unspentOutputs = emptyList(),
                usedAddresses = listOf(
                    UsedAddress("bc1qaddr0", "m/84'/0'/0'/0/0", BigDecimal("0.03")),
                    UsedAddress("bc1qaddr1", "m/84'/0'/0'/0/1", BigDecimal("0.02")),
                ),
                hasUnconfirmed = false,
                recentTransactions = emptyList(),
            ),
        )

        walletManager.updateInternal()

        coVerify(exactly = 0) { networkProvider.getInfo(any()) }
        coVerify(exactly = 1) { networkProvider.getInfoByXpub(any()) }
    }

    // ========== buildDescriptor tests ==========

    @Test
    fun buildDescriptor_btc_wpkh() {
        walletManager = createWalletManager(Blockchain.Bitcoin)
        val descriptor = walletManager.buildDescriptor(testXpub)
        assertThat(descriptor).isEqualTo("wpkh($testXpub)")
    }

    @Test
    fun buildDescriptor_doge_pkh() {
        walletManager = createWalletManager(Blockchain.Dogecoin)
        val descriptor = walletManager.buildDescriptor(testXpub)
        assertThat(descriptor).isEqualTo("pkh($testXpub)")
    }

    @Test
    fun buildDescriptor_ltc_wpkh() {
        walletManager = createWalletManager(Blockchain.Litecoin)
        val descriptor = walletManager.buildDescriptor(testXpub)
        assertThat(descriptor).isEqualTo("wpkh($testXpub)")
    }

    @Test
    fun updateInternal_xpubSet_btc_usesWpkhDescriptor() = runTest {
        walletManager.enableDynamicAddresses(testXpub)

        coEvery { networkProvider.getInfoByXpub(any()) } returns Result.Success(
            XpubInfoResponse(
                balance = BigDecimal.ZERO,
                unspentOutputs = emptyList(),
                usedAddresses = emptyList(),
                hasUnconfirmed = false,
                recentTransactions = emptyList(),
            ),
        )

        walletManager.updateInternal()

        coVerify { networkProvider.getInfoByXpub(match { it.startsWith("wpkh(") }) }
    }

    // ========== DynamicAddressesManager interface tests ==========

    @Test
    fun enableDisable_dynamicAddresses() {
        assertThat(walletManager.isDynamicAddressesEnabled).isFalse()

        walletManager.enableDynamicAddresses(testXpub)
        assertThat(walletManager.isDynamicAddressesEnabled).isTrue()

        walletManager.disableDynamicAddresses()
        assertThat(walletManager.isDynamicAddressesEnabled).isFalse()
    }

    @Test
    fun walletManager_isDynamicAddressesManager() {
        assertThat(walletManager).isInstanceOf(DynamicAddressesManager::class.java)
    }

    // ========== Helpers ==========

    private fun createWalletManager(blockchain: Blockchain): BitcoinWalletManager {
        val addresses = BitcoinAddressService(blockchain).makeAddresses(testAccountXpub.publicKey)

        val wallet = Wallet(
            blockchain = blockchain,
            addresses = addresses,
            publicKey = Wallet.PublicKey(testAccountXpub.publicKey, null),
            tokens = emptySet(),
        )

        val provider: BitcoinNetworkProvider = mockk(relaxed = true)
        return BitcoinWalletManager(
            wallet = wallet,
            transactionBuilder = BitcoinTransactionBuilder(testAccountXpub.publicKey, blockchain, addresses),
            networkProvider = provider,
            feesCalculator = BitcoinFeesCalculator(blockchain),
        )
    }

    private fun String.hexToByteArray(): ByteArray {
        check(length % 2 == 0)
        return chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }
}