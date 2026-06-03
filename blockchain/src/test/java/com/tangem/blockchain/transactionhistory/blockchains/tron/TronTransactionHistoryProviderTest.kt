package com.tangem.blockchain.transactionhistory.blockchains.tron

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.pagination.Page
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.network.blockbook.network.BlockBookApi
import com.tangem.blockchain.network.blockbook.network.responses.GetAddressResponse
import com.tangem.blockchain.transactionhistory.TransactionHistoryState
import com.tangem.blockchain.transactionhistory.models.TransactionHistoryItem
import com.tangem.blockchain.transactionhistory.models.TransactionHistoryItem.TransactionType.TronStakingTransactionType
import com.tangem.blockchain.transactionhistory.models.TransactionHistoryRequest
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.math.BigDecimal
import java.util.concurrent.TimeUnit

@Suppress("LargeClass")
class TronTransactionHistoryProviderTest {

    private val blockBookApi = mockk<BlockBookApi>()
    private val provider = TronTransactionHistoryProvider(
        blockchain = Blockchain.Tron,
        blockBookApi = blockBookApi,
    )

    // region getTransactionHistoryState - Coin

    @Test
    fun `coin state returns HasTransactions when transactions present`() = runTest {
        coEvery {
            blockBookApi.getTransactions(WALLET, null, 1, TransactionHistoryRequest.FilterType.Coin)
        } returns addressResponse(transactions = listOf(coinTransaction()))

        val state =
            provider.getTransactionHistoryState(WALLET, TransactionHistoryRequest.FilterType.Coin)

        assertThat(state).isInstanceOf(TransactionHistoryState.Success.HasTransactions::class.java)
        assertThat((state as TransactionHistoryState.Success.HasTransactions).txCount).isEqualTo(1)
    }

    @Test
    fun `coin state returns Empty when transactions are null`() = runTest {
        coEvery {
            blockBookApi.getTransactions(WALLET, null, 1, TransactionHistoryRequest.FilterType.Coin)
        } returns addressResponse(transactions = null)

        val state =
            provider.getTransactionHistoryState(WALLET, TransactionHistoryRequest.FilterType.Coin)

        assertThat(state).isEqualTo(TransactionHistoryState.Success.Empty)
    }

    @Test
    fun `coin state returns Empty when transactions are empty`() = runTest {
        coEvery {
            blockBookApi.getTransactions(WALLET, null, 1, TransactionHistoryRequest.FilterType.Coin)
        } returns addressResponse(transactions = emptyList())

        val state =
            provider.getTransactionHistoryState(WALLET, TransactionHistoryRequest.FilterType.Coin)

        assertThat(state).isEqualTo(TransactionHistoryState.Success.Empty)
    }

    @Test
    fun `coin state returns FetchError when api throws`() = runTest {
        val cause = IllegalStateException("network down")
        coEvery {
            blockBookApi.getTransactions(WALLET, null, 1, TransactionHistoryRequest.FilterType.Coin)
        } throws cause

        val state =
            provider.getTransactionHistoryState(WALLET, TransactionHistoryRequest.FilterType.Coin)

        assertThat(state).isInstanceOf(TransactionHistoryState.Failed.FetchError::class.java)
        assertThat((state as TransactionHistoryState.Failed.FetchError).exception.message).isEqualTo(
            cause.message,
        )
    }

    // endregion

    // region getTransactionHistoryState - Contract

    @Test
    fun `contract state returns HasTransactions when token has transfers`() = runTest {
        val filter = contractFilter()
        coEvery {
            blockBookApi.getTransactions(WALLET, null, 1, filter)
        } returns addressResponse(
            transactions = emptyList(),
            trxTokens = listOf(trxToken(id = CONTRACT, transfers = 7)),
        )

        val state = provider.getTransactionHistoryState(WALLET, filter)

        assertThat(state).isInstanceOf(TransactionHistoryState.Success.HasTransactions::class.java)
        assertThat((state as TransactionHistoryState.Success.HasTransactions).txCount).isEqualTo(7)
    }

