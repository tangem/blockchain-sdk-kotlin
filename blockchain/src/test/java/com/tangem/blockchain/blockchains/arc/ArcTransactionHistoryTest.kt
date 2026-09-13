package com.tangem.blockchain.blockchains.arc

import com.google.common.truth.Truth
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.pagination.Page
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.transactionhistory.DefaultTransactionHistoryProvider
import com.tangem.blockchain.transactionhistory.TransactionHistoryProviderFactory
import com.tangem.blockchain.transactionhistory.blockchains.polygon.EtherscanTransactionHistoryProvider
import com.tangem.blockchain.transactionhistory.blockchains.polygon.network.EtherScanApi
import com.tangem.blockchain.transactionhistory.blockchains.polygon.network.PolygonScanResult
import com.tangem.blockchain.transactionhistory.blockchains.polygon.network.PolygonTransaction
import com.tangem.blockchain.transactionhistory.blockchains.polygon.network.PolygonTransactionHistoryResponse
import com.tangem.blockchain.transactionhistory.models.TransactionHistoryRequest
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.math.BigDecimal

internal class ArcTransactionHistoryTest {

    private val api = mockk<EtherScanApi>()
    private val provider = EtherscanTransactionHistoryProvider(
        blockchain = Blockchain.Arc,
        api = api,
        etherscanApiKey = "",
    )

    @Test
    fun `Arc transaction history is not supported`() {
        listOf(Blockchain.Arc, Blockchain.ArcTestnet).forEach { blockchain ->
            val provider = TransactionHistoryProviderFactory.makeProvider(
                blockchain = blockchain,
                config = BlockchainSdkConfig(),
            )

            Truth.assertThat(provider).isSameInstanceAs(DefaultTransactionHistoryProvider)
        }
    }

    @Test
    fun `Arc coin history amount and fee are scaled with 18 decimals`() = runTest {
        coEvery {
            api.getCoinTransactionHistory(chainId = any(), address = any(), page = any(), offset = any(), apiKey = any())
        } returns response(transaction(value = "2000000000000000000", gasPrice = "25000000000", gasUsed = "21000"))

        val result = provider.getTransactionsHistory(
            TransactionHistoryRequest(
                address = WALLET,
                decimals = Blockchain.Arc.decimals(),
                page = Page.Initial,
                pageSize = 20,
                filterType = TransactionHistoryRequest.FilterType.Coin,
            ),
        )

        val item = (result as Result.Success).data.items.single()
        val expectedAmount = Amount(value = BigDecimal("2.000000000000000000"), blockchain = Blockchain.Arc)
        val expectedFee = Amount(value = BigDecimal("0.000525000000000000"), blockchain = Blockchain.Arc)
        Truth.assertThat(item.amount).isEqualTo(expectedAmount)
        Truth.assertThat(item.fee).isEqualTo(expectedFee)
        Truth.assertThat(item.isOutgoing).isTrue()
    }

    private fun response(vararg transactions: PolygonTransaction) = PolygonTransactionHistoryResponse(
        status = "1",
        message = "OK",
        result = PolygonScanResult.Transactions(txs = transactions.toList()),
    )

    private fun transaction(value: String, gasPrice: String, gasUsed: String) = PolygonTransaction(
        confirmations = "10",
        contractAddress = "",
        from = WALLET,
        functionName = "",
        methodId = "0x",
        gasPrice = gasPrice,
        gasUsed = gasUsed,
        hash = "0xc933bcf1c692a29b5e659ccf7feb4fffc41cc2494adf7d71bd3be10aaf804730",
        isError = "0",
        timeStamp = "1788341900",
        to = "0x8ba1f109551bd432803012645ac136ddd64dba72",
        txReceiptStatus = "1",
        value = value,
        input = "0x",
    )

    private companion object {
        const val WALLET = "0x6653Db93Cb761Fe63aD959a0B32F0765A9A2EE18"
    }
}