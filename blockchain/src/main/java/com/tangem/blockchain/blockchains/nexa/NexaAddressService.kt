package com.tangem.blockchain.blockchains.nexa

import com.tangem.blockchain.blockchains.nexa.cashaddr.NexaAddr
import com.tangem.blockchain.blockchains.nexa.cashaddr.NexaAddressType
import com.tangem.blockchain.common.address.Address
import com.tangem.blockchain.common.address.AddressService
import com.tangem.blockchain.common.address.AddressType
import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.calculateRipemd160
import com.tangem.common.extensions.calculateSha256
import com.tangem.common.extensions.toCompressedPublicKey
import com.tangem.common.extensions.toHexString
import org.bitcoinj.script.ScriptBuilder
import org.bitcoinj.script.ScriptChunk
import org.bitcoinj.script.ScriptOpCodes

class NexaAddressService(
    isTestNet: Boolean,
) : AddressService() {

    private val cashAddr = NexaAddr(isTestNet)

    override fun makeAddress(walletPublicKey: ByteArray, curve: EllipticCurve?) =
        makeTemplateNexaAddress(walletPublicKey).value

    override fun validate(address: String) = cashAddr.isValidCashAddress(address)

    fun getPublicKey(address: String): ByteArray {
        return cashAddr.decodeNexaAddress(address).hash
    }

    /**
     * @see <a href="https://explorer.nexa.org/">explorer</a>
     * @return script public key hash (as in the Explorer)
     */
    fun getScriptPublicKey(address: String): ByteArray {
        return cashAddr.decodeNexaAddress(address).scriptPublicKeyHash
    }

    private fun makeTemplateNexaAddress(walletPublicKey: ByteArray): Address {
        val scriptTemplate = scriptTemplate(walletPublicKey)

        val hash = ScriptBuilder()
            .data(scriptTemplate)
            .build().program

        val address = cashAddr.toNexaAddress(NexaAddressType.TEMPLATE, hash)

        return Address(address, AddressType.Default)
    }

    companion object {
        fun getScriptHash(walletPublicKey: ByteArray): String {
            return scriptTemplate(walletPublicKey).calculateSha256().reversedArray().toHexString()
        }

        private fun scriptTemplate(walletPublicKey: ByteArray): ByteArray {
            val publicKeyHash = walletPublicKey.toCompressedPublicKey()

            val constraint = ScriptBuilder()
                .data(publicKeyHash)
                .build().program

            val constraintHash = constraint.calculateSha256().calculateRipemd160()

            return ScriptBuilder()
                .addChunk(ScriptChunk(ScriptOpCodes.OP_FALSE, null))
                .addChunk(ScriptChunk(ScriptOpCodes.OP_1, null))
                .data(constraintHash)
                .build().program
        }
    }
}