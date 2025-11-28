package com.tangem.blockchain.blockchains.quai

import com.tangem.blockchain.blockchains.ethereum.EthereumAddressService
import com.tangem.blockchain.blockchains.ethereum.EthereumDerivationData
import com.tangem.blockchain.common.logging.Logger
import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.hexToBytes
import com.tangem.crypto.hdWallet.DerivationNode
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey

/**
 * Address service for Quai Network with Cyprus-1 zone validation
 *
 * Quai Network uses EVM-compatible addresses but with specific validation rules:
 * - First byte must be 0x00 (Cyprus-1 zone)
 * - 9th bit must be 0
 *
 * This requires iterative derivation on last node to find a valid address.
 *
 */
internal class QuaiAddressService : EthereumAddressService() {

    override fun makeAddressFromExtendedPublicKey(
        extendedPublicKey: ExtendedPublicKey,
        curve: EllipticCurve?,
        derivationPath: String?,
        cachedIndex: Int?,
    ): EthereumDerivationData {
        return findValidCyprus1AddressWithHDDerivation(
            extendedPublicKey = extendedPublicKey,
            curve = curve,
            derivationPath = derivationPath,
            cachedIndex = cachedIndex,
        )
    }

    override fun validate(address: String): Boolean {
        if (!super.validate(address)) {
            return false
        }
        return validateCyprus1Address(address)
    }

    /**
     * Validates that the address meets Cyprus-1 zone requirements:
     * - First byte must be 0x00 (region=0, zone=0)
     * - 9th bit must be 0
     */
    private fun validateCyprus1Address(address: String): Boolean {
        return try {
            val addressBytes = address.removePrefix("0x").hexToBytes()
            if (addressBytes.size < 2) {
                Logger.logNetwork("${this::class.java.simpleName} Address too short: ${addressBytes.size} bytes")
                return false
            }
            val firstByte = addressBytes[0]
            val secondByte = addressBytes[1]
            val hasCorrectFirstByte = firstByte == CYPRUS_1_FIRST_BYTE.toByte()
            val ninthBit = secondByte.toInt() and CYPRUS_1_NINTH_BIT_MASK == 0
            val isValid = hasCorrectFirstByte && ninthBit
            isValid
        } catch (e: Exception) {
            Logger.logNetwork("${this::class.java.simpleName} Error validating address $address: ${e.message}")
            false
        }
    }

    /**
     * Finds a valid Cyprus-1 address through proper HD derivation
     * Uses ExtendedPublicKey to derive child keys with different indices
     *
     * According to BIP44: m/44'/994'/0'/0/{index}
     * We have the base key for m/44'/994'/0'/0 and need to iterate through indices
     *
     * First tries cached index if available, then falls back to iterative search
     */
    private fun findValidCyprus1AddressWithHDDerivation(
        extendedPublicKey: ExtendedPublicKey,
        curve: EllipticCurve?,
        derivationPath: String?,
        cachedIndex: Int?,
    ): EthereumDerivationData {
        cachedIndex?.let { index ->
            tryCachedIndex(extendedPublicKey, curve, derivationPath, index)?.let { return it }
        }

        return findValidAddressByIteration(extendedPublicKey, curve, derivationPath)
    }

    /**
     * Attempts to use cached index to generate and validate address
     * @return EthereumDerivationData if cached index is valid, null otherwise
     */
    private fun tryCachedIndex(
        extendedPublicKey: ExtendedPublicKey,
        curve: EllipticCurve?,
        derivationPath: String?,
        cachedIndex: Int,
    ): EthereumDerivationData? {
        Logger.logNetwork("${this::class.java.simpleName} Trying cached index: $cachedIndex")
        return try {
            val derivationNode = DerivationNode.NonHardened(cachedIndex.toLong())
            val derivedKey = extendedPublicKey.derivePublicKey(derivationNode)
            val address = super.makeAddress(derivedKey.publicKey, curve)

            if (validateCyprus1Address(address)) {
                Logger.logNetwork(
                    "${this::class.java.simpleName} Cached index $cachedIndex is valid: $address",
                )
                val path = derivationPath?.let {
                    DerivationPath("$it/$cachedIndex")
                }
                EthereumDerivationData(
                    address = address,
                    path = path,
                    publicKey = derivedKey,
                    index = cachedIndex,
                )
            } else {
                Logger.logNetwork(
                    "${this::class.java.simpleName} Cached index $cachedIndex failed validation, fall back to search",
                )
                null
            }
        } catch (e: Exception) {
            Logger.logNetwork(
                "${this::class.java.simpleName} Error via cached index $cachedIndex: ${e.message}, fall back to search",
            )
            null
        }
    }

    /**
     * Iteratively searches for a valid Cyprus-1 address starting from index 0
     */
    private fun findValidAddressByIteration(
        extendedPublicKey: ExtendedPublicKey,
        curve: EllipticCurve?,
        derivationPath: String?,
    ): EthereumDerivationData {
        Logger.logNetwork("${this::class.java.simpleName} Starting iterative search for valid Cyprus-1 address")

        for (index in 0 until Int.MAX_VALUE) {
            try {
                val derivationNode = DerivationNode.NonHardened(index.toLong())
                val derivedKey = extendedPublicKey.derivePublicKey(derivationNode)
                val address = super.makeAddress(derivedKey.publicKey, curve)

                if (validateCyprus1Address(address)) {
                    Logger.logNetwork(
                        "${this::class.java.simpleName} Found valid Cyprus-1 address: $address (index: $index)",
                    )
                    val path = derivationPath?.let {
                        DerivationPath("$it/$index")
                    }
                    return EthereumDerivationData(
                        address = address,
                        path = path,
                        publicKey = derivedKey,
                        index = index,
                    )
                }
            } catch (e: Exception) {
                Logger.logNetwork(
                    "${this::class.java.simpleName} Error during HD derivation for index $index: ${e.message}",
                )
                continue
            }
        }

        Logger.logNetwork(
            "${this::class.java.simpleName} No Cyprus-1 address found after iterations! This should not happen.",
        )
        error("No valid Cyprus-1 address found for Quai Network after Int.MAX_INT iterations")
    }

    companion object {
        private const val CYPRUS_1_FIRST_BYTE = 0x00
        private const val CYPRUS_1_NINTH_BIT_MASK = 0x80
    }
}