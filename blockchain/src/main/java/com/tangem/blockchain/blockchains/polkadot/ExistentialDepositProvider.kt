package com.tangem.blockchain.blockchains.polkadot

import java.math.BigDecimal

/**
 * Created by Anton Zhilenkov on 06/09/2022.
 *
 * Networks with Existential Deposit must implement it. If the account balance drops below the
 * existential deposit value, it will be deactivated and any remaining funds will be destroyed.
 */
interface ExistentialDepositProvider {
    fun getExistentialDeposit(): BigDecimal
}
