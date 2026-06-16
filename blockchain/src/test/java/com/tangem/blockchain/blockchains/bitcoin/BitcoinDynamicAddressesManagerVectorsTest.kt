package com.tangem.blockchain.blockchains.bitcoin

import com.google.common.truth.Truth.assertWithMessage
import com.tangem.blockchain.common.Blockchain
import com.tangem.crypto.CryptoUtils
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import org.junit.Before
import org.junit.Test

/**
 * Known-vector tests for [BitcoinDynamicAddressesManager] address derivation.
 *
 * The account key is the BIP84 reference account xpub (m/84'/0'/0' of the mnemonic
 * "abandon abandon ... about"), published in
 * https://github.com/bitcoin/bips/blob/master/bip-0084.mediawiki#test-vectors:
 * zpub6rFR7y4Q2AijBEqTUquhVz398htDFrtymD9xYYfG1m4wAcvPhXNfE3EfH1r1ADqtfSdVCToUG868RvUUkgDKf31mGDtKsAYz2oz2AGutZYs
 *
 * Bitcoin mainnet addresses at 0/0, 0/1 and 1/0 are the vectors published in BIP84 itself.
 * All remaining expected values were produced by an independent BIP32/bech32/base58check/cashaddr
 * implementation from the same account key — NOT by the code under test — so a systematic
 * derivation or encoding bug here cannot self-confirm.
 */
class BitcoinDynamicAddressesManagerVectorsTest {

    @Before
    fun setup() {
        CryptoUtils.initCrypto()
    }

    @Test
    fun deriveAddress_bitcoin_matchesBip84Vectors() {
        assertVectors(
            VectorCase(
                blockchain = Blockchain.Bitcoin,
                chain = 0,
                index = 0,
                expectedAddress = "bc1qcr8te4kr609gcawutmrza0j4xv80jy8z306fyu",
                expectedPublicKey = "0330d54fd0dd420a6e5f8d3624f5f3482cae350f79d5f0753bf5beef9c2d91af3c",
            ),
            VectorCase(
                blockchain = Blockchain.Bitcoin,
                chain = 0,
                index = 1,
                expectedAddress = "bc1qnjg0jd8228aq7egyzacy8cys3knf9xvrerkf9g",
                expectedPublicKey = "03e775fd51f0dfb8cd865d9ff1cca2a158cf651fe997fdc9fee9c1d3b5e995ea77",
            ),
            VectorCase(
                blockchain = Blockchain.Bitcoin,
                chain = 0,
                index = 5,
                expectedAddress = "bc1qnpzzqjzet8gd5gl8l6gzhuc4s9xv0djt0rlu7a",
                expectedPublicKey = "0284ae0efbe5cb35b24036d48d49d3c55a84fa75552bd162fc3a2a55e2997dd459",
            ),
            VectorCase(
                blockchain = Blockchain.Bitcoin,
                chain = 0,
                index = 100,
                expectedAddress = "bc1q2m7xsl8hf256as3s6e0pvcgz5n5a0de4ey30rl",
                expectedPublicKey = "03ba72a88f6a7393a0bc6a8cbd8255a10c9b6b709d1160a4b30808dbc0eed2007f",
            ),
            VectorCase(
                blockchain = Blockchain.Bitcoin,
                chain = 1,
                index = 0,
                expectedAddress = "bc1q8c6fshw2dlwun7ekn9qwf37cu2rn755upcp6el",
                expectedPublicKey = "03025324888e429ab8e3dbaf1f7802648b9cd01e9b418485c5fa4c1b9b5700e1a6",
            ),
            VectorCase(
                blockchain = Blockchain.Bitcoin,
                chain = 1,
                index = 1,
                expectedAddress = "bc1qggnasd834t54yulsep6fta8lpjekv4zj6gv5rf",
                expectedPublicKey = "03dcf71df71c755b3af46f7e84b4182e6291cec5a8c630f775638a739da29adcb6",
            ),
        )
    }

    @Test
    fun deriveAddress_bitcoinTestnet_matchesKnownVectors() {
        assertVectors(
            VectorCase(
                blockchain = Blockchain.BitcoinTestnet,
                chain = 0,
                index = 0,
                expectedAddress = "tb1qcr8te4kr609gcawutmrza0j4xv80jy8zmfp6l0",
                expectedPublicKey = "0330d54fd0dd420a6e5f8d3624f5f3482cae350f79d5f0753bf5beef9c2d91af3c",
            ),
            VectorCase(
                blockchain = Blockchain.BitcoinTestnet,
                chain = 1,
                index = 0,
                expectedAddress = "tb1q8c6fshw2dlwun7ekn9qwf37cu2rn755ut76fzv",
                expectedPublicKey = "03025324888e429ab8e3dbaf1f7802648b9cd01e9b418485c5fa4c1b9b5700e1a6",
            ),
        )
    }

