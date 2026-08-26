package com.tangem.blockchain.blockchains.bitcoin.psbt

import com.tangem.blockchain.blockchains.bitcoin.walletconnect.PsbtSignatureApplier
import com.tangem.blockchain.blockchains.bitcoin.walletconnect.models.SignInput
import com.tangem.blockchain.common.psbt.PsbtSighash
import com.tangem.blockchain.extensions.Result
import com.tangem.common.extensions.hexToBytes
import fr.acinq.bitcoin.ByteVector
import fr.acinq.bitcoin.PublicKey
import fr.acinq.bitcoin.psbt.Input
import org.junit.Assert.assertTrue
import org.junit.Test

class PsbtSignatureApplierLegacyTest {

    @Test
    fun `finalizes a legacy non-witness input into a scriptSig`() {
        val psbt = PsbtTestFixtures.singleP2pkhLegacyInputPsbt()
        val applier = PsbtSignatureApplier()
        val signInputs = listOf(SignInput(PsbtTestFixtures.legacyOwnerAddress, 0, listOf(PsbtSighash.ALL)))

        val signed = (
            applier.applySignatures(
                psbt = psbt,
                signatures = listOf(PsbtTestFixtures.dummySignature64),
                signInputs = signInputs,
                inputIndices = listOf(0),
                publicKey = PsbtTestFixtures.ownerPublicKey,
                sighashByte = PsbtSighash.ALL,
            ) as Result.Success
            ).data

        val finalized = applier.finalizePsbt(signed, listOf(0))

        assertTrue(finalized.inputs[0] is Input.NonWitnessInput.FinalizedNonWitnessInput)
    }

    @Test
    fun `does not finalize a legacy input carrying multiple partial signatures`() {
        // A multisig-shaped legacy input must not be auto-finalized into a single-sig P2PKH scriptSig.
        val base = PsbtTestFixtures.singleP2pkhLegacyInputPsbt()
        val input = base.inputs[0] as Input.NonWitnessInput.PartiallySignedNonWitnessInput
        val secondPubKey = "0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798".hexToBytes()
        val dummyDer = ByteVector(byteArrayOf(0x30, 0x02, 0x01, 0x01))
        val twoSigInput = input.copy(
            partialSigs = mapOf(
                PublicKey(ByteVector(PsbtTestFixtures.ownerPublicKey)) to dummyDer,
                PublicKey(ByteVector(secondPubKey)) to dummyDer,
            ),
        )
        val psbt = base.copy(inputs = listOf(twoSigInput))

        val finalized = PsbtSignatureApplier().finalizePsbt(psbt, listOf(0))

        assertTrue(finalized.inputs[0] is Input.NonWitnessInput.PartiallySignedNonWitnessInput)
    }

    @Test
    fun `witness input finalization still works after legacy support is added`() {
        val psbt = PsbtTestFixtures.singleP2wpkhInputPsbt()
        val applier = PsbtSignatureApplier()
        val signInputs = listOf(SignInput("unused", 0, listOf(PsbtSighash.ALL)))

        val signed = (
            applier.applySignatures(
                psbt = psbt,
                signatures = listOf(PsbtTestFixtures.dummySignature64),
                signInputs = signInputs,
                inputIndices = listOf(0),
                publicKey = PsbtTestFixtures.ownerPublicKey,
            ) as Result.Success
            ).data

        val finalized = applier.finalizePsbt(signed, listOf(0))

        assertTrue(finalized.inputs[0] is Input.WitnessInput.FinalizedWitnessInput)
    }
}