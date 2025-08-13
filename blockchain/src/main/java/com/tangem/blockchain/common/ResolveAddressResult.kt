package com.tangem.blockchain.common

sealed class ResolveAddressResult {
    data class Resolved(val address: String) : ResolveAddressResult()
    data class Error(val error: Exception) : ResolveAddressResult()
    data object NotSupported : ResolveAddressResult()
}

sealed interface ReversResolveAddressResult {
    data class Resolved(val name: String) : ReversResolveAddressResult
    data class Error(val error: Exception) : ReversResolveAddressResult
    data object NotSupported : ReversResolveAddressResult
}