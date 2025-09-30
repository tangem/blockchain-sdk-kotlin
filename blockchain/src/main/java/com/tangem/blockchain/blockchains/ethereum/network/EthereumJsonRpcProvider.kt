package com.tangem.blockchain.blockchains.ethereum.network

internal class EthereumJsonRpcProvider(
    baseUrl: String,
    postfixUrl: String = "",
    authToken: String? = null,
    nowNodesApiKey: String? = null,
) : EthereumLikeJsonRpcProvider(baseUrl, postfixUrl, authToken, nowNodesApiKey) {

    override fun getMethods(): EthereumLikeMethod = EthereumMethod
}

data class EthereumTokenBalanceRequestData(
    val address: String,
    val contractAddress: String,
)

data class EthereumTokenAllowanceRequestData(
    val ownerAddress: String,
    val contractAddress: String,
    val spenderAddress: String,
)

data class EthereumResolveENSNameRequestData(
    val contractAddress: String,
    val nameBytes: ByteArray,
    val callDataBytes: ByteArray,
)

data class EthereumReverseResolveENSAddressRequestData(
    val address: ByteArray,
    val contractAddress: String,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as EthereumReverseResolveENSAddressRequestData
        if (!address.contentEquals(other.address)) return false
        if (contractAddress != other.contractAddress) return false
        return true
    }

    override fun hashCode(): Int {
        var result = address.contentHashCode()
        result = 31 * result + contractAddress.hashCode()
        return result
    }
}