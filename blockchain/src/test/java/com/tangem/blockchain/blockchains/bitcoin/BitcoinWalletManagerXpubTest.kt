package com.tangem.blockchain.blockchains.bitcoin

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinAddressInfo
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkProvider
import com.tangem.blockchain.blockchains.bitcoin.network.UsedAddress
import com.tangem.blockchain.blockchains.bitcoin.network.XpubInfoResponse
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.DynamicAddressesManager
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.address.Address
import com.tangem.blockchain.common.address.AddressType
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.common.CompletionResult
import com.tangem.crypto.CryptoUtils
import com.tangem.crypto.hdWallet.DerivationNode
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
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

        // BTC issues two descriptor requests (wpkh + pkh); return the same body for both.
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
        coVerify(exactly = 2) { networkProvider.getInfoByXpub(any()) }
    }

    @Test
    fun updateInternal_xpubSet_btc_callsBothWpkhAndPkh() = runTest {
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

        coVerify(exactly = 1) { networkProvider.getInfoByXpub(match { it.startsWith("wpkh(") }) }
        coVerify(exactly = 1) { networkProvider.getInfoByXpub(match { it.startsWith("pkh(") }) }
    }

    @Test
    fun updateInternal_xpubSet_btc_mergesSegwitAndLegacyBalances() = runTest {
        walletManager.enableDynamicAddresses(testXpub)

        coEvery { networkProvider.getInfoByXpub(match { it.startsWith("wpkh(") }) } returns Result.Success(
            XpubInfoResponse(
                balance = BigDecimal("0.05"),
                unspentOutputs = emptyList(),
                usedAddresses = listOf(
                    UsedAddress("bc1qaddr0", "m/84'/0'/0'/0/0", BigDecimal("0.05")),
                ),
                hasUnconfirmed = false,
                recentTransactions = emptyList(),
            ),
        )
        coEvery { networkProvider.getInfoByXpub(match { it.startsWith("pkh(") }) } returns Result.Success(
            XpubInfoResponse(
                balance = BigDecimal("0.03"),
                unspentOutputs = emptyList(),
                usedAddresses = listOf(
                    UsedAddress("1addrLegacy", "m/84'/0'/0'/0/0", BigDecimal("0.03")),
                ),
                hasUnconfirmed = false,
                recentTransactions = emptyList(),
            ),
        )

        walletManager.updateInternal()

        // Aggregate balance = SegWit (0.05) + Legacy (0.03) = 0.08
        assertThat(walletManager.wallet.amounts[com.tangem.blockchain.common.AmountType.Coin]?.value)
            .isEqualTo(BigDecimal("0.08"))
        // usedAddresses contains both, and scriptType is stamped correctly.
        val used = walletManager.usedAddresses
        assertThat(used).hasSize(2)
        assertThat(used.first { it.address == "bc1qaddr0" }.scriptType).isEqualTo(AddressType.Default)
        assertThat(used.first { it.address == "1addrLegacy" }.scriptType).isEqualTo(AddressType.Legacy)
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

    // ========== UTXO signing-path rewrite tests ==========

    @Test
    fun updateInternal_xpubSet_rewritesUtxoPathsToWalletDerivationBranch() = runTest {
        // The wallet's real branch is m/44'/…, while BlockBook labels wpkh children as m/84'/….
        val basePath = DerivationPath("m/44'/0'/0'/0/0")
        val (wm, builder) = createWalletManagerWithDerivation(basePath)
        wm.enableDynamicAddresses(testXpub)

        coEvery { networkProvider.getInfoByXpub(match { it.startsWith("wpkh(") }) } returns Result.Success(
            xpubResponse(
                utxo(path = "m/84'/0'/0'/0/2"),
                utxo(path = "m/84'/0'/0'/1/5"),
            ),
        )
        coEvery { networkProvider.getInfoByXpub(match { it.startsWith("pkh(") }) } returns
            Result.Success(xpubResponse())

        wm.updateInternal()

        val utxos = builder.unspentOutputs.orEmpty()
        assertThat(utxos).hasSize(2)
        assertThat(utxos.map { DerivationPath(it.derivationPath!!) }).containsExactly(
            DerivationPath("m/44'/0'/0'/0/2"),
            DerivationPath("m/44'/0'/0'/1/5"),
        )
        assertThat(utxos.all { it.publicKey != null }).isTrue()
    }

    @Test
    fun updateInternal_xpubSet_walletWithoutDerivation_keepsBlockBookPaths() = runTest {
        val (wm, builder) = createWalletManagerWithDerivation(basePath = null)
        wm.enableDynamicAddresses(testXpub)

        coEvery { networkProvider.getInfoByXpub(match { it.startsWith("wpkh(") }) } returns Result.Success(
            xpubResponse(utxo(path = "m/84'/0'/0'/0/2")),
        )
        coEvery { networkProvider.getInfoByXpub(match { it.startsWith("pkh(") }) } returns
            Result.Success(xpubResponse())

        wm.updateInternal()

        // No wallet derivation to rebuild from — fall back to the BlockBook label.
        val utxos = builder.unspentOutputs.orEmpty()
        assertThat(utxos.map { it.derivationPath }).containsExactly("m/84'/0'/0'/0/2")
        assertThat(utxos.single().publicKey).isNotNull()
    }

    @Test
    fun updateInternal_xpubSet_xpubNotAtWalletAccountPath_keepsBlockBookPaths() = runTest {
        // The wallet's hd key is the ACCOUNT key itself, so the xpub's (0,0) child does not match it —
        // the dropLast(2) invariant is broken and the rebuild must be skipped.
        val (wm, builder) = createWalletManagerWithDerivation(
            basePath = DerivationPath("m/44'/0'/0'/0/0"),
            hdWalletKey = testAccountXpub,
        )
        wm.enableDynamicAddresses(testXpub)

        coEvery { networkProvider.getInfoByXpub(match { it.startsWith("wpkh(") }) } returns Result.Success(
            xpubResponse(utxo(path = "m/84'/0'/0'/0/2")),
        )
        coEvery { networkProvider.getInfoByXpub(match { it.startsWith("pkh(") }) } returns
            Result.Success(xpubResponse())

        wm.updateInternal()

        assertThat(builder.unspentOutputs.orEmpty().map { it.derivationPath }).containsExactly("m/84'/0'/0'/0/2")
    }

    // ========== Signing-mode gate tests ==========

    @Test
    fun send_dynamicEnabled_baseOnlyInputs_routesToSingleAddressSigning() = runTest {
        val (wm, builder) = createWalletManagerWithMockedBuilder()
        wm.enableDynamicAddresses(testXpub)
        every { builder.unspentOutputs } returns listOf(
            utxo(path = "m/84'/0'/0'/0/0", publicKey = testAccountXpub.publicKey),
            utxo(path = "m/84'/0'/0'/0/0", publicKey = testAccountXpub.publicKey.copyOf()),
        )
        every { builder.buildToSign(any(), any()) } returns
            Result.Failure(BlockchainSdkError.CustomError("stop before signing"))

        wm.send(transactionData(), mockk(relaxed = true))

        verify(exactly = 1) { builder.buildToSign(any(), any()) }
        verify(exactly = 0) { builder.buildToSignMultiAddress(any(), any(), any()) }
    }

    @Test
    fun send_dynamicEnabled_nonBaseInput_routesToMultiAddressSigning() = runTest {
        val (wm, builder) = createWalletManagerWithMockedBuilder()
        wm.enableDynamicAddresses(testXpub)
        every { builder.unspentOutputs } returns listOf(
            utxo(path = "m/84'/0'/0'/0/0", publicKey = testAccountXpub.publicKey),
            utxo(path = "m/84'/0'/0'/0/2", publicKey = ByteArray(33) { 3 }),
        )
        every { builder.buildToSignMultiAddress(any(), any(), any()) } returns
            Result.Failure(BlockchainSdkError.CustomError("stop before signing"))

        wm.send(transactionData(), mockk(relaxed = true))

        verify(exactly = 1) { builder.buildToSignMultiAddress(any(), any(), any()) }
        verify(exactly = 0) { builder.buildToSign(any(), any()) }
    }

    @Test
    fun send_dynamicEnabled_singleNonBaseKeyOnly_routesToMultiAddressSigning() = runTest {
        // All UTXOs on ONE non-base address (e.g. everything on a change address after a send-max):
        // the old `distinct >= 2` gate wrongly routed this to single-address signing.
        val (wm, builder) = createWalletManagerWithMockedBuilder()
        wm.enableDynamicAddresses(testXpub)
        val nonBaseKey = ByteArray(33) { 3 }
        every { builder.unspentOutputs } returns listOf(
            utxo(path = "m/84'/0'/0'/1/0", publicKey = nonBaseKey),
            utxo(path = "m/84'/0'/0'/1/0", publicKey = nonBaseKey.copyOf()),
        )
        every { builder.buildToSignMultiAddress(any(), any(), any()) } returns
            Result.Failure(BlockchainSdkError.CustomError("stop before signing"))

        wm.send(transactionData(), mockk(relaxed = true))

        verify(exactly = 1) { builder.buildToSignMultiAddress(any(), any(), any()) }
        verify(exactly = 0) { builder.buildToSign(any(), any()) }
    }

    // ========== Signature/input association tests ==========

    @Test
    fun sendMultiAddress_associatesSignatureToInputByPublicKey_regardlessOfSignerValueOrder() = runTest {
        // Each input's signature must be resolved by its own public key, not by the Map's value order.
        // The old `.values.toList().reversed()` reconstruction misassigns as soon as the signer's Map
        // iteration order is not the exact reverse of the input order (the hot signer never reverses).
        val (wm, builder) = createWalletManagerWithMockedBuilder()
        wm.enableDynamicAddresses(testXpub)

        val key0 = ByteArray(33) { 10 }
        val key1 = ByteArray(33) { 11 }
        val key2 = ByteArray(33) { 12 }
        val sig0 = byteArrayOf(0xA0.toByte())
        val sig1 = byteArrayOf(0xB1.toByte())
        val sig2 = byteArrayOf(0xC2.toByte())

        val infos = listOf(
            MultiAddressSignatureInfo(ByteArray(32) { 0 }, key0, DerivationPath("m/84'/0'/0'/0/0")),
            MultiAddressSignatureInfo(ByteArray(32) { 1 }, key1, DerivationPath("m/84'/0'/0'/1/3")),
            MultiAddressSignatureInfo(ByteArray(32) { 2 }, key2, DerivationPath("m/84'/0'/0'/0/7")),
        )

        // At least one non-base input routes send() to the multi-address path.
        every { builder.unspentOutputs } returns listOf(
            utxo(path = "m/84'/0'/0'/0/0", publicKey = testAccountXpub.publicKey),
            utxo(path = "m/84'/0'/0'/1/3", publicKey = key1),
        )
        every { builder.buildToSignMultiAddress(any(), any(), any()) } returns Result.Success(infos)

        val captured = slot<List<ByteArray>>()
        every { builder.buildToSendMultiAddress(capture(captured), any()) } returns ByteArray(0)

        // Deliberately scrambled Map order — input order is key0, key1, key2.
        val signer = mockk<TransactionSigner>()
        coEvery { signer.multiSign(any(), any()) } returns CompletionResult.Success(
            linkedMapOf(key2 to sig2, key0 to sig0, key1 to sig1),
        )
        coEvery { networkProvider.sendTransaction(any()) } returns
            SimpleResult.Failure(BlockchainSdkError.CustomError("stop after build"))

        wm.send(transactionData(), signer)

        assertThat(captured.captured).containsExactly(sig0, sig1, sig2).inOrder()
    }

    @Test
    fun sendMultiAddress_twoInputsOnSameAddress_eachInputKeepsItsOwnSignature() = runTest {
        // Two UTXOs on the same derived address share identical key bytes but are distinct instances
        // (buildToSignMultiAddress copies the pubkey per input). Neither the Map keying nor the lookup
        // may collapse them, otherwise one input receives the other's signature.
        val (wm, builder) = createWalletManagerWithMockedBuilder()
        wm.enableDynamicAddresses(testXpub)

        val keyBytes = ByteArray(33) { 3 }
        val keyA = keyBytes.copyOf()
        val keyB = keyBytes.copyOf()
        val sigA = byteArrayOf(0xAA.toByte())
        val sigB = byteArrayOf(0xBB.toByte())

        val infos = listOf(
            MultiAddressSignatureInfo(ByteArray(32) { 1 }, keyA, DerivationPath("m/84'/0'/0'/1/0")),
            MultiAddressSignatureInfo(ByteArray(32) { 2 }, keyB, DerivationPath("m/84'/0'/0'/1/0")),
        )

        every { builder.unspentOutputs } returns listOf(
            utxo(path = "m/84'/0'/0'/1/0", publicKey = keyA),
            utxo(path = "m/84'/0'/0'/1/0", publicKey = keyB),
        )
        every { builder.buildToSignMultiAddress(any(), any(), any()) } returns Result.Success(infos)

        val captured = slot<List<ByteArray>>()
        every { builder.buildToSendMultiAddress(capture(captured), any()) } returns ByteArray(0)

        val signer = mockk<TransactionSigner>()
        coEvery { signer.multiSign(any(), any()) } returns CompletionResult.Success(
            linkedMapOf(keyA to sigA, keyB to sigB),
        )
        coEvery { networkProvider.sendTransaction(any()) } returns
            SimpleResult.Failure(BlockchainSdkError.CustomError("stop after build"))

        wm.send(transactionData(), signer)

        assertThat(captured.captured).containsExactly(sigA, sigB).inOrder()
    }

    @Test
    fun sendMultiAddress_coldSignerReverseOrder_pairsCorrectly() = runTest {
        // The cold signer (MultipleSignCommand drains an ArrayDeque from the tail) emits the Map in
        // exact REVERSE input order — the case the old `.reversed()` reconstruction was written for.
        val (wm, builder) = createWalletManagerWithMockedBuilder()
        wm.enableDynamicAddresses(testXpub)

        val key0 = ByteArray(33) { 10 }
        val key1 = ByteArray(33) { 11 }
        val key2 = ByteArray(33) { 12 }
        val sig0 = byteArrayOf(0xA0.toByte())
        val sig1 = byteArrayOf(0xB1.toByte())
        val sig2 = byteArrayOf(0xC2.toByte())

        val infos = listOf(
            MultiAddressSignatureInfo(ByteArray(32) { 0 }, key0, DerivationPath("m/84'/0'/0'/0/0")),
            MultiAddressSignatureInfo(ByteArray(32) { 1 }, key1, DerivationPath("m/84'/0'/0'/1/3")),
            MultiAddressSignatureInfo(ByteArray(32) { 2 }, key2, DerivationPath("m/84'/0'/0'/0/7")),
        )

        every { builder.unspentOutputs } returns listOf(
            utxo(path = "m/84'/0'/0'/1/3", publicKey = key1),
        )
        every { builder.buildToSignMultiAddress(any(), any(), any()) } returns Result.Success(infos)

        val captured = slot<List<ByteArray>>()
        every { builder.buildToSendMultiAddress(capture(captured), any()) } returns ByteArray(0)

        val signer = mockk<TransactionSigner>()
        coEvery { signer.multiSign(any(), any()) } returns CompletionResult.Success(
            linkedMapOf(key2 to sig2, key1 to sig1, key0 to sig0),
        )
        coEvery { networkProvider.sendTransaction(any()) } returns
            SimpleResult.Failure(BlockchainSdkError.CustomError("stop after build"))

        wm.send(transactionData(), signer)

        assertThat(captured.captured).containsExactly(sig0, sig1, sig2).inOrder()
    }

    @Test
    fun sendMultiAddress_signerMapMissingInputKey_returnsFailure() = runTest {
        // A signer that loses an input's signature must produce a hard local failure —
        // never a broadcast with a partially/incorrectly signed transaction.
        val (wm, builder) = createWalletManagerWithMockedBuilder()
        wm.enableDynamicAddresses(testXpub)

        val key0 = ByteArray(33) { 10 }
        val key1 = ByteArray(33) { 11 }
        val sig0 = byteArrayOf(0xA0.toByte())

        val infos = listOf(
            MultiAddressSignatureInfo(ByteArray(32) { 0 }, key0, DerivationPath("m/84'/0'/0'/0/0")),
            MultiAddressSignatureInfo(ByteArray(32) { 1 }, key1, DerivationPath("m/84'/0'/0'/1/3")),
        )

        every { builder.unspentOutputs } returns listOf(
            utxo(path = "m/84'/0'/0'/1/3", publicKey = key1),
        )
        every { builder.buildToSignMultiAddress(any(), any(), any()) } returns Result.Success(infos)
        every { builder.buildToSendMultiAddress(any(), any()) } returns ByteArray(0)

        val signer = mockk<TransactionSigner>()
        coEvery { signer.multiSign(any(), any()) } returns CompletionResult.Success(
            linkedMapOf(key0 to sig0),
        )

        val result = wm.send(transactionData(), signer)

        assertThat(result).isInstanceOf(Result.Failure::class.java)
        assertThat((result as Result.Failure).error.customMessage).contains("Missing signature")
        verify(exactly = 0) { builder.buildToSendMultiAddress(any(), any()) }
        coVerify(exactly = 0) { networkProvider.sendTransaction(any()) }
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

    private fun createWalletManagerWithDerivation(
        basePath: DerivationPath?,
        hdWalletKey: ExtendedPublicKey = baseKeyFromTestXpub(),
    ): Pair<BitcoinWalletManager, BitcoinTransactionBuilder> {
        val derivationType = basePath?.let {
            Wallet.PublicKey.DerivationType.Plain(Wallet.HDKey(extendedPublicKey = hdWalletKey, path = it))
        }
        val wallet = Wallet(
            blockchain = Blockchain.Bitcoin,
            addresses = setOf(
                Address("bc1qtest000000000000000000000000000000", AddressType.Default),
            ),
            publicKey = Wallet.PublicKey(testAccountXpub.publicKey, derivationType),
            tokens = emptySet(),
        )
        val builder = BitcoinTransactionBuilder(testAccountXpub.publicKey, Blockchain.Bitcoin, wallet.addresses)
        val manager = BitcoinWalletManager(
            wallet = wallet,
            transactionBuilder = builder,
            networkProvider = networkProvider,
            feesCalculator = BitcoinFeesCalculator(Blockchain.Bitcoin),
        )
        return manager to builder
    }

    // On real wallets the blockchainKey is the xpub's (0,0) child — mirror that in the fixture.
    private fun baseKeyFromTestXpub(): ExtendedPublicKey = testAccountXpub
        .derivePublicKey(DerivationNode.NonHardened(0))
        .derivePublicKey(DerivationNode.NonHardened(0))

    private fun createWalletManagerWithMockedBuilder(): Pair<BitcoinWalletManager, BitcoinTransactionBuilder> {
        val builder = mockk<BitcoinTransactionBuilder>(relaxed = true)
        val wallet = Wallet(
            blockchain = Blockchain.Bitcoin,
            addresses = setOf(
                Address("bc1qtest000000000000000000000000000000", AddressType.Default),
            ),
            publicKey = Wallet.PublicKey(testAccountXpub.publicKey, null),
            tokens = emptySet(),
        )
        val manager = BitcoinWalletManager(
            wallet = wallet,
            transactionBuilder = builder,
            networkProvider = networkProvider,
            feesCalculator = BitcoinFeesCalculator(Blockchain.Bitcoin),
        )
        return manager to builder
    }

    private fun transactionData() = TransactionData.Uncompiled(
        amount = Amount(Blockchain.Bitcoin),
        fee = Fee.Common(Amount(Blockchain.Bitcoin)),
        sourceAddress = "bc1qsource",
        destinationAddress = "bc1qdestination",
    )

    private fun xpubResponse(vararg utxos: BitcoinUnspentOutput) = XpubInfoResponse(
        balance = utxos.sumOf { it.amount },
        unspentOutputs = utxos.toList(),
        usedAddresses = emptyList(),
        hasUnconfirmed = false,
        recentTransactions = emptyList(),
    )

    private fun utxo(path: String, publicKey: ByteArray? = null) = BitcoinUnspentOutput(
        amount = BigDecimal.ONE,
        outputIndex = 1,
        transactionHash = ByteArray(32),
        outputScript = ByteArray(0),
        address = "bc1qutxoaddr",
        derivationPath = path,
        publicKey = publicKey,
    )

    private fun String.hexToByteArray(): ByteArray {
        check(length % 2 == 0)
        return chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }
}