    @Test
    fun `contract state matches token by contract address case-insensitively`() = runTest {
        val filter = contractFilter()
        coEvery {
            blockBookApi.getTransactions(WALLET, null, 1, filter)
        } returns addressResponse(
            transactions = emptyList(),
            trxTokens = listOf(
                trxToken(
                    id = "another",
                    transfers = 3,
                    contract = CONTRACT.uppercase(),
                ),
            ),
        )

        val state = provider.getTransactionHistoryState(WALLET, filter)

        assertThat((state as TransactionHistoryState.Success.HasTransactions).txCount).isEqualTo(3)
    }

    @Test
    fun `contract state returns Empty when matching token not found`() = runTest {
        val filter = contractFilter()
        coEvery {
            blockBookApi.getTransactions(WALLET, null, 1, filter)
        } returns addressResponse(
            transactions = emptyList(),
            trxTokens = listOf(trxToken(id = "different", transfers = 5)),
        )

        val state = provider.getTransactionHistoryState(WALLET, filter)

        assertThat(state).isEqualTo(TransactionHistoryState.Success.Empty)
    }

    @Test
    fun `contract state returns Empty when token transfers is null`() = runTest {
        val filter = contractFilter()
        coEvery {
            blockBookApi.getTransactions(WALLET, null, 1, filter)
        } returns addressResponse(
            transactions = emptyList(),
            trxTokens = listOf(trxToken(id = CONTRACT, transfers = null)),
        )

        val state = provider.getTransactionHistoryState(WALLET, filter)

        assertThat(state).isEqualTo(TransactionHistoryState.Success.Empty)
    }

    @Test
    fun `contract state returns Empty when token transfers is zero`() = runTest {
        val filter = contractFilter()
        coEvery {
            blockBookApi.getTransactions(WALLET, null, 1, filter)
        } returns addressResponse(
            transactions = emptyList(),
            trxTokens = listOf(trxToken(id = CONTRACT, transfers = 0)),
        )

        val state = provider.getTransactionHistoryState(WALLET, filter)

        assertThat(state).isEqualTo(TransactionHistoryState.Success.Empty)
    }

    @Test
    fun `contract state returns Empty when trxTokens is null`() = runTest {
        val filter = contractFilter()
        coEvery {
            blockBookApi.getTransactions(WALLET, null, 1, filter)
        } returns addressResponse(transactions = emptyList(), trxTokens = null)

        val state = provider.getTransactionHistoryState(WALLET, filter)

        assertThat(state).isEqualTo(TransactionHistoryState.Success.Empty)
    }

    // endregion

    // region getTransactionsHistory - pagination

    @Test
    fun `initial page returns Next page when response page is present`() = runTest {
        val request = coinRequest(page = Page.Initial)
        coEvery {
            blockBookApi.getTransactions(
                WALLET,
                null,
                PAGE_SIZE,
                TransactionHistoryRequest.FilterType.Coin,
            )
        } returns addressResponse(transactions = listOf(coinTransaction()), page = 1)

        val result = provider.getTransactionsHistory(request)

        val wrapper = (result as Result.Success).data
        assertThat(wrapper.nextPage).isEqualTo(Page.Next("2"))
        assertThat(wrapper.items).hasSize(1)
    }

    @Test
    fun `next page request forwards page value to api`() = runTest {
        val request = coinRequest(page = Page.Next("3"))
        coEvery {
            blockBookApi.getTransactions(
                WALLET,
                "3",
                PAGE_SIZE,
                TransactionHistoryRequest.FilterType.Coin,
            )
        } returns addressResponse(transactions = listOf(coinTransaction()), page = 3)

        val result = provider.getTransactionsHistory(request)

        val wrapper = (result as Result.Success).data
        assertThat(wrapper.nextPage).isEqualTo(Page.Next("4"))
    }

