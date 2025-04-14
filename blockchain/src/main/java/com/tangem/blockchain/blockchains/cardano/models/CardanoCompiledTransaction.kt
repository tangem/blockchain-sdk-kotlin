package com.tangem.blockchain.blockchains.cardano.models

import java.math.BigDecimal

data class CardanoCompiledTransaction(
    val inputs: List<Input>,
    val outputs: List<Output>,
    val fee: BigDecimal,
    val auxiliaryScripts: String?,
    val certificates: List<Certificate>,
    val collateralInputs: List<String>?,
    val currentTreasuryValue: String?,
    val era: String?,
    val governanceActions: List<String>?,
    val metadata: String?,
    val mint: String?,
    val redeemers: List<String>?,
    val referenceInputs: List<String>?,
    val requiredSigners: String?,
    val returnCollateral: String?,
    val totalCollateral: String?,
    val updateProposal: String?,
    val voters: List<String>?,
    val withdrawals: List<Long>?,
    val witnesses: List<String>?,
) {
    data class Input(
        val transactionID: String,
        val index: Long,
    )

    data class Output(
        val address: String,
        val amount: Long,
    )

    data class Credential(
        val keyHash: ByteArray,
    )

    data class RewardAddress(
        val network: Int?,
        val credential: Credential?,
    )

    sealed interface Certificate {
        data class StakeDelegation(
            val credential: Credential,
            val poolKeyHash: ByteArray,
        ) : Certificate

        data class StakeRegistrationLegacy(
            val credential: Credential,
        ) : Certificate

        data class StakeDeregistrationLegacy(
            val credential: Credential,
        ) : Certificate

        data class StakeDeregistrationConway(
            val credential: Credential,
            val coin: Long,
        ) : Certificate
    }
}

@Suppress("MagicNumber")
enum class CardanoTransactionIndex(val index: Int) {
    Input(0),
    Output(1),
    Fee(2),
    Certificates(4),
    Withdrawals(5),
}

enum class CertificateIndex {
    StakeRegistrationLegacy,
    StakeDeregistrationLegacy,
    StakeDelegation,
    PoolRegistration,
    PoolRetirement,
    GenesisKeyDelegation,
    MoveInstantaneousRewardsCert,
    StakeRegistrationConway,
    StakeDeregistrationConway,
    VoteDelegation,
    StakeAndVoteDelegation,
    StakeRegistrationAndDelegation,
    VoteRegistrationAndDelegation,
    StakeVoteRegistrationAndDelegation,
    CommitteeHotAuth,
    CommitteeColdResign,
    DRepRegistration,
    DRepDeregistration,
    DRepUpdate,
}