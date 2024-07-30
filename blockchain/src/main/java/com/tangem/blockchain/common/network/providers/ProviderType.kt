package com.tangem.blockchain.common.network.providers

/**
 * Provider type
 *
[REDACTED_AUTHOR]
 */
sealed interface ProviderType {

    data class Public(val url: String) : ProviderType

    data object NowNodes : ProviderType

    data object GetBlock : ProviderType

    data object QuickNode : ProviderType

    sealed interface BitcoinLike : ProviderType {
        data object Blockchair : BitcoinLike
        data object Blockcypher : BitcoinLike
    }

    sealed interface EthereumLike : ProviderType {
        data object Infura : EthereumLike
    }

    sealed interface Solana : ProviderType {
        data object Official : Solana
    }

    sealed interface Cardano : ProviderType {
        data object Rosetta : Cardano
        object Adalite : Cardano
    }

    sealed interface Tron : ProviderType {
        object TronGrid : Tron
    }

    sealed interface Kaspa : ProviderType {
        object SecondaryAPI : Kaspa
    }

    sealed interface Ton : ProviderType {
        object TonCentral : Ton
    }

    sealed interface Chia : ProviderType {
        object Tangem : Chia
        object FireAcademy : Chia
    }

    sealed interface Hedera : ProviderType {
        object Arkhia : Hedera
    }

    sealed interface Koinos : ProviderType {
        object KoinosPro : Koinos
    }

    sealed interface Bittensor : ProviderType {
        object Dwellir : Bittensor
        object Onfinality : Bittensor
    }
}