    @Test
    fun `last page is returned when response page is null`() = runTest {
        val request = coinRequest(page = Page.Initial)
        coEvery {
            blockBookApi.getTransactions(
                WALLET,
                null,
                PAGE_SIZE,
                TransactionHistoryRequest.FilterType.Coin,
            )
        } returns addressResponse(transactions = listOf(coinTransaction()), page = null)

        val result = provider.getTransactionsHistory(request)

        val wrapper = (result as Result.Success).data
        assertThat(wrapper.nextPage).isEqualTo(Page.LastPage)
        assertThat(wrapper.items).hasSize(1)
    }

    @Test
    fun `end reached returns empty items and last page when response page is lower than requested`() = runTest {
        val request = coinRequest(page = Page.Next("5"))
        coEvery {
            blockBookApi.getTransactions(
                WALLET,
                "5",
                PAGE_SIZE,
                TransactionHistoryRequest.FilterType.Coin,
            )
        } returns addressResponse(transactions = listOf(coinTransaction()), page = 2)

        val result = provider.getTransactionsHistory(request)

        val wrapper = (result as Result.Success).data
        assertThat(wrapper.nextPage).isEqualTo(Page.LastPage)
        assertThat(wrapper.items).isEmpty()
    }

    @Test
    fun `empty transactions list returns empty items`() = runTest {
        val request = coinRequest(page = Page.Initial)
        coEvery {
            blockBookApi.getTransactions(
                WALLET,
                null,
                PAGE_SIZE,
                TransactionHistoryRequest.FilterType.Coin,
            )
        } returns addressResponse(transactions = emptyList(), page = 1)

        val result = provider.getTransactionsHistory(request)

        val wrapper = (result as Result.Success).data
        assertThat(wrapper.items).isEmpty()
        assertThat(wrapper.nextPage).isEqualTo(Page.Next("2"))
    }

    @Test
    fun `null transactions list returns empty items`() = runTest {
        val request = coinRequest(page = Page.Initial)
        coEvery {
            blockBookApi.getTransactions(
                WALLET,
                null,
                PAGE_SIZE,
                TransactionHistoryRequest.FilterType.Coin,
            )
        } returns addressResponse(transactions = null, page = 1)

        val result = provider.getTransactionsHistory(request)

        val wrapper = (result as Result.Success).data
        assertThat(wrapper.items).isEmpty()
    }

    @Test
    fun `history api error is wrapped into Result Failure`() = runTest {
        val request = coinRequest(page = Page.Initial)
        coEvery {
            blockBookApi.getTransactions(
                WALLET,
                null,
                PAGE_SIZE,
                TransactionHistoryRequest.FilterType.Coin,
            )
        } throws IllegalStateException("boom")

        val result = provider.getTransactionsHistory(request)

        assertThat(result).isInstanceOf(Result.Failure::class.java)
    }

    // endregion

    // region getTransactionsHistory - coin mapping

    @Test
    fun `coin transfer is incoming when wallet is not the source`() = runTest {
        val tx = coinTransaction(fromAddress = RECIPIENT, toAddress = WALLET)
        val result = singleCoinItem(tx)

        assertThat(result.isOutgoing).isFalse()
    }

    @Test
    fun `txHash strips 0x prefix`() = runTest {
        val tx = coinTransaction(txid = "0x${TX_HASH}")
        val result = singleCoinItem(tx)

        assertThat(result.txHash).isEqualTo(TX_HASH)
    }

    @Test
    fun `timestamp is converted from seconds to millis`() = runTest {
        val tx = coinTransaction(blockTime = 1_700_000_000)
        val result = singleCoinItem(tx)

        assertThat(result.timestamp).isEqualTo(TimeUnit.SECONDS.toMillis(1_700_000_000L))
    }

