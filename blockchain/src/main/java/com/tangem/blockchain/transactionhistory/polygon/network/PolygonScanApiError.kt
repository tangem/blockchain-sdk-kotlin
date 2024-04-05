package com.tangem.blockchain.transactionhistory.polygon.network

internal sealed class PolygonScanApiError {
    object EndOfTransactionsReached : PolygonScanApiError()
    data class Error(val message: String?) : PolygonScanApiError()
}