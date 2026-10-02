package com.tangem.blockchain.collectibles

import com.tangem.blockchain.collectibles.models.CollectiblesVault
import com.tangem.blockchain.collectibles.models.CollectiblesVaultUsdc
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.TransactionSendResult
import com.tangem.blockchain.extensions.Result

/** No-op provider for every blockchain the gacha program is not deployed on. */
internal object DefaultCollectiblesVaultProvider : CollectiblesVaultProvider {

    override fun isCollectiblesVaultSupported(): Boolean = false

    override fun getCollectiblesVaultAddress(): Result<String> = unsupported()

    override suspend fun isCollectiblesVaultInitialized(): Result<Boolean> = unsupported()

    override suspend fun getCollectiblesVault(): Result<CollectiblesVault?> = unsupported()

    override suspend fun getCollectiblesVaultUsdc(): Result<CollectiblesVaultUsdc> = unsupported()

    override suspend fun buildInitCollectiblesVault(
        hotDelegate: String,
        perSpinCap: Long,
        dailyCap: Long,
    ): Result<TransactionData.Compiled> = unsupported()

    override suspend fun buildUpdateCollectiblesVault(
        hotDelegate: String?,
        perSpinCap: Long?,
        dailyCap: Long?,
    ): Result<TransactionData.Compiled> = unsupported()

    override suspend fun buildCloseCollectiblesVault(): Result<TransactionData.Compiled> = unsupported()

    override suspend fun sendPurchasePack(
        hotDelegate: String,
        amount: Long,
        feeAmount: Long,
        memo: String,
        signer: suspend (message: ByteArray) -> Result<ByteArray>,
    ): Result<TransactionSendResult> = unsupported()

    private fun unsupported(): Result.Failure = Result.Failure(
        BlockchainSdkError.UnsupportedOperation("Gacha vault is not supported for this blockchain"),
    )
}