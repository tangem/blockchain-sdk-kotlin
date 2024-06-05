package com.tangem.blockchain.common.network.providers

/**
 * Provider type
 *
 * @author Andrew Khokhlov on 08/04/2024
 */
sealed interface ProviderType {

    data class Public(val url: String) : ProviderType

    object NowNodes : ProviderType

    object GetBlock : ProviderType

    object QuickNode : ProviderType

    sealed interface BitcoinLike : ProviderType {
        object Blockchair : BitcoinLike
        object Blockcypher : BitcoinLike
    }

    sealed interface EthereumLike : ProviderType {
        object Infura : EthereumLike
    }

    sealed interface Solana : ProviderType {
        object Official : Solana
    }

    sealed interface Cardano : ProviderType {
        object Rosetta : Cardano
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