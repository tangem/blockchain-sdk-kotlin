package com.tangem.blockchain.blockchains.cosmos.proto

import com.tangem.blockchain.blockchains.cosmos.proto.CosmosProtoMessage.DelegateAmount
import kotlinx.serialization.Serializable

@Serializable
data class CosmosProtoMessage(
    val delegateContainer: CosmosMessageDelegateContainer,
    val feeAndKeyContainer: CosmosMessageFeeAndKeyContainer,
    val chainId: String,
    val accountNumber: Int,
) {
    @Serializable
    data class CosmosMessageDelegateContainer(
        val delegate: CosmosMessageDelegate,
        val stakingProvider: String,
    )

    @Serializable
    data class CosmosMessageDelegate(
        val messageType: String,
        val delegateData: ByteArray,
    )

    @Serializable
    data class CosmosMessagePublicKeyContainer(
        val publicKeyWrapper: CosmosMessagePublicKeyWrapper,
        val publicKeyParamWrapper: CosmosMessagePublicKeyParamWrapper,
    )

    @Serializable
    data class CosmosMessagePublicKeyWrapper(
        val publicKeyType: String,
        val publicKey: CosmosMessagePublicKey,
    )

    @Serializable
    data class CosmosMessagePublicKeyParam(
        val param: Int,
    )

    @Serializable
    data class CosmosMessageFeeAndKeyContainer(
        val publicKeyContainer: CosmosMessagePublicKeyContainer,
        val feeContainer: CosmosMessageFeeContainer,
    )

    @Serializable
    data class CosmosMessagePublicKey(
        val publicKey: ByteArray,
    )

    @Serializable
    data class CosmosMessagePublicKeyParamWrapper(
        val publicKeyParam: CosmosMessagePublicKeyParam,
    )

    @Serializable
    data class CosmosMessageFeeContainer(
        val feeAmount: DelegateAmount,
        val gas: Long,
    )

    @Serializable
    data class DelegateAmount(
        val denomination: String,
        val amount: String,
    )
}

@Serializable
sealed class DelegateData(
    val delegatorAddress: String,
    val validatorAddress: String,
    val delegateAmount: DelegateAmount? = null,
)

@Serializable
data class RedelegateData(
    val delegatorAddress: String,
    val validatorSrcAddress: String,
    val validatorDstAddress: String,
    val delegateAmount: DelegateAmount? = null,
)