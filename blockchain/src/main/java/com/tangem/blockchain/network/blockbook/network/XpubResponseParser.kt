package com.tangem.blockchain.network.blockbook.network

import com.tangem.blockchain.blockchains.bitcoin.BitcoinUnspentOutput
import com.tangem.blockchain.blockchains.bitcoin.network.UsedAddress
import com.tangem.blockchain.blockchains.bitcoin.network.XpubInfoResponse
import com.tangem.blockchain.blockchains.bitcoincash.BitcoinCashAddressService
import com.tangem.blockchain.blockchains.clore.CloreMainNetParams
import com.tangem.blockchain.blockchains.dash.DashMainNetParams
import com.tangem.blockchain.blockchains.ducatus.DucatusMainNetParams
import com.tangem.blockchain.blockchains.ravencoin.RavencoinMainNetParams
import com.tangem.blockchain.blockchains.ravencoin.RavencoinTestNetParams
import com.tangem.blockchain.common.BasicTransactionData
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.extensions.toBigDecimalOrDefault
import com.tangem.blockchain.network.blockbook.network.responses.GetAddressResponse
import com.tangem.blockchain.network.blockbook.network.responses.GetUtxoResponseItem
import com.tangem.blockchain.network.blockbook.network.responses.GetXpubResponse
import com.tangem.common.extensions.hexToBytes
import org.bitcoinj.core.Address
import org.bitcoinj.core.LegacyAddress
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.params.TestNet3Params
import org.bitcoinj.script.ScriptBuilder
import org.libdohj.params.DogecoinMainNetParams
import org.libdohj.params.LitecoinMainNetParams
import java.math.BigDecimal
import java.util.Calendar

internal class XpubResponseParser(private val blockchain: Blockchain) {

    private val networkParameters: NetworkParameters = when (blockchain) {
        Blockchain.Bitcoin, Blockchain.BitcoinCash -> MainNetParams()
        Blockchain.BitcoinTestnet, Blockchain.BitcoinCashTestnet -> TestNet3Params()
        Blockchain.Litecoin -> LitecoinMainNetParams()
        Blockchain.Dogecoin -> DogecoinMainNetParams()
        Blockchain.Ducatus -> DucatusMainNetParams()
        Blockchain.Dash -> DashMainNetParams()
        Blockchain.Ravencoin -> RavencoinMainNetParams()
        Blockchain.RavencoinTestnet -> RavencoinTestNetParams()
        Blockchain.Clore -> CloreMainNetParams()
        else -> error("${blockchain.fullName} blockchain is not supported by ${this::class.simpleName}")
    }

    @Suppress("UnnecessaryParentheses")
    fun parseXpubInfo(xpubResponse: GetXpubResponse, xpubUtxoItems: List<GetUtxoResponseItem>): XpubInfoResponse {
        val ownAddresses = xpubResponse.tokens
            ?.map { it.name }
            ?.toSet()
            .orEmpty()

        val balance = xpubResponse.balance.toBigDecimalOrNull() ?: BigDecimal.ZERO

        val confirmedUtxoItems = xpubUtxoItems.filter { it.confirmations > 0 }
        val allUtxoAddresses = xpubUtxoItems.mapNotNull { item ->
            val utxoAddress = item.address ?: return@mapNotNull null
            val utxoDerivationPath = item.path ?: return@mapNotNull null
            val utxoBalance = item.value.toBigDecimalOrNull()
                ?.movePointLeft(blockchain.decimals()) ?: BigDecimal.ZERO
            UsedAddress(address = utxoAddress, derivationPath = utxoDerivationPath, balance = utxoBalance)
        }

        val xpubUsedAddresses = createUsedAddresses(xpubResponse)
        val mergedUsedAddresses = mergeUsedAddresses(xpubUsedAddresses, allUtxoAddresses)

        return XpubInfoResponse(
            balance = balance.movePointLeft(blockchain.decimals()),
            unspentOutputs = parseXpubUtxo(confirmedUtxoItems),
            usedAddresses = mergedUsedAddresses,
            hasUnconfirmed = (xpubResponse.unconfirmedTxs ?: 0) != 0,
            recentTransactions = createXpubUnconfirmedTransactions(
                xpubResponse = xpubResponse,
                ownAddresses = ownAddresses,
            ),
        )
    }