    @Test
    fun `destination falls back to vout address when toAddress is null`() = runTest {
        val tx = coinTransaction(
            toAddress = null,
            vout = listOf(
                GetAddressResponse.Transaction.Vout(
                    addresses = listOf(RECIPIENT),
                    hex = null,
                    value = null,
                ),
            ),
        )
        val result = singleCoinItem(tx)

        val destination = result.destinationType as TransactionHistoryItem.DestinationType.Single
        assertThat(destination.addressType.address).isEqualTo(RECIPIENT)
    }

    @Test
    fun `source falls back to vin address when fromAddress is null`() = runTest {
        val tx = coinTransaction(
            fromAddress = null,
            vin = listOf(
                GetAddressResponse.Transaction.Vin(
                    addresses = listOf(WALLET),
                    value = null,
                ),
            ),
        )
        val result = singleCoinItem(tx)

        assertThat((result.sourceType as TransactionHistoryItem.SourceType.Single).address).isEqualTo(
            WALLET,
        )
    }

    @Test
    fun `destination is Contract address type when token transfers exist`() = runTest {
        val tx = coinTransaction(
            toAddress = RECIPIENT,
            tokenTransfers = listOf(tokenTransfer()),
        )
        val result = singleCoinItem(tx)

        val destination = result.destinationType as TransactionHistoryItem.DestinationType.Single
        assertThat(destination.addressType).isInstanceOf(TransactionHistoryItem.AddressType.Contract::class.java)
    }

    // endregion

    // region getTransactionsHistory - status

    @Test
    fun `status OK maps to Confirmed`() = runTest {
        val tx = coinTransaction(status = GetAddressResponse.Transaction.StatusType.OK)
        assertThat(singleCoinItem(tx).status).isEqualTo(TransactionHistoryItem.TransactionStatus.Confirmed)
    }

    @Test
    fun `status PENDING maps to Unconfirmed`() = runTest {
        val tx = coinTransaction(status = GetAddressResponse.Transaction.StatusType.PENDING)
        assertThat(singleCoinItem(tx).status).isEqualTo(TransactionHistoryItem.TransactionStatus.Unconfirmed)
    }

    @Test
    fun `status FAILURE maps to Failed`() = runTest {
        val tx = coinTransaction(status = GetAddressResponse.Transaction.StatusType.FAILURE)
        assertThat(singleCoinItem(tx).status).isEqualTo(TransactionHistoryItem.TransactionStatus.Failed)
    }

    @Test
    fun `missing receipt with confirmations maps to Confirmed`() = runTest {
        val tx = coinTransaction(receipt = null, confirmations = 12)
        assertThat(singleCoinItem(tx).status).isEqualTo(TransactionHistoryItem.TransactionStatus.Confirmed)
    }

    @Test
    fun `missing receipt without confirmations maps to Unconfirmed`() = runTest {
        val tx = coinTransaction(receipt = null, confirmations = 0)
        assertThat(singleCoinItem(tx).status).isEqualTo(TransactionHistoryItem.TransactionStatus.Unconfirmed)
    }

    @Test
    fun `null receipt status with confirmations maps to Confirmed`() = runTest {
        val tx = coinTransaction(
            receipt = GetAddressResponse.Transaction.TronTXReceipt(status = null),
            confirmations = 1,
        )
        assertThat(singleCoinItem(tx).status).isEqualTo(TransactionHistoryItem.TransactionStatus.Confirmed)
    }

    // endregion

    // region getTransactionsHistory - staking type & direction

    @Test
    fun `vote witness contract is outgoing and exposes validator`() = runTest {
        val tx = coinTransaction(
            contractType = 4,
            voteList = mapOf(VALIDATOR to 1),
        )
        val result = singleCoinItem(tx)

        val type = result.type as TronStakingTransactionType.VoteWitnessContract
        assertThat(type.validatorAddress).isEqualTo(VALIDATOR)
        assertThat(result.isOutgoing).isTrue()
    }

