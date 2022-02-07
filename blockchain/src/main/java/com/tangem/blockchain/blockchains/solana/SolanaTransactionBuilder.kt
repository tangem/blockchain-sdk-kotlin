package com.tangem.blockchain.blockchains.solana

import com.tangem.blockchain.blockchains.solana.solanaj.core.Transaction
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.TransactionData
import org.p2p.solanaj.core.PublicKey
import org.p2p.solanaj.programs.SystemProgram
import java.math.BigDecimal

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

private fun BigDecimal.toLamports(): Long = movePointRight(Blockchain.Solana.decimals()).toSolanaDecimals().toLong()