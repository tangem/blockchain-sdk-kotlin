package com.tangem.blockchain.common

sealed class ResolveAddressResult {
    data class Resolved(val address: String) : ResolveAddressResult()
    data class Error(val error: Exception) : ResolveAddressResult()
    data object NotSupported : ResolveAddressResult()
}