package com.tangem.blockchain.common

sealed class ResolveAddressResult {
    data class Resolved(val address: String) : ResolveAddressResult()
    data class Error(val error: Exception) : ResolveAddressResult()
    data object NotSupported : ResolveAddressResult()
}

sealed interface ReverseResolveAddressResult {
    data class Resolved(val name: String) : ReverseResolveAddressResult
    data class Error(val error: Exception) : ReverseResolveAddressResult
    data object NotSupported : ReverseResolveAddressResult
}