    @Test
    fun `freeze balance is outgoing`() = runTest {
        val tx = coinTransaction(contractType = 54)
        val result = singleCoinItem(tx)

        assertThat(result.type).isEqualTo(TronStakingTransactionType.FreezeBalanceV2Contract)
        assertThat(result.isOutgoing).isTrue()
    }

    @Test
    fun `unfreeze balance is incoming`() = runTest {
        val tx = coinTransaction(contractType = 55)
        val result = singleCoinItem(tx)

        assertThat(result.type).isEqualTo(TronStakingTransactionType.UnfreezeBalanceV2Contract)
        assertThat(result.isOutgoing).isFalse()
    }

    @Test
    fun `withdraw balance is incoming`() = runTest {
        val tx = coinTransaction(contractType = 13)
        val result = singleCoinItem(tx)

        assertThat(result.type).isEqualTo(TronStakingTransactionType.WithdrawBalanceContract)
        assertThat(result.isOutgoing).isFalse()
    }

    @Test
    fun `withdraw expire unfreeze is incoming`() = runTest {
        val tx = coinTransaction(contractType = 56)
        val result = singleCoinItem(tx)

        assertThat(result.type).isEqualTo(TronStakingTransactionType.WithdrawExpireUnfreezeContract)
        assertThat(result.isOutgoing).isFalse()
    }

    @Test
    fun `transfer asset contract maps to Transfer`() = runTest {
        val tx = coinTransaction(contractType = 2)
        assertThat(singleCoinItem(tx).type).isEqualTo(TransactionHistoryItem.TransactionType.Transfer)
    }

    @Test
    fun `unknown contract type maps to ContractMethod with contract name`() = runTest {
        val tx = coinTransaction(contractType = 999, contractName = "SomeContract")
        val type = singleCoinItem(tx).type as TransactionHistoryItem.TransactionType.ContractMethod
        assertThat(type.id).isEqualTo("SomeContract")
    }

    @Test
    fun `unknown contract type with null name maps to ContractMethod with empty id`() = runTest {
        val tx = coinTransaction(contractType = 999, contractName = null)
        val type = singleCoinItem(tx).type as TransactionHistoryItem.TransactionType.ContractMethod
        assertThat(type.id).isEmpty()
    }

    // endregion

    // region getTransactionsHistory - extractTypeV2 (contractType == null, chainExtraData based)

    @Test
    fun `v2 null chainExtraData maps to Transfer`() = runTest {
        val tx = coinTransaction(contractType = null, chainExtraData = null)
        assertThat(singleCoinItem(tx).type).isEqualTo(TransactionHistoryItem.TransactionType.Transfer)
    }

    @Test
    fun `v2 non-tron payloadType maps to Transfer`() = runTest {
        val tx = coinTransaction(
            contractType = null,
            chainExtraData = chainExtraData(payloadType = "ethereum", contractType = "VoteWitnessContract"),
        )
        assertThat(singleCoinItem(tx).type).isEqualTo(TransactionHistoryItem.TransactionType.Transfer)
    }

    @Test
    fun `v2 null payload contractType maps to Transfer`() = runTest {
        val tx = coinTransaction(
            contractType = null,
            chainExtraData = chainExtraData(contractType = null),
        )
        assertThat(singleCoinItem(tx).type).isEqualTo(TransactionHistoryItem.TransactionType.Transfer)
    }

    @Test
    fun `v2 TransferContract maps to Transfer`() = runTest {
        val tx = coinTransaction(
            contractType = null,
            chainExtraData = chainExtraData(contractType = "TransferContract"),
        )
        assertThat(singleCoinItem(tx).type).isEqualTo(TransactionHistoryItem.TransactionType.Transfer)
    }

