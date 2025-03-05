package com.tangem.blockchain.blockchains.cardano.converter

import co.nstant.`in`.cbor.CborDecoder
import co.nstant.`in`.cbor.model.*
import co.nstant.`in`.cbor.model.Array
import co.nstant.`in`.cbor.model.Map
import com.tangem.blockchain.blockchains.cardano.models.CardanoCompiledTransaction
import com.tangem.blockchain.blockchains.cardano.models.CardanoTransactionIndex
import com.tangem.blockchain.blockchains.cardano.models.CertificateIndex
import com.tangem.blockchain.extensions.toBigDecimalOrDefault
import com.tangem.common.extensions.toHexString
import java.math.BigDecimal

class CardanoCompiledTransactionConverter {

    fun convert(cbor: CborDecoder): CardanoCompiledTransaction? {
        val decoded = cbor.decode().firstOrNull() as? Array ?: return null
        val container = decoded.dataItems.toList().firstOrNull() as? Map ?: return null

        var inputs = emptyList<CardanoCompiledTransaction.Input>()
        var outputs = emptyList<CardanoCompiledTransaction.Output>()
        var fee = BigDecimal.ZERO
        var certificates = emptyList<CardanoCompiledTransaction.Certificate>()
        var withdrawals = emptyList<Long>()

        container.keys.forEach { key ->
            val item = container[key]
            val keyValue = (key as? UnsignedInteger)?.value?.toInt() ?: return null
            when (keyValue) {
                CardanoTransactionIndex.Input.index -> inputs = convertInput(item)
                CardanoTransactionIndex.Output.index -> outputs = convertOutputs(item)
                CardanoTransactionIndex.Fee.index -> fee = convertFee(item)
                CardanoTransactionIndex.Certificates.index -> certificates = convertCertificates(item)
                CardanoTransactionIndex.Withdrawals.index -> withdrawals = convertWithdrawals(item)
            }
        }
        return CardanoCompiledTransaction(
            inputs = inputs,
            outputs = outputs,
            fee = fee,
            auxiliaryScripts = null,
            certificates = certificates,
            collateralInputs = listOf(),
            currentTreasuryValue = null,
            era = null,
            governanceActions = listOf(),
            metadata = null,
            mint = null,
            redeemers = listOf(),
            referenceInputs = listOf(),
            requiredSigners = null,
            returnCollateral = null,
            totalCollateral = null,
            updateProposal = null,
            voters = listOf(),
            withdrawals = withdrawals,
            witnesses = listOf(),
        )
    }

    private fun convertInput(inputsData: DataItem): List<CardanoCompiledTransaction.Input> {
        if (inputsData !is Array || inputsData.tag.value != TAG_VALUE) return emptyList()

        return inputsData.dataItems.mapNotNull { item ->
            if (item is Array) {
                val maybeTransactionId = item.dataItems.firstOrNull() as? ByteString ?: return emptyList()
                val maybeIndex = item.dataItems[1] as? UnsignedInteger ?: return emptyList()
                CardanoCompiledTransaction.Input(
                    transactionID = maybeTransactionId.bytes.toHexString(),
                    index = maybeIndex.value.toLong(),
                )
            } else {
                null
            }
        }
    }

    private fun convertOutputs(outputsData: DataItem): List<CardanoCompiledTransaction.Output> {
        if (outputsData !is Array) return emptyList()

        return outputsData.dataItems.mapNotNull { item ->
            if (item is Array) {
                val maybeTransactionId = item.dataItems.firstOrNull() as? ByteString ?: return emptyList()
                val maybeIndex = item.dataItems[1] as? UnsignedInteger ?: return emptyList()
                CardanoCompiledTransaction.Output(
                    address = maybeTransactionId.bytes.toHexString(),
                    amount = maybeIndex.value.toLong(),
                )
            } else {
                null
            }
        }
    }

    private fun convertFee(feeData: DataItem): BigDecimal {
        return (feeData as? UnsignedInteger)?.value.toBigDecimalOrDefault()
    }