    fun parseXpubUtxo(items: List<GetUtxoResponseItem>): List<BitcoinUnspentOutput> {
        val scriptCache = mutableMapOf<String, ByteArray>()
        return items.mapNotNull { item ->
            val amount = item.value.toBigDecimalOrNull()?.movePointLeft(blockchain.decimals())
                ?: return@mapNotNull null
            val address = item.address ?: return@mapNotNull null

            val outputScript = scriptCache.getOrPut(address) {
                ScriptBuilder.createOutputScript(resolveAddress(address)).program
            }

            BitcoinUnspentOutput(
                amount = amount,
                outputIndex = item.vout.toLong(),
                transactionHash = item.txid.hexToBytes(),
                outputScript = outputScript,
                address = address,
                derivationPath = item.path,
            )
        }
    }

    private fun createUsedAddresses(xpubResponse: GetXpubResponse): List<UsedAddress> {
        return xpubResponse.tokens?.map { token ->
            UsedAddress(
                address = token.name,
                derivationPath = token.path,
                balance = (token.balance?.toBigDecimalOrNull() ?: BigDecimal.ZERO)
                    .movePointLeft(blockchain.decimals()),
            )
        }.orEmpty()
    }

    private fun mergeUsedAddresses(
        xpubAddresses: List<UsedAddress>,
        utxoAddresses: List<UsedAddress>,
    ): List<UsedAddress> {
        val byAddress = linkedMapOf<String, UsedAddress>()
        xpubAddresses.forEach { byAddress[it.address] = it }
        utxoAddresses.forEach { addr ->
            byAddress.putIfAbsent(addr.address, addr)
        }
        return byAddress.values.toList()
    }

    private fun createXpubUnconfirmedTransactions(
        xpubResponse: GetXpubResponse,
        ownAddresses: Set<String>,
    ): List<BasicTransactionData> {
        return xpubResponse.transactions
            ?.filter { it.confirmations == 0 }
            ?.map { tx ->
                val areAllVinOurs = tx.vin.all { vin ->
                    vin.isOwn == true || vin.addresses?.any { it in ownAddresses } == true
                }
                val isIncoming = !areAllVinOurs

                val source: String
                val destination: String
                val amount: BigDecimal

                if (isIncoming) {
                    source = tx.vin.firstOrNull()?.addresses?.firstOrNull() ?: "unknown"
                    val ownVouts = tx.vout.filter { vout -> isOwnVout(vout, ownAddresses) }
                    destination = ownVouts
                        .flatMap { it.addresses.orEmpty() }
                        .firstOrNull { it in ownAddresses }
                        ?: "unknown"
                    amount = ownVouts
                        .sumOf { it.value.toBigDecimalOrDefault() }
                        .movePointLeft(blockchain.decimals())
                } else {
                    source = ownAddresses.firstOrNull() ?: "unknown"
                    val externalVouts = tx.vout.filter { vout -> isExternalVout(vout, ownAddresses) }
                    destination = externalVouts.firstOrNull()?.addresses?.firstOrNull() ?: "unknown"
                    amount = externalVouts
                        .sumOf { it.value.toBigDecimalOrDefault() }
                        .movePointLeft(blockchain.decimals())
                }

                BasicTransactionData(
                    balanceDif = if (isIncoming) amount else amount.negate(),
                    hash = tx.txid,
                    date = Calendar.getInstance().apply {
                        timeInMillis = tx.blockTime.toLong() * MILLIS_IN_SECOND
                    },
                    isConfirmed = false,
                    destination = destination,
                    source = source,
                )
            }
            .orEmpty()
    }

    private fun isOwnVout(vout: GetAddressResponse.Transaction.Vout, ownAddresses: Set<String>): Boolean {
        if (vout.isOwn == true) return true
        return vout.addresses?.any { it in ownAddresses } == true
    }

    private fun isExternalVout(vout: GetAddressResponse.Transaction.Vout, ownAddresses: Set<String>): Boolean {
        return !isOwnVout(vout, ownAddresses)
    }

    private fun resolveAddress(address: String): Address {
        return when (blockchain) {
            Blockchain.BitcoinCash, Blockchain.BitcoinCashTestnet -> {
                val addressService = BitcoinCashAddressService(blockchain)
                LegacyAddress.fromPubKeyHash(networkParameters, addressService.getPublicKeyHash(address))
            }
            else -> Address.fromString(networkParameters, address)
        }
    }

    private companion object {
        const val MILLIS_IN_SECOND = 1000L
    }
}