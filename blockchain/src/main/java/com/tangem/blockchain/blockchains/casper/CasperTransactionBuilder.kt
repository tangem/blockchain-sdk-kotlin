package com.tangem.blockchain.blockchains.casper

import com.tangem.blockchain.common.Wallet

/**
 * Casper transaction builder
 *
 * @property wallet wallet
 *
 */
@Suppress("UnusedPrivateMember")
internal class CasperTransactionBuilder(private val wallet: Wallet) {

    var minReserve = 2.5.toBigDecimal()
}