    private fun convertCertificates(certsData: DataItem): List<CardanoCompiledTransaction.Certificate> {
        if (certsData !is Array || certsData.tag.value != TAG_VALUE) return emptyList()

        val certificates = certsData.dataItems.map { item ->
            if (item is Array) {
                val index = (item.dataItems.firstOrNull() as? UnsignedInteger)?.value
                if (index != null) {
                    val certificateIndex = CertificateIndex.entries[index.toInt()]
                    when (certificateIndex) {
                        CertificateIndex.StakeRegistrationLegacy -> item.getRegistrationLegacy()
                        CertificateIndex.StakeDelegation -> item.getDelegation()
                        CertificateIndex.StakeDeregistrationLegacy -> item.getDeregistrationLegacy()
                        CertificateIndex.StakeDeregistrationConway -> item.getDeregistrationConway()
                        else -> null
                    }
                } else {
                    null
                }
            } else {
                null
            }
        }

        return certificates.filterNotNull()
    }

    private fun convertWithdrawals(withdrawalsData: DataItem): List<Long> {
        val withdrawals = withdrawalsData as? Array ?: return emptyList()

        return withdrawals.dataItems.mapNotNull {
            (it as? UnsignedInteger)?.value?.toLong()
        }
    }

    private fun Array.getRegistrationLegacy(): CardanoCompiledTransaction.Certificate.StakeRegistrationLegacy? {
        val credentials = (dataItems[1] as? Array)?.dataItems
        val keyHashArray = (credentials?.get(1) as? ByteString)?.bytes

        return if (keyHashArray != null && keyHashArray.size == KEY_HASH_LENGTH) {
            CardanoCompiledTransaction.Certificate.StakeRegistrationLegacy(
                credential = CardanoCompiledTransaction.Credential(keyHash = keyHashArray),
            )
        } else {
            null
        }
    }

    private fun Array.getDelegation(): CardanoCompiledTransaction.Certificate.StakeDelegation? {
        val credentials = (dataItems[1] as? Array)?.dataItems
        val poolKeyHash = (dataItems[2] as? ByteString)?.bytes
        val keyHashArray = (credentials?.get(1) as? ByteString)?.bytes

        return if (poolKeyHash != null && keyHashArray != null && keyHashArray.size == KEY_HASH_LENGTH) {
            CardanoCompiledTransaction.Certificate.StakeDelegation(
                credential = CardanoCompiledTransaction.Credential(keyHash = keyHashArray),
                poolKeyHash = poolKeyHash,
            )
        } else {
            null
        }
    }

    private fun Array.getDeregistrationLegacy(): CardanoCompiledTransaction.Certificate.StakeDeregistrationLegacy? {
        val credentials = (dataItems[1] as? Array)?.dataItems
        val keyHashArray = (credentials?.get(1) as? ByteString)?.bytes

        return if (keyHashArray != null && keyHashArray.size == KEY_HASH_LENGTH) {
            CardanoCompiledTransaction.Certificate.StakeDeregistrationLegacy(
                credential = CardanoCompiledTransaction.Credential(keyHash = keyHashArray),
            )
        } else {
            null
        }
    }

    private fun Array.getDeregistrationConway(): CardanoCompiledTransaction.Certificate.StakeDeregistrationConway? {
        val credentials = (dataItems[1] as? Array)?.dataItems
        val keyHashArray = (credentials?.get(1) as? ByteString)?.bytes
        val coinAmount = (credentials?.get(2) as? UnsignedInteger)?.value?.toLong()

        return if (keyHashArray != null && coinAmount != null && keyHashArray.size == KEY_HASH_LENGTH) {
            CardanoCompiledTransaction.Certificate.StakeDeregistrationConway(
                credential = CardanoCompiledTransaction.Credential(keyHash = keyHashArray),
                coin = coinAmount,
            )
        } else {
            null
        }
    }

    private companion object {
        const val TAG_VALUE = 258L
        const val KEY_HASH_LENGTH = 28
    }
}