package com.tangem.blockchain.blockchains.bitcoin

import com.tangem.blockchain.blockchains.dash.DashMainNetParams
import com.tangem.blockchain.blockchains.ducatus.DucatusMainNetParams
import com.tangem.blockchain.blockchains.ravencoin.RavencoinMainNetParams
import com.tangem.blockchain.blockchains.ravencoin.RavencoinTestNetParams
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.address.*
import com.tangem.common.card.EllipticCurve
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
import org.libdohj.params.DogecoinMainNetParams
import org.libdohj.params.LitecoinMainNetParams

open class BitcoinAddressService(
    blockchain: Blockchain,
) : AddressService, BitcoinScriptAddressesProvider {

    private val networkParameters: NetworkParameters = when (blockchain) {
        Blockchain.Bitcoin -> MainNetParams()
        Blockchain.BitcoinTestnet -> TestNet3Params()
        Blockchain.Litecoin -> LitecoinMainNetParams()
        Blockchain.Dogecoin -> DogecoinMainNetParams()
        Blockchain.Ducatus -> DucatusMainNetParams()
        Blockchain.Dash -> DashMainNetParams()
        Blockchain.Ravencoin -> RavencoinMainNetParams()
        Blockchain.RavencoinTestnet -> RavencoinTestNetParams()
        else -> throw IllegalStateException(
            "${blockchain.fullName} blockchain is not supported by ${this::class.simpleName}"
        )
    }

    val legacy = BitcoinLegacyAddressService(blockchain, networkParameters)
    val bech32 = BitcoinBech32AddressService(blockchain, networkParameters)

    // TODO refactoring look at default address type, probably return legacy address
    override fun makeAddress(
        publicKey: Wallet.PublicKey,
        addressType: AddressType,
        curve: EllipticCurve,
    ): PlainAddress {
        val address = when (addressType) {
            AddressType.Legacy -> legacy.makeAddressOldStyle(publicKey.blockchainKey).value
            AddressType.Default -> bech32.makeAddressOldStyle(publicKey.blockchainKey).value
        }

        return PlainAddress(
            value = address,
            type = addressType,
            publicKey = publicKey
        )
    }

    override fun validate(address: String): Boolean {
        return legacy.validate(address) || bech32.validate(address)
    }

    // override fun makeAddresses(walletPublicKey: ByteArray, curve: EllipticCurve?): Set<Address> {
    //     return when (blockchain) {
    //         Blockchain.Bitcoin, Blockchain.BitcoinTestnet, Blockchain.Litecoin -> {
    //             setOf(makeLegacyAddress(walletPublicKey), makeSegwitAddress(walletPublicKey))
    //         }
    //         else -> {
    //             super.makeAddresses(walletPublicKey, curve)
    //         }
    //     }
    // }

    override fun makeAddresses(
        publicKey: Wallet.PublicKey,
        pairPublicKey: ByteArray,
    ): List<BitcoinScriptAddress> {

        val script = create1of2MultisigOutputScript(publicKey.blockchainKey, pairPublicKey)

        val legacyAddressString = legacy.makeScriptAddress(script)
        val legacyAddress = BitcoinScriptAddress(
            script = script,
            value = legacyAddressString,
            type = AddressType.Legacy,
            publicKey = publicKey
        )

        val segwitAddressString = bech32.makeScriptAddress(script)
        val segwitAddress = BitcoinScriptAddress(
            script = script,
            value = segwitAddressString,
            type = AddressType.Default,
            publicKey = publicKey
        )

        return listOf(legacyAddress, segwitAddress)
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

data class BitcoinScriptAddress(
    val script: Script,
    override val value: String,
    override val type: AddressType = AddressType.Default,
    override val publicKey: Wallet.PublicKey,
) : Address(value, type, publicKey)