    @Test
    fun `v2 TriggerSmartContract maps to Transfer`() = runTest {
        val tx = coinTransaction(
            contractType = null,
            chainExtraData = chainExtraData(contractType = "TriggerSmartContract"),
        )
        assertThat(singleCoinItem(tx).type).isEqualTo(TransactionHistoryItem.TransactionType.Transfer)
    }

    @Test
    fun `v2 VoteWitnessContract is outgoing and exposes validator from votes`() = runTest {
        val tx = coinTransaction(
            contractType = null,
            chainExtraData = chainExtraData(
                contractType = "VoteWitnessContract",
                votes = listOf(
                    GetAddressResponse.Transaction.ChainExtraData.Payload.Vote(address = VALIDATOR, count = "1"),
                ),
            ),
        )
        val result = singleCoinItem(tx)

        val type = result.type as TronStakingTransactionType.VoteWitnessContract
        assertThat(type.validatorAddress).isEqualTo(VALIDATOR)
        assertThat(result.isOutgoing).isTrue()
    }

    @Test
    fun `v2 VoteWitnessContract with null votes exposes empty validator`() = runTest {
        val tx = coinTransaction(
            contractType = null,
            chainExtraData = chainExtraData(contractType = "VoteWitnessContract", votes = null),
        )
        val type = singleCoinItem(tx).type as TronStakingTransactionType.VoteWitnessContract
        assertThat(type.validatorAddress).isEmpty()
    }

    @Test
    fun `v2 WithdrawBalanceContract is claim rewards and incoming`() = runTest {
        val tx = coinTransaction(
            contractType = null,
            chainExtraData = chainExtraData(contractType = "WithdrawBalanceContract"),
        )
        val result = singleCoinItem(tx)

        assertThat(result.type).isEqualTo(TronStakingTransactionType.WithdrawBalanceContract)
        assertThat(result.isOutgoing).isFalse()
    }

    @Test
    fun `v2 FreezeBalanceV2Contract is outgoing`() = runTest {
        val tx = coinTransaction(
            contractType = null,
            chainExtraData = chainExtraData(contractType = "FreezeBalanceV2Contract"),
        )
        val result = singleCoinItem(tx)

        assertThat(result.type).isEqualTo(TronStakingTransactionType.FreezeBalanceV2Contract)
        assertThat(result.isOutgoing).isTrue()
    }

    @Test
    fun `v2 UnfreezeBalanceV2Contract is incoming`() = runTest {
        val tx = coinTransaction(
            contractType = null,
            chainExtraData = chainExtraData(contractType = "UnfreezeBalanceV2Contract"),
        )
        val result = singleCoinItem(tx)

        assertThat(result.type).isEqualTo(TronStakingTransactionType.UnfreezeBalanceV2Contract)
        assertThat(result.isOutgoing).isFalse()
    }

    @Test
    fun `v2 WithdrawExpireUnfreezeContract is incoming`() = runTest {
        val tx = coinTransaction(
            contractType = null,
            chainExtraData = chainExtraData(contractType = "WithdrawExpireUnfreezeContract"),
        )
        val result = singleCoinItem(tx)

        assertThat(result.type).isEqualTo(TronStakingTransactionType.WithdrawExpireUnfreezeContract)
        assertThat(result.isOutgoing).isFalse()
    }

    @Test
    fun `v2 unknown contractType maps to Transfer`() = runTest {
        val tx = coinTransaction(
            contractType = null,
            chainExtraData = chainExtraData(contractType = "SomethingUnknownContract"),
        )
        assertThat(singleCoinItem(tx).type).isEqualTo(TransactionHistoryItem.TransactionType.Transfer)
    }

    // endregion

    // region getTransactionsHistory - contract/token mapping

