package com.tangem.blockchain.blockchains.ethereum.eip7702

import com.tangem.common.extensions.hexToBytes
import org.kethereum.keccakshortcut.keccak
import org.kethereum.rlp.RLPElement
import org.kethereum.rlp.RLPList
import org.kethereum.rlp.encode
import java.math.BigInteger

/**
 * Utility for working with EIP-7702 authorization data.
 * EIP-7702 introduces a new transaction type that allows an EOA to temporarily delegate
 * control to a smart contract during a transaction.
 */
object EthEip7702Util {

    private const val ADDRESS_SIZE_IN_BYTES = 20
    private const val MAGIC_BYTE: Byte = 0x05 // EIP-7702 transaction type

    /**
     * Encode EIP-7702 authorization data for signing.
     *
     * The authorization format is:
     * keccak256(MAGIC || rlp([chain_id, address, nonce]))
     *
     * @param chainId The chain ID.
     * @param contractAddress The address to authorize (20 bytes).
     * @param nonce The nonce for replay protection.
     * @return The hash to be signed.
     */
    fun encodeAuthorizationForSigning(chainId: Int, contractAddress: String, nonce: BigInteger): ByteArray {
        // Convert address to bytes
        val addressBytes = contractAddress.hexToBytes()

        require(addressBytes.size == ADDRESS_SIZE_IN_BYTES) { "Address must be 20 bytes" }

        // Encode the authorization data
        val encoded = encodeAuthorizationData(chainId, addressBytes, nonce)

        // Return keccak256 hash
        return encoded.keccak()
    }

    /**
     * Encode authorization data using RLP encoding.
     *
     * @param chainId The chain ID.
     * @param address The contract address (20 bytes).
     * @param nonce The nonce.
     * @return The RLP-encoded authorization data.
     */
    private fun encodeAuthorizationData(chainId: Int, address: ByteArray, nonce: BigInteger): ByteArray {
        val chainIdRlp = RLPElement(chainId.toBigInteger().toByteArray())
        val addressRlp = RLPElement(address)
        val nonceBytesRlp = RLPElement(nonce.toByteArray())

        // RLP encode: [chain_id, address, nonce]
        val rlpList = RLPList(listOf(chainIdRlp, addressRlp, nonceBytesRlp))

        // Prepend magic byte
        return byteArrayOf(MAGIC_BYTE) + rlpList.encode()
    }
}