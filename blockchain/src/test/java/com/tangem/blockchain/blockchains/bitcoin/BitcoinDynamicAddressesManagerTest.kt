package com.tangem.blockchain.blockchains.bitcoin

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.bitcoin.network.UsedAddress
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.address.AddressType
import com.tangem.crypto.CryptoUtils
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class BitcoinDynamicAddressesManagerTest {

    @Before
    fun setup() {
        CryptoUtils.initCrypto()
    }

    // BIP32 Test Vector 1: seed = "000102030405060708090a0b0c0d0e0f"
    // m/0' public key chain (account-level for testing)
    // Using a known xpub at depth 3 (m/84'/0'/0') for Bitcoin SegWit
    // xpub from: https://github.com/bitcoin/bips/blob/master/bip-0032.mediawiki#test-vectors
    //
    // For practical testing we construct ExtendedPublicKey directly with known values
    // and verify derived addresses match expected BIP32 derivation results.

    // ========== Address Derivation Tests ==========

    @Test
    fun deriveReceiveAddress_index0_btc_returnsBech32() {
        val manager = BitcoinDynamicAddressesManager(BTC_ACCOUNT_XPUB, Blockchain.Bitcoin)

        val derived = manager.deriveAddress(chain = 0, index = 0)

        assertThat(derived.address).startsWith("bc1")
        assertThat(derived.chain).isEqualTo(0)
        assertThat(derived.index).isEqualTo(0)
        assertThat(derived.publicKey).isNotEmpty()
        assertThat(derived.publicKey.size).isEqualTo(33) // compressed public key
    }

    @Test
    fun deriveReceiveAddress_index5_btc_differentFromIndex0() {
        val manager = BitcoinDynamicAddressesManager(BTC_ACCOUNT_XPUB, Blockchain.Bitcoin)

        val derived0 = manager.deriveAddress(chain = 0, index = 0)
        val derived5 = manager.deriveAddress(chain = 0, index = 5)

        assertThat(derived5.address).startsWith("bc1")
        assertThat(derived5.address).isNotEqualTo(derived0.address)
        assertThat(derived5.index).isEqualTo(5)
        assertThat(derived5.publicKey).isNotEqualTo(derived0.publicKey)
    }

    @Test
    fun deriveAddress_doge_returnsLegacyP2PKH() {
        val manager = BitcoinDynamicAddressesManager(DOGE_ACCOUNT_XPUB, Blockchain.Dogecoin)

        val derived = manager.deriveAddress(chain = 0, index = 0)

        assertThat(derived.address).startsWith("D")
        assertThat(derived.chain).isEqualTo(0)
        assertThat(derived.index).isEqualTo(0)
        assertThat(derived.publicKey.size).isEqualTo(33)
    }

    @Test
    fun deriveChangeAddress_index0_correctChain1() {
        val manager = BitcoinDynamicAddressesManager(BTC_ACCOUNT_XPUB, Blockchain.Bitcoin)

        val receive = manager.deriveAddress(chain = 0, index = 0)
        val change = manager.deriveAddress(chain = 1, index = 0)

        // Change and receive at same index must produce different addresses
        assertThat(change.address).isNotEqualTo(receive.address)
        assertThat(change.chain).isEqualTo(1)
        assertThat(change.index).isEqualTo(0)
        assertThat(change.address).startsWith("bc1")
    }

    // ========== Gap-Aware Unused Address Finder Tests ==========

    @Test
    fun findFirstUnused_gapInSequence_returnsGapIndex() {
        val manager = BitcoinDynamicAddressesManager(BTC_ACCOUNT_XPUB, Blockchain.Bitcoin)

        // Derive addresses at known indices to build the "used" list
        val addr0 = manager.deriveAddress(0, 0)
        val addr1 = manager.deriveAddress(0, 1)
        val addr2 = manager.deriveAddress(0, 2)
        val addr3 = manager.deriveAddress(0, 3)
        val addr7 = manager.deriveAddress(0, 7)

        val usedAddresses = listOf(
            UsedAddress(addr0.address, "m/84'/0'/0'/0/0", BigDecimal.ONE),
            UsedAddress(addr1.address, "m/84'/0'/0'/0/1", BigDecimal.ZERO),
            UsedAddress(addr2.address, "m/84'/0'/0'/0/2", BigDecimal.ONE),
            UsedAddress(addr3.address, "m/84'/0'/0'/0/3", BigDecimal.ZERO),
            UsedAddress(addr7.address, "m/84'/0'/0'/0/7", BigDecimal.ONE),
        )

        val firstUnused = manager.findFirstUnusedReceiveAddress(usedAddresses)

        // Index 4 is the first gap
        assertThat(firstUnused.index).isEqualTo(4)
        assertThat(firstUnused.chain).isEqualTo(0)
    }

    @Test
    fun findFirstUnused_noGap_returnsNextIndex() {
        val manager = BitcoinDynamicAddressesManager(BTC_ACCOUNT_XPUB, Blockchain.Bitcoin)

        val addr0 = manager.deriveAddress(0, 0)
        val addr1 = manager.deriveAddress(0, 1)
        val addr2 = manager.deriveAddress(0, 2)
        val addr3 = manager.deriveAddress(0, 3)

        val usedAddresses = listOf(
            UsedAddress(addr0.address, "m/84'/0'/0'/0/0", BigDecimal.ONE),
            UsedAddress(addr1.address, "m/84'/0'/0'/0/1", BigDecimal.ONE),
            UsedAddress(addr2.address, "m/84'/0'/0'/0/2", BigDecimal.ONE),
            UsedAddress(addr3.address, "m/84'/0'/0'/0/3", BigDecimal.ONE),
        )

        val firstUnused = manager.findFirstUnusedReceiveAddress(usedAddresses)

        assertThat(firstUnused.index).isEqualTo(4)
    }

    @Test
    fun findFirstUnused_emptyUsed_returnsIndex0() {
        val manager = BitcoinDynamicAddressesManager(BTC_ACCOUNT_XPUB, Blockchain.Bitcoin)

        val firstUnused = manager.findFirstUnusedReceiveAddress(emptyList())

        assertThat(firstUnused.index).isEqualTo(0)
        assertThat(firstUnused.chain).isEqualTo(0)
    }

    @Test
    fun findFirstUnusedChange_separateFromReceive() {
        val manager = BitcoinDynamicAddressesManager(BTC_ACCOUNT_XPUB, Blockchain.Bitcoin)

        val changeAddr0 = manager.deriveAddress(1, 0)
        val changeAddr1 = manager.deriveAddress(1, 1)

        // Only change addresses (chain=1) are used
        val usedAddresses = listOf(
            UsedAddress(changeAddr0.address, "m/84'/0'/0'/1/0", BigDecimal.ONE),
            UsedAddress(changeAddr1.address, "m/84'/0'/0'/1/1", BigDecimal.ONE),
            // Also add receive addresses - they should NOT affect change search
            UsedAddress(
                manager.deriveAddress(0, 0).address,
                "m/84'/0'/0'/0/0",
                BigDecimal.ONE,
            ),
        )

        val firstUnusedChange = manager.findFirstUnusedChangeAddress(usedAddresses)

        // Change chain should show index 2 as next unused (0 and 1 are used)
        assertThat(firstUnusedChange.index).isEqualTo(2)
        assertThat(firstUnusedChange.chain).isEqualTo(1)
    }

    @Test
    fun findFirstUnusedReceive_legacyEntriesAreIgnored() {
        val manager = BitcoinDynamicAddressesManager(BTC_ACCOUNT_XPUB, Blockchain.Bitcoin)

        val segwit0 = manager.deriveAddress(0, 0)
        val segwit1 = manager.deriveAddress(0, 1)

        // Legacy entries at the same logical path MUST NOT close gaps in the SegWit tree.
        // Only SegWit (Default) used-addresses should drive receive-address gap search.
        val usedAddresses = listOf(
            UsedAddress(
                address = segwit0.address,
                derivationPath = "m/84'/0'/0'/0/0",
                balance = BigDecimal.ONE,
                scriptType = AddressType.Default,
            ),
            UsedAddress(
                address = segwit1.address,
                derivationPath = "m/84'/0'/0'/0/1",
                balance = BigDecimal.ONE,
                scriptType = AddressType.Default,
            ),
            UsedAddress(
                address = "legacyAddr2",
                derivationPath = "m/84'/0'/0'/0/2",
                balance = BigDecimal.ONE,
                scriptType = AddressType.Legacy,
            ),
            UsedAddress(
                address = "legacyAddr3",
                derivationPath = "m/84'/0'/0'/0/3",
                balance = BigDecimal.ONE,
                scriptType = AddressType.Legacy,
            ),
        )

        val firstUnused = manager.findFirstUnusedReceiveAddress(usedAddresses)

        // Even though Legacy fills indices 2 and 3, SegWit only reaches 1 — next unused is 2.
        assertThat(firstUnused.index).isEqualTo(2)
        assertThat(firstUnused.chain).isEqualTo(0)
    }

    // ========== Path Parsing Tests ==========

    @Test
    fun parseChainAndIndex_segwitPath() {
        val (chain, index) = BitcoinDynamicAddressesManager.parseChainAndIndex("m/84'/0'/0'/0/5")

        assertThat(chain).isEqualTo(0)
        assertThat(index).isEqualTo(5)
    }

    @Test
    fun parseChainAndIndex_legacyPath() {
        val (chain, index) = BitcoinDynamicAddressesManager.parseChainAndIndex("m/44'/3'/0'/1/2")

        assertThat(chain).isEqualTo(1)
        assertThat(index).isEqualTo(2)
    }

    companion object {
        // Known account-level extended public key for BTC (m/84'/0'/0')
        // This is a real zpub decoded to raw components
        // zpub6qkhJbASjzPuWqEHpw7upUZ9bc6DSs6FiwaV8rWW8hfYHRuDPQwf6zGmnzubDQNth74ha7KjgSkiQCZpUsPrQb4k3QdDngJ7jXu55nVEuev
        private val BTC_ACCOUNT_XPUB = createTestXpub(
            publicKey = "0339a36013301597daef41fbe593a02cc513d0b55527ec2df1050e2e8ff49c85c2",
            chainCode = "873dff81c02f525623fd1fe5167eac3a55a049de3d314bb42ee227ffed37d508",
        )

        // For Dogecoin testing: use same key structure but with Dogecoin blockchain
        // (Address format will be different but derivation math is the same)
        private val DOGE_ACCOUNT_XPUB = BTC_ACCOUNT_XPUB

        private fun createTestXpub(publicKey: String, chainCode: String): ExtendedPublicKey {
            return ExtendedPublicKey(
                publicKey = publicKey.hexToByteArray(),
                chainCode = chainCode.hexToByteArray(),
                depth = 3,
                parentFingerprint = byteArrayOf(0x00, 0x00, 0x00, 0x00),
                childNumber = 0L,
            )
        }

        private fun String.hexToByteArray(): ByteArray {
            check(length % 2 == 0) { "Hex string must have even length" }
            return chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        }
    }
}