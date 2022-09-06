package com.tangem.blockchain.blockchains.polkadot

import java.math.BigDecimal

/**
[REDACTED_AUTHOR]
 *
 * Networks with Existential Deposit must implement it. If the account balance drops below the
 * existential deposit value, it will be deactivated and any remaining funds will be destroyed.
 */
interface ExistentialDepositProvider {
    fun getExistentialDeposit(): BigDecimal
}