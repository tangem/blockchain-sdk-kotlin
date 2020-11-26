package com.tangem.blockchain.blockchains.bitcoin


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
import org.bitcoinj.core.ECKey
import org.bitcoinj.core.LegacyAddress
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.core.SegwitAddress
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
        val ecPublicKey = ECKey.fromPublicOnly(walletPublicKey)
        return LegacyAddress.fromKey(networkParameters, ecPublicKey).toBase58()
    }

    override fun validate(address: String): Boolean {
        return validateLegacyAddress(address) || validateSegwitAddress(address)
    }

    override fun makeAddresses(walletPublicKey: ByteArray): Set<Address> {
        return when (blockchain) {
            Blockchain.Bitcoin, Blockchain.BitcoinTestnet -> {
                setOf(makeLegacyAddress(walletPublicKey), makeSegwitAddress(walletPublicKey))
            }
            else -> {
                super.makeAddresses(walletPublicKey)
            }
        }
    }

    private fun makeLegacyAddress(walletPublicKey: ByteArray) =
            Address(makeAddress(walletPublicKey), BitcoinAddressType.Legacy)

    private fun makeSegwitAddress(walletPublicKey: ByteArray): Address {
        val compressedPublicKey = ECKey.fromPublicOnly(walletPublicKey.toCompressedPublicKey())
        val address = SegwitAddress.fromKey(networkParameters, compressedPublicKey).toBech32()
        return Address(address, BitcoinAddressType.Segwit)
    }

    private fun validateSegwitAddress(address: String): Boolean {
        return try {
            if (blockchain == Blockchain.Ducatus) return false
            SegwitAddress.fromBech32(networkParameters, address)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun validateLegacyAddress(address: String): Boolean {
        return try {
            LegacyAddress.fromBase58(networkParameters, address)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun make1of2MultisigAddresses(publicKey1: ByteArray, publicKey2: ByteArray): Set<Address> {
        val script = create1of2MultisigOutputScript(publicKey1, publicKey2)
        val legacyAddress = makeLegacyScriptAddress(script)
        val segwitAddress = makeSegwitScriptAddress(script)
        return setOf(legacyAddress, segwitAddress)
    }

    private fun create1of2MultisigOutputScript(publicKey1: ByteArray, publicKey2: ByteArray): Script {
        val publicKeys = mutableListOf(
                publicKey1.toCompressedPublicKey(),
                publicKey2.toCompressedPublicKey()
        )
        val publicEcKeys = publicKeys.map { ECKey.fromPublicOnly(it) }
        return ScriptBuilder.createRedeemScript(1, publicEcKeys)
    }

    private fun makeLegacyScriptAddress(script: Script): BitcoinScriptAddress {
        val scriptHash = script.program.calculateSha256().calculateRipemd160()
        val address = LegacyAddress.fromScriptHash(networkParameters, scriptHash)
        return BitcoinScriptAddress(script, address.toBase58(), BitcoinAddressType.Legacy)
    }

    private fun makeSegwitScriptAddress(script: Script): BitcoinScriptAddress {
        val scriptHash = script.program.calculateSha256()
        val address = SegwitAddress.fromHash(networkParameters, scriptHash)
        return BitcoinScriptAddress(script, address.toBech32(), BitcoinAddressType.Segwit)
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