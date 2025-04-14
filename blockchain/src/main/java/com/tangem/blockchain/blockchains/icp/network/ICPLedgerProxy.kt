package com.tangem.blockchain.blockchains.icp.network

import org.ic4j.agent.annotations.*
import org.ic4j.candid.annotations.Name
import org.ic4j.candid.types.Type
import java.util.concurrent.CompletableFuture

interface ICPLedgerProxy {
    @QUERY
    @Name("account_balance")
    fun getBalance(
        @Argument(Type.RECORD)
        balanceRequest: ICPBalanceRequest,
    ): ICPAmount

    @UPDATE
    @Name("transfer")
    @Waiter(timeout = 10, sleep = 1)
    fun transfer(
        @Argument(Type.RECORD)
        transferRequest: ICPTransferRequest,
    ): CompletableFuture<ICPTransferResponse>
}