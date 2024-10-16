package com.tangem.blockchain.blockchains.casper.network.converters

import com.tangem.blockchain.blockchains.casper.network.request.CasperQueryBalanceBody

/**
 * Converter from address to [CasperQueryBalanceBody]
 *
 */
internal object CasperBalanceBodyConverter {

    fun convert(address: String): CasperQueryBalanceBody {
        return CasperQueryBalanceBody(
            purseIdentifier = CasperQueryBalanceBody.PurseIdentifier(
                mainPurseUnderPublicKey = address,
            ),
        )
    }
}