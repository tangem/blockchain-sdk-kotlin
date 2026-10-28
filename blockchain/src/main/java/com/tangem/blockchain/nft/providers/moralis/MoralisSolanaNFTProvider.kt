package com.tangem.blockchain.nft.providers.moralis

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.di.DepsContainer
import com.tangem.blockchain.common.logging.AddHeaderInterceptor
import com.tangem.blockchain.common.logging.Logger
import com.tangem.blockchain.common.moralis.MoralisConstants
import com.tangem.blockchain.network.createRetrofitInstance
import com.tangem.blockchain.nft.NFTProvider
import com.tangem.blockchain.nft.models.NFTAsset
import com.tangem.blockchain.nft.models.NFTCollection
import com.tangem.blockchain.nft.models.SolanaNFTTokenStandard
import com.tangem.blockchain.nft.providers.moralis.solana.MoralisCursorPaginator
import com.tangem.blockchain.nft.providers.moralis.solana.MoralisSolanaCollectionsMerger
import com.tangem.blockchain.nft.providers.moralis.solana.MoralisSolanaCoreNFTMapper
import com.tangem.blockchain.nft.providers.moralis.solana.MoralisSolanaLegacyNFTMapper
import com.tangem.blockchain.nft.providers.moralis.solana.Page
import com.tangem.blockchain.nft.providers.moralis.solana.network.MoralisSolanaApi
import com.tangem.blockchain.nft.providers.moralis.solana.network.MoralisWalletNFTsApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

internal class MoralisSolanaNFTProvider(
    private val blockchain: Blockchain,
    private val apiKey: String?,
) : NFTProvider {

    private val moralisSolanaApi = createRetrofitInstance(
        baseUrl = MoralisConstants.SOLANA_GATEWAY_API_URL,
        headerInterceptors = listOf(
            AddHeaderInterceptor(
                headers = buildMap {
                    apiKey?.let { put(MoralisConstants.API_KEY_HEADER, it) }
                },
            ),
        ),
    ).create(MoralisSolanaApi::class.java)

    private val walletNFTsApi = createRetrofitInstance(
        baseUrl = MoralisConstants.API_URL,
        headerInterceptors = listOf(
            AddHeaderInterceptor(headers = buildMap { apiKey?.let { put(MoralisConstants.API_KEY_HEADER, it) } }),
        ),
    ).create(MoralisWalletNFTsApi::class.java)

    private val paginator = MoralisCursorPaginator()
    private val coreMapper = MoralisSolanaCoreNFTMapper(blockchain)
    private val legacyMapper = MoralisSolanaLegacyNFTMapper(blockchain)

    override suspend fun getCollections(walletAddress: String): List<NFTCollection> = coroutineScope {
        val core = if (DepsContainer.blockchainFeatureToggles.isSolanaCoreNftEnabled) {
            async { getCoreCollections(walletAddress) }
        } else {
            null
        }
        val legacy = getLegacyCollections(walletAddress)
        MoralisSolanaCollectionsMerger.merge(legacy = legacy, core = core?.await().orEmpty())
    }

    private suspend fun getCoreCollections(walletAddress: String): List<NFTCollection> = try {
        loadCoreCollections(walletAddress)
    } catch (e: CancellationException) {
        throw e
    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
        Logger.logNetwork("$LOG_TAG Core NFTs dropped, legacy list kept: $e")
        emptyList()
    }

    private suspend fun loadCoreCollections(walletAddress: String): List<NFTCollection> {
        val items = paginator.loadAll { cursor ->
            val response = walletNFTsApi.getWalletNFTs(
                address = walletAddress,
                tokenStandards = SolanaNFTTokenStandard.MPL_CORE,
                cursor = cursor,
            )
            // Moralis reports a failed chain inside an HTTP 200, alongside whatever the page still holds. Throwing
            // here would drop that page, so the failure is only logged and the read goes on.
            val failedChains = response.meta?.failedChains
            if (!failedChains.isNullOrEmpty()) {
                Logger.logNetwork("$LOG_TAG Moralis failed chains: $failedChains")
            }
            Page(items = response.result.orEmpty(), nextCursor = response.cursor)
        }
        return coreMapper.toCollections(items, owner = walletAddress)
    }

    private suspend fun getLegacyCollections(walletAddress: String): List<NFTCollection> =
        legacyMapper.toCollections(moralisSolanaApi.getNFTAssets(address = walletAddress), owner = walletAddress)

    override suspend fun getAssets(
        walletAddress: String,
        collectionIdentifier: NFTCollection.Identifier,
    ): List<NFTAsset> {
        throw UnsupportedOperationException(
            "Use getCollections() to get all assets grouped into collection",
        )
    }

    override suspend fun getAsset(
        collectionIdentifier: NFTCollection.Identifier,
        assetIdentifier: NFTAsset.Identifier,
    ): NFTAsset? {
        throw UnsupportedOperationException(
            "Moralis Solana NFT API doesn't support this method, use getAssets() instead",
        )
    }

    override suspend fun getSalePrice(
        collectionIdentifier: NFTCollection.Identifier,
        assetIdentifier: NFTAsset.Identifier,
    ): NFTAsset.SalePrice? {
        throw UnsupportedOperationException("Moralis Solana NFT API doesn't support this method")
    }

    private companion object {
        const val LOG_TAG = "MoralisSolanaNFTProvider"
    }
}