    @Test
    fun deriveAddress_litecoin_matchesKnownVectors() {
        assertVectors(
            VectorCase(
                blockchain = Blockchain.Litecoin,
                chain = 0,
                index = 0,
                expectedAddress = "ltc1qcr8te4kr609gcawutmrza0j4xv80jy8z4nqduv",
                expectedPublicKey = "0330d54fd0dd420a6e5f8d3624f5f3482cae350f79d5f0753bf5beef9c2d91af3c",
            ),
            VectorCase(
                blockchain = Blockchain.Litecoin,
                chain = 0,
                index = 1,
                expectedAddress = "ltc1qnjg0jd8228aq7egyzacy8cys3knf9xvralvdac",
                expectedPublicKey = "03e775fd51f0dfb8cd865d9ff1cca2a158cf651fe997fdc9fee9c1d3b5e995ea77",
            ),
            VectorCase(
                blockchain = Blockchain.Litecoin,
                chain = 1,
                index = 0,
                expectedAddress = "ltc1q8c6fshw2dlwun7ekn9qwf37cu2rn755u9ym7p0",
                expectedPublicKey = "03025324888e429ab8e3dbaf1f7802648b9cd01e9b418485c5fa4c1b9b5700e1a6",
            ),
        )
    }

    @Test
    fun deriveAddress_dogecoin_matchesKnownVectors() {
        assertVectors(
            VectorCase(
                blockchain = Blockchain.Dogecoin,
                chain = 0,
                index = 0,
                expectedAddress = "DNiZwUS1j3bwusgPLrk6CPjzKUoZuYsVYq",
                expectedPublicKey = "0330d54fd0dd420a6e5f8d3624f5f3482cae350f79d5f0753bf5beef9c2d91af3c",
            ),
            VectorCase(
                blockchain = Blockchain.Dogecoin,
                chain = 0,
                index = 1,
                expectedAddress = "DKQwd7jGrh8GMk7xFRuBvcVVfX21CE9JQ1",
                expectedPublicKey = "03e775fd51f0dfb8cd865d9ff1cca2a158cf651fe997fdc9fee9c1d3b5e995ea77",
            ),
            VectorCase(
                blockchain = Blockchain.Dogecoin,
                chain = 1,
                index = 0,
                expectedAddress = "DAp1SXeQHMjSMBHmvYhc9eZe5BbMzfFgk1",
                expectedPublicKey = "03025324888e429ab8e3dbaf1f7802648b9cd01e9b418485c5fa4c1b9b5700e1a6",
            ),
        )
    }

    @Test
    fun deriveAddress_dash_matchesKnownVectors() {
        assertVectors(
            VectorCase(
                blockchain = Blockchain.Dash,
                chain = 0,
                index = 0,
                expectedAddress = "XtGKEU9GPLvFXp6NUA4kWAGBGgexe96hBy",
                expectedPublicKey = "0330d54fd0dd420a6e5f8d3624f5f3482cae350f79d5f0753bf5beef9c2d91af3c",
            ),
            VectorCase(
                blockchain = Blockchain.Dash,
                chain = 1,
                index = 0,
                expectedAddress = "XgMkjXMewf3jy7hm3r2GTR5q2PSkiCDJHD",
                expectedPublicKey = "03025324888e429ab8e3dbaf1f7802648b9cd01e9b418485c5fa4c1b9b5700e1a6",
            ),
        )
    }

    @Test
    fun deriveAddress_ravencoin_matchesKnownVectors() {
        assertVectors(
            VectorCase(
                blockchain = Blockchain.Ravencoin,
                chain = 0,
                index = 0,
                expectedAddress = "RSrfUjNf2TWESsrz5Sjek9ubCcXsByMC4J",
                expectedPublicKey = "0330d54fd0dd420a6e5f8d3624f5f3482cae350f79d5f0753bf5beef9c2d91af3c",
            ),
            VectorCase(
                blockchain = Blockchain.Ravencoin,
                chain = 1,
                index = 0,
                expectedAddress = "REx6ynb3amditBUNf8hAhQjExKKfNSkc6g",
                expectedPublicKey = "03025324888e429ab8e3dbaf1f7802648b9cd01e9b418485c5fa4c1b9b5700e1a6",
            ),
            VectorCase(
                blockchain = Blockchain.RavencoinTestnet,
                chain = 0,
                index = 0,
                expectedAddress = "my6RhGaMEf8v9yyQKqiuUYniJLfyU4gzqe",
                expectedPublicKey = "0330d54fd0dd420a6e5f8d3624f5f3482cae350f79d5f0753bf5beef9c2d91af3c",
            ),
        )
    }

