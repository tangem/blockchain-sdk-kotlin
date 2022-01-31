package com.tangem.blockchain.blockchains.solana

import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.blockchains.solana.solanaj.core.Transaction
import org.p2p.solanaj.core.PublicKey
import org.p2p.solanaj.programs.SystemProgram

/**
[REDACTED_AUTHOR]
 */
class SolanaTransactionBuilder {

    fun buildToSign(transactionData: TransactionData, recentBlockhash: String): Transaction {
        val from = PublicKey(transactionData.sourceAddress)
        val to = PublicKey(transactionData.destinationAddress)
        val lamports = transactionData.amount.value!!.toLamports()

        val solanaTx = Transaction(from)
        solanaTx.addInstruction(SystemProgram.transfer(from, to, lamports))
        solanaTx.setRecentBlockHash(recentBlockhash)
        return solanaTx
    }
}