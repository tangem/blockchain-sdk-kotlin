package com.tangem.blockchain.common.network.providers

import kotlin.reflect.KClass

/** Get all non-public provider types for testing purpose. Method uses reflection */
fun getAllNonpublicProviderTypes(): List<ProviderType> = getAllNonpublicProviderTypes(kclass = ProviderType::class)

private fun getAllNonpublicProviderTypes(kclass: KClass<out ProviderType>): List<ProviderType> {
    return kclass.sealedSubclasses
        .flatMap {
            if (it.isSealed) {
                getAllNonpublicProviderTypes(it)
            } else {
                listOf(it.objectInstance)
            }
        }
        .filterNotNull()
}