    @Test
    fun deriveAddress_bitcoinCash_matchesKnownVectors() {
        assertVectors(
            VectorCase(
                blockchain = Blockchain.BitcoinCash,
                chain = 0,
                index = 0,
                expectedAddress = "bitcoincash:qrqva0xkc0fu4rr4m30vvt4725esa7gsug52ypqews",
                expectedPublicKey = "0330d54fd0dd420a6e5f8d3624f5f3482cae350f79d5f0753bf5beef9c2d91af3c",
            ),
            VectorCase(
                blockchain = Blockchain.BitcoinCash,
                chain = 0,
                index = 1,
                expectedAddress = "bitcoincash:qzwfp7f5afgl5rm9qsthqslqjzx6dy5esvjf289d62",
                expectedPublicKey = "03e775fd51f0dfb8cd865d9ff1cca2a158cf651fe997fdc9fee9c1d3b5e995ea77",
            ),
            VectorCase(
                blockchain = Blockchain.BitcoinCash,
                chain = 1,
                index = 0,
                expectedAddress = "bitcoincash:qqlrfxzaefhamj0mx6v5pex8mr3gw06jns8cxcmhjt",
                expectedPublicKey = "03025324888e429ab8e3dbaf1f7802648b9cd01e9b418485c5fa4c1b9b5700e1a6",
            ),
            VectorCase(
                blockchain = Blockchain.BitcoinCashTestnet,
                chain = 0,
                index = 0,
                expectedAddress = "bchtest:qrqva0xkc0fu4rr4m30vvt4725esa7gsugscqxzwfv",
                expectedPublicKey = "0330d54fd0dd420a6e5f8d3624f5f3482cae350f79d5f0753bf5beef9c2d91af3c",
            ),
        )
    }

    private fun assertVectors(vararg cases: VectorCase) {
        cases.forEach { case ->
            val manager = BitcoinDynamicAddressesManager(BIP84_ACCOUNT_XPUB, case.blockchain)

            val derived = manager.deriveAddress(chain = case.chain, index = case.index)

            assertWithMessage("${case.blockchain} ${case.chain}/${case.index} address")
                .that(derived.address)
                .isEqualTo(case.expectedAddress)
            assertWithMessage("${case.blockchain} ${case.chain}/${case.index} public key")
                .that(derived.publicKey.toHexString())
                .isEqualTo(case.expectedPublicKey)
            assertWithMessage("${case.blockchain} ${case.chain}/${case.index} chain")
                .that(derived.chain)
                .isEqualTo(case.chain)
            assertWithMessage("${case.blockchain} ${case.chain}/${case.index} index")
                .that(derived.index)
                .isEqualTo(case.index)
        }
    }

    private data class VectorCase(
        val blockchain: Blockchain,
        val chain: Int,
        val index: Int,
        val expectedAddress: String,
        val expectedPublicKey: String,
    )

    private companion object {
        /**
         * BIP84 reference account key (m/84'/0'/0'), decoded from the zpub in the class KDoc
         * (depth=3, parentFingerprint=7ef32bdb, childNumber=0x80000000 — account 0', hardened).
         */
        private val BIP84_ACCOUNT_XPUB = ExtendedPublicKey(
            publicKey = "02707a62fdacc26ea9b63b1c197906f56ee0180d0bcf1966e1a2da34f5f3a09a9b".hexToByteArray(),
            chainCode = "4a53a0ab21b9dc95869c4e92a161194e03c0ef3ff5014ac692f433c4765490fc".hexToByteArray(),
            depth = 3,
            parentFingerprint = "7ef32bdb".hexToByteArray(),
            childNumber = 0x80000000L,
        )

        private fun String.hexToByteArray(): ByteArray {
            check(length % 2 == 0) { "Hex string must have even length" }
            return chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        }

        private fun ByteArray.toHexString(): String = joinToString("") { "%02x".format(it) }
    }
}