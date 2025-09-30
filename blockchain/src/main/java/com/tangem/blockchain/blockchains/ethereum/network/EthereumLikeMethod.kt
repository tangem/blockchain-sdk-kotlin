package com.tangem.blockchain.blockchains.ethereum.network

/**
 * Abstract interface for Ethereum-like RPC methods
 * Allows different blockchains to use their own method prefixes
 */
interface EthereumLikeMethod {
    val getBalance: String
    val getTransactionCount: String
    val call: String
    val sendRawTransaction: String
    val estimateGas: String
    val gasPrice: String
    val feeHistory: String
}

/**
 * Standard Ethereum RPC methods with eth_ prefix
 */
object EthereumMethod : EthereumLikeMethod {
    override val getBalance = "eth_getBalance"
    override val getTransactionCount = "eth_getTransactionCount"
    override val call = "eth_call"
    override val sendRawTransaction = "eth_sendRawTransaction"
    override val estimateGas = "eth_estimateGas"
    override val gasPrice = "eth_gasPrice"
    override val feeHistory = "eth_feeHistory"
}

/**
 * Quai Network RPC methods with quai_ prefix
 */
object QuaiMethod : EthereumLikeMethod {
    override val getBalance = "quai_getBalance"
    override val getTransactionCount = "quai_getTransactionCount"
    override val call = "quai_call"
    override val sendRawTransaction = "quai_sendRawTransaction"
    override val estimateGas = "quai_estimateGas"
    override val gasPrice = "quai_gasPrice"
    override val feeHistory = "quai_feeHistory"
}