package com.tangem.blockchain.blockchains.bitcoin


import com.google.common.primitives.UnsignedBytes
import com.tangem.blockchain.blockchains.ducatus.DucatusMainNetParams
import com.tangem.blockchain.blockchains.litecoin.LitecoinMainNetParams
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.address.Address
import com.tangem.blockchain.common.address.AddressService
import com.tangem.blockchain.common.address.AddressType
import com.tangem.blockchain.common.address.DefaultAddressType
import com.tangem.common.extensions.calculateRipemd160
import com.tangem.common.extensions.calculateSha256
import com.tangem.common.extensions.toCompressedPublicKey
import org.bitcoinj.core.*
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.params.TestNet3Params
import org.bitcoinj.script.Script
import org.bitcoinj.script.ScriptBuilder

open class BitcoinAddressService(private val blockchain: Blockchain) : AddressService() {

    private val networkParameters: NetworkParameters = when (blockchain) {
        Blockchain.Bitcoin -> MainNetParams()
        Blockchain.BitcoinTestnet -> TestNet3Params()
        Blockchain.Litecoin -> LitecoinMainNetParams()
        Blockchain.Ducatus -> DucatusMainNetParams()
        else -> throw Exception("${blockchain.fullName} blockchain is not supported by ${this::class.simpleName}")
    }

    override fun makeAddress(walletPublicKey: ByteArray): String {
        val publicKeyHash = walletPublicKey.calculateSha256().calculateRipemd160()
        val checksum = byteArrayOf(networkParameters.addressHeader.toByte()).plus(publicKeyHash)
                .calculateSha256().calculateSha256()
        val result = byteArrayOf(networkParameters.addressHeader.toByte()) + publicKeyHash + checksum.copyOfRange(0, 4)
        return Base58.encode(result)
    }

    override fun validate(address: String): Boolean {
        return validateLegacyAddress(address) || validateSegwitAddress(address)
    }

    private fun validateSegwitAddress(address: String): Boolean {
        return try {
            when (blockchain) {
                Blockchain.Bitcoin -> SegwitAddress.fromBech32(MainNetParams(), address)
                Blockchain.BitcoinTestnet -> SegwitAddress.fromBech32(TestNet3Params(), address)
                Blockchain.Litecoin -> SegwitAddress.fromBech32(LitecoinMainNetParams(), address)
                else -> return false
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun validateLegacyAddress(address: String): Boolean {
        return try {
            when (blockchain) {
                Blockchain.Bitcoin -> LegacyAddress.fromBase58(MainNetParams(), address)
                Blockchain.BitcoinTestnet -> LegacyAddress.fromBase58(TestNet3Params(), address)
                Blockchain.Litecoin -> LegacyAddress.fromBase58(LitecoinMainNetParams(), address)
                Blockchain.Ducatus -> LegacyAddress.fromBase58(DucatusMainNetParams(), address)
                else -> return false
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun make1of2MultisigAddresses(publicKey1: ByteArray, publicKey2: ByteArray): Set<Address> { //TODO: add segwit address
        val script = create1of2MultisigOutputScript(publicKey1, publicKey2)
        val scriptHash = script.program.calculateSha256().calculateRipemd160()
        val address = LegacyAddress.fromScriptHash(networkParameters, scriptHash)
        val scriptAddress = BitcoinScriptAddress(script, address.toBase58())
        return setOf(scriptAddress)
    }

    private fun create1of2MultisigOutputScript(publicKey1: ByteArray, publicKey2: ByteArray): Script {
        val publicKeys = mutableListOf(
                publicKey1.toCompressedPublicKey(),
                publicKey2.toCompressedPublicKey()
        )
        val publicEcKeys = publicKeys.map { ECKey.fromPublicOnly(it) }
        return ScriptBuilder.createRedeemScript(1, publicEcKeys)
    }
}

sealed class BitcoinAddressType : AddressType {
    object Legacy : AddressType {
        override val displayNameRes = 1 //TODO: change to string resource
    }

    object Segwit : AddressType {
        override val displayNameRes = 2 //TODO: change to string resource
    }
}

class BitcoinScriptAddress(
        val script: Script,
        value: String,
        type: AddressType = DefaultAddressType
) : Address(value, type)