package com.tangem.blockchain.blockchains.decimal

import com.tangem.blockchain.blockchains.ethereum.network.EthereumInfoResponse
import com.tangem.blockchain.blockchains.ethereum.network.EthereumJsonRpcProvider
import com.tangem.blockchain.blockchains.ethereum.network.EthereumNetworkService
import com.tangem.blockchain.blockchains.ethereum.network.EthereumTxCountInfo
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.network.blockchair.BlockchairToken
import java.math.BigDecimal
import java.math.BigInteger

class DecimalNetworkService(
    jsonRpcProviders: List<EthereumJsonRpcProvider>,
) : EthereumNetworkService(jsonRpcProviders, blockcypherNetworkProvider = null, blockchairEthNetworkProvider = null) {

    override suspend fun getInfo(address: String, tokens: Set<Token>): Result<EthereumInfoResponse> {
        return super.getInfo(convertAddress(address), tokens)
    }

    override suspend fun getTxCountInfo(address: String): Result<EthereumTxCountInfo> {
        return super.getTxCountInfo(convertAddress(address))
    }

    override suspend fun getAllowance(ownerAddress: String, token: Token, spenderAddress: String): Result<BigDecimal> {
        return super.getAllowance(convertAddress(ownerAddress), token, spenderAddress)
    }

    override suspend fun getSignatureCount(address: String): Result<Int> {
        return super.getSignatureCount(convertAddress(address))
    }

    override suspend fun getTokensBalance(address: String, tokens: Set<Token>): Result<Map<Token, BigDecimal>> {
        return super.getTokensBalance(convertAddress(address), tokens)
    }

    override suspend fun findErc20Tokens(address: String): Result<List<BlockchairToken>> {
        return super.findErc20Tokens(convertAddress(address))
    }

    override suspend fun getGasLimit(to: String, from: String, value: String?, data: String?): Result<BigInteger> {
        return super.getGasLimit(convertAddress(to), convertAddress(from), value, data)
    }

    private fun convertAddress(address: String): String {
        return DecimalAddressService.convertDelAddressToDscAddress(address)
    }
}