    @Test
    fun `token transfer maps amount token and addresses`() = runTest {
        val transfer = tokenTransfer(
            from = WALLET,
            to = RECIPIENT,
            value = "2500000",
            name = "Tether",
            symbol = "USDT",
            token = CONTRACT,
        )
        val tx = coinTransaction(tokenTransfers = listOf(transfer))
        val result = singleContractItem(tx, decimals = 6)

        assertThat(result.amount.value!!.compareTo(BigDecimal("2.5"))).isEqualTo(0)
        assertThat(result.amount.type).isInstanceOf(AmountType.Token::class.java)
        assertThat(result.type).isEqualTo(TransactionHistoryItem.TransactionType.Transfer)
        assertThat((result.sourceType as TransactionHistoryItem.SourceType.Single).address).isEqualTo(
            WALLET,
        )
        val destination = result.destinationType as TransactionHistoryItem.DestinationType.Single
        assertThat(destination.addressType).isInstanceOf(TransactionHistoryItem.AddressType.User::class.java)
        assertThat(destination.addressType.address).isEqualTo(RECIPIENT)
        assertThat(result.isOutgoing).isTrue()
    }

    @Test
    fun `token transfer with null value is excluded as zero amount`() = runTest {
        // For the Contract filter a zero/null amount transfer is filtered out of history.
        val transfer = tokenTransfer(from = WALLET, to = RECIPIENT, value = null, token = CONTRACT)
        val tx = coinTransaction(tokenTransfers = listOf(transfer))
        val filter = contractFilter()
        val request = TransactionHistoryRequest(
            address = WALLET,
            decimals = 6,
            page = Page.Initial,
            pageSize = PAGE_SIZE,
            filterType = filter,
        )
        coEvery {
            blockBookApi.getTransactions(WALLET, null, PAGE_SIZE, filter)
        } returns addressResponse(transactions = listOf(tx), page = 1)

        val result = provider.getTransactionsHistory(request)

        assertThat((result as Result.Success).data.items).isEmpty()
    }

    @Test
    fun `token transfer matched by from address when wallet is sender`() = runTest {
        // contract matches but `to` is the recipient, wallet is `from` -> the || branch on `from` matches
        val transfer =
            tokenTransfer(from = WALLET, to = RECIPIENT, value = "1000000", token = CONTRACT)
        val tx = coinTransaction(tokenTransfers = listOf(transfer))
        val result = singleContractItem(tx, decimals = 6)

        assertThat(result.amount.value!!.compareTo(BigDecimal("1"))).isEqualTo(0)
    }

    // endregion

    // region helpers

    private suspend fun singleCoinItem(tx: GetAddressResponse.Transaction): TransactionHistoryItem {
        val request = coinRequest(page = Page.Initial)
        coEvery {
            blockBookApi.getTransactions(
                WALLET,
                null,
                PAGE_SIZE,
                TransactionHistoryRequest.FilterType.Coin,
            )
        } returns addressResponse(transactions = listOf(tx), page = 1)

        val result = provider.getTransactionsHistory(request)
        return (result as Result.Success).data.items.single()
    }

    private suspend fun singleContractItem(tx: GetAddressResponse.Transaction, decimals: Int): TransactionHistoryItem {
        val filter = contractFilter()
        val request = TransactionHistoryRequest(
            address = WALLET,
            decimals = decimals,
            page = Page.Initial,
            pageSize = PAGE_SIZE,
            filterType = filter,
        )
        coEvery {
            blockBookApi.getTransactions(WALLET, null, PAGE_SIZE, filter)
        } returns addressResponse(transactions = listOf(tx), page = 1)

        val result = provider.getTransactionsHistory(request)
        return (result as Result.Success).data.items.single()
    }

    private fun coinRequest(page: Page) = TransactionHistoryRequest(
        address = WALLET,
        decimals = Blockchain.Tron.decimals(),
        page = page,
        pageSize = PAGE_SIZE,
        filterType = TransactionHistoryRequest.FilterType.Coin,
    )

    private fun contractFilter() = TransactionHistoryRequest.FilterType.Contract(
        tokenInfo = Token(
            name = "Tether",
            symbol = "USDT",
            contractAddress = CONTRACT,
            decimals = 6,
        ),
    )

    private fun addressResponse(
        transactions: List<GetAddressResponse.Transaction>?,
        page: Int? = null,
        trxTokens: List<GetAddressResponse.TrxToken>? = null,
    ) = GetAddressResponse(
        balance = "0",
        unconfirmedTxs = 0,
        txs = transactions?.size ?: 0,
        transactions = transactions,
        page = page,
        totalPages = null,
        itemsOnPage = null,
        trxTokens = trxTokens,
    )

    private fun trxToken(id: String, name: String? = "Tether", transfers: Int?, contract: String? = id) =
        GetAddressResponse.TrxToken(
            type = "TRC20",
            name = name,
            id = id,
            transfers = transfers,
            balance = "0",
            contract = contract,
        )

    @Suppress("LongParameterList")
    private fun coinTransaction(
        txid: String = TX_HASH,
        value: String = "1000000",
        fees: String = "0",
        blockTime: Int = 1_700_000_000,
        confirmations: Int = 1,
        fromAddress: String? = WALLET,
        toAddress: String? = RECIPIENT,
        contractType: Int? = 1,
        contractName: String? = null,
        voteList: Map<String, Int>? = null,
        chainExtraData: GetAddressResponse.Transaction.ChainExtraData? = null,
        status: GetAddressResponse.Transaction.StatusType? = GetAddressResponse.Transaction.StatusType.OK,
        receipt: GetAddressResponse.Transaction.TronTXReceipt? =
            GetAddressResponse.Transaction.TronTXReceipt(status = status),
        vin: List<GetAddressResponse.Transaction.Vin> = emptyList(),
        vout: List<GetAddressResponse.Transaction.Vout> = emptyList(),
        tokenTransfers: List<GetAddressResponse.Transaction.TokenTransfer> = emptyList(),
    ) = GetAddressResponse.Transaction(
        txid = txid,
        vout = vout,
        confirmations = confirmations,
        blockTime = blockTime,
        value = value,
        vin = vin,
        fees = fees,
        tokenTransfers = tokenTransfers,
        ethereumSpecific = null,
        chainExtraData = chainExtraData,
        tronTXReceipt = receipt,
        fromAddress = fromAddress,
        toAddress = toAddress,
        contractType = contractType,
        contractName = contractName,
        voteList = voteList,
    )

    @Suppress("LongParameterList")
    private fun chainExtraData(
        payloadType: String? = "tron",
        contractType: String? = null,
        votes: List<GetAddressResponse.Transaction.ChainExtraData.Payload.Vote>? = null,
    ) = GetAddressResponse.Transaction.ChainExtraData(
        payloadType = payloadType,
        payload = GetAddressResponse.Transaction.ChainExtraData.Payload(
            contractType = contractType,
            operation = null,
            bandwidthUsage = null,
            votes = votes,
        ),
    )

    @Suppress("LongParameterList")
    private fun tokenTransfer(
        from: String = WALLET,
        to: String = RECIPIENT,
        value: String? = "1000000",
        name: String? = "Tether",
        symbol: String? = "USDT",
        token: String? = CONTRACT,
    ) = GetAddressResponse.Transaction.TokenTransfer(
        type = "TRC20",
        from = from,
        to = to,
        contract = token,
        token = token,
        name = name,
        symbol = symbol,
        decimals = 6,
        value = value,
    )

    private companion object {
        const val WALLET = "TJRyWwFs9wTFGZg3JbrVriFbNfCug5tDeC"
        const val RECIPIENT = "TQrY8tryqsYVCYS3MFbtffiPp2ccyn4STm"
        const val VALIDATOR = "TKSXDA8HfE9E1y39RczVQ1ZascUEtaSToF"
        const val CONTRACT = "TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t"
        const val TX_HASH = "abc123deadbeef"
        const val PAGE_SIZE = 20
    }

    // endregion
}