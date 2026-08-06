package com.tangem.blockchain.blockchains.solana

import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.extensions.Result
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toHexString
import io.ktor.util.*
import org.junit.Assert
import org.junit.Test

internal class SolanaTransactionHelperTest {

    @Test
    fun `isTransactionMessage returns true for legacy transaction message`() {
        // A real legacy transaction with a single signature placeholder; the message is what gets signed.
        val transactionWithSignatures = ("010000000000000000000000000000000000000000000000000000000000000000000" +
            "000000000000000000000000000000000000000000000000000000000000001000709457e1b8faf3cc24" +
            "988c7416acef1066088a46411ad5cedc30a70943e008f5f15f5bb7e96e98ac7113d2dd578044070ede30bb" +
            "61ed0ef4e0df49db168657880ba0000000000000000000000000000000000000000000000000000000000000" +
            "000f2afc06308af48267bcade2d22a6332d9607252d1d88b9d4b9d91f7bce6ec4c506a1d8179137542a983437b" +
            "dfe2a7ab2557f535c8a78722b68a49dc00000000006a1d817a502050b680791e6ce6db88e1e5b7150f61fc6790a" +
            "4eb4d10000000006a7d51718c774c928566398691d5eb68b5eb8a39b4b6d5c73555b210000000006a7d517192c5c512" +
            "18cc94c3d4af17f58daee089ba1fd44e3dbd98a0000000006a7d517193584d0feed9bb3431d13206be544281b57b8566" +
            "cc5375ff400000092ba528ef980924d516fa35a49d228a8d451d89a4a149a8988ad5e15325c09f603020200017a03000" +
            "000457e1b8faf3cc24988c7416acef1066088a46411ad5cedc30a70943e008f5f151e00000000000000643430376563313" +
            "538636638333730363034303235383962393865316464c0c62d0000000000c80000000000000006a1d8179137542a983437b" +
            "dfe2a7ab2557f535c8a78722b68a49dc000000000040201077400000000457e1b8faf3cc24988c7416acef1066088a46411ad" +
            "5cedc30a70943e008f5f15457e1b8faf3cc24988c7416acef1066088a46411ad5cedc30a70943e008f5f1500000000000000000" +
            "000000000000000000000000000000000000000000000000000000000000000000000000000000004060103060805000402000000")
            .hexToBytes()
        val message = SolanaTransactionHelper.removeSignaturesPlaceholders(transactionWithSignatures)

        Assert.assertTrue(SolanaTransactionHelper.isTransactionMessage(message))
    }

    @Test
    fun `isTransactionMessage returns true for legacy transaction message with many accounts`() {
        val base64Tx = """
            AQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAABAAUMMs46T+b22EVEJz+YL3KyQnmwDSeubb+kXIbssdrBP0cSY04PqyhUSpaSmXwmD5AGJGw5jQFoxCpqCoDHyRUFxSt8eazGux7vKsP9SOUVlW6RvjKOHgzob4NNeNuvzhfpZS6g1K0EOXLfR5+hmmDTxEzxqKoayyYGcYnfiZ1Qsq6qtCREEs4S0wdW7gwtgKM/JO72XtKjcayVwSlxrklWXcqVbTpWrpoggVbym3PrMTNp5yhXSpKzGlSXQRPmHEgpzAf6e09X6pdgO8CGfg/Drrn4SDIuIS/8IkJj9opduXUAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAMGRm/lIRcy/+ytunLDm+e8jOW7xfcSayxDmzpAAAAAvOlgzG/+eRqMo2MgDHjNi0Wsr5/o0CTAgpYgGPRF4bIG3fbh12Whk9nL4UbO63msHLSF7V9bN5E6jPWFfv8AqQ3IUXC4UD+LVokSAAMPIAbwFmTcAF0bSlZfZiEI1/9kOLP46LH+8zEXtumAIS0BoHQ9Zp749xF0NoKnfne2xuYDCQsDBQYAAAoHCwQBAtwDMm/ydjP6jbvZnSMBAAAAAAAAAAAAAAAADgAAAIXVCpv3Z0lk7qnryvKmehZePlNf3eWAVAM+bbs/5oZYh8kuHXPtOuasE9/L/bxWnQ1zrbWurRKchhqbv2sBgeUvXFFQygP8CzjBB+Iihl4zxE8wILPFPMszsmKBNOPTWDFIYKzAufJa7Fxape/MuVh+ZlQjpic/4m058tr8x2nPzPSk9t+DpuU9Dlr/R41/2Y1zhiZsS9462lAkycKY4ASXc0HLM5BfAiorKrQxFXdydcGgh0kh8C3/NzTBbbRhMKCSiLB87SH4lTEqpzxvT0fMLUJCkXKPE/DGKiot2sLD1rIuO7Gtc8rkprGPOrUw5bMLv2uCUeLOwJtTr6go36ejCm4w9Tj60rcMISxUIYLTpKwS8CL3xHg6/KxdK1tTMGpR0KZNLoegmWXHmOUEHqoyYsuOYEavhiQ1R9u89T4vLrGH9blZ8ATCVRPGalkZufqN7r/3fN7u28JPUu4YQ8LMKos3HU2IOwMU2G8nQDmzZMphUCXLIPL9vPbmKzfxM44O5lT4Fk0CPB5uncI0NNYGrq+aNwHK95sykF6YRd+7yk+iydYOYPv+S4V2lpAA6+ofnzMesj01lCVrrem2+bIIAAUC8EkCAAgACQNxtwIAAAAAAA==
        """.trimIndent().replace("\n", "")
        val message = SolanaTransactionHelper.removeSignaturesPlaceholders(base64Tx.decodeBase64Bytes())

        Assert.assertTrue(SolanaTransactionHelper.isTransactionMessage(message))
    }

    @Test
    fun `isTransactionMessage returns true for v0 versioned transaction message`() {
        // Synthetic but well-formed v0 message: 0x80 version prefix + header + 2 accounts + blockhash +
        // 1 instruction + 0 address-table lookups, consumed exactly.
        val message = byteArrayOf(0x80.toByte()) + // version prefix (v0)
            byteArrayOf(0x01, 0x00, 0x01) + // header: 1 required signature, 0 readonly signed, 1 readonly unsigned
            byteArrayOf(0x02) + ByteArray(2 * 32) + // 2 account keys
            ByteArray(32) + // recent blockhash
            byteArrayOf(0x01) + // instruction count
            byteArrayOf(0x01) + // program id index
            byteArrayOf(0x01, 0x00) + // 1 account index = [0]
            byteArrayOf(0x03, 0x0A, 0x0B, 0x0C) + // data length 3 + 3 data bytes
            byteArrayOf(0x00) // address table lookup count

        Assert.assertTrue(SolanaTransactionHelper.isTransactionMessage(message))
    }

    @Test
    fun `isTransactionMessage returns false for human-readable sign-in message`() {
        val message = "Sign in to Tangem\nNonce: 8f3a91c0d4".toByteArray()

        Assert.assertFalse(SolanaTransactionHelper.isTransactionMessage(message))
    }

    @Test
    fun `isTransactionMessage returns false for empty and short payloads`() {
        Assert.assertFalse(SolanaTransactionHelper.isTransactionMessage(byteArrayOf()))
        Assert.assertFalse(SolanaTransactionHelper.isTransactionMessage(byteArrayOf(0x01, 0x02, 0x03)))
    }

    @Test
    fun `isTransactionMessage returns false when message has trailing bytes`() {
        val transactionWithSignatures = ("010000000000000000000000000000000000000000000000000000000000000000000" +
            "000000000000000000000000000000000000000000000000000000000000001000709457e1b8faf3cc24" +
            "988c7416acef1066088a46411ad5cedc30a70943e008f5f15f5bb7e96e98ac7113d2dd578044070ede30bb" +
            "61ed0ef4e0df49db168657880ba0000000000000000000000000000000000000000000000000000000000000" +
            "000f2afc06308af48267bcade2d22a6332d9607252d1d88b9d4b9d91f7bce6ec4c506a1d8179137542a983437b" +
            "dfe2a7ab2557f535c8a78722b68a49dc00000000006a1d817a502050b680791e6ce6db88e1e5b7150f61fc6790a" +
            "4eb4d10000000006a7d51718c774c928566398691d5eb68b5eb8a39b4b6d5c73555b210000000006a7d517192c5c512" +
            "18cc94c3d4af17f58daee089ba1fd44e3dbd98a0000000006a7d517193584d0feed9bb3431d13206be544281b57b8566" +
            "cc5375ff400000092ba528ef980924d516fa35a49d228a8d451d89a4a149a8988ad5e15325c09f603020200017a03000" +
            "000457e1b8faf3cc24988c7416acef1066088a46411ad5cedc30a70943e008f5f151e00000000000000643430376563313" +
            "538636638333730363034303235383962393865316464c0c62d0000000000c80000000000000006a1d8179137542a983437b" +
            "dfe2a7ab2557f535c8a78722b68a49dc000000000040201077400000000457e1b8faf3cc24988c7416acef1066088a46411ad" +
            "5cedc30a70943e008f5f15457e1b8faf3cc24988c7416acef1066088a46411ad5cedc30a70943e008f5f1500000000000000000" +
            "000000000000000000000000000000000000000000000000000000000000000000000000000000004060103060805000402000000")
            .hexToBytes()
        val messageWithTrailingByte =
            SolanaTransactionHelper.removeSignaturesPlaceholders(transactionWithSignatures) + 0x00

        Assert.assertFalse(SolanaTransactionHelper.isTransactionMessage(messageWithTrailingByte))
    }

    @Test
    fun `isTransactionMessage returns false when a legacy instruction references an out-of-range account index`() {
        // Well-formed legacy message: header + 2 account keys + blockhash + 1 instruction. Only the instruction's
        // single account index varies between the two cases below, isolating the index-bounds check.
        fun legacyMessageWithAccountIndex(accountIndex: Int): ByteArray =
            byteArrayOf(0x01, 0x00, 0x01) + // header: 1 required signature, 0 readonly signed, 1 readonly unsigned
                byteArrayOf(0x02) + ByteArray(2 * 32) + // 2 account keys
                ByteArray(32) + // recent blockhash
                byteArrayOf(0x01) + // instruction count
                byteArrayOf(0x01) + // program id index (references account 1)
                byteArrayOf(0x01, accountIndex.toByte()) + // 1 account index
                byteArrayOf(0x00) // data length 0

        // Sanity check: with an in-range index (< 2 accounts) the same message is a valid transaction message.
        val validMessage = legacyMessageWithAccountIndex(accountIndex = 0)
        Assert.assertTrue(SolanaTransactionHelper.isTransactionMessage(validMessage))

        // The account index references account #5, past the 2 static accounts — must be rejected.
        val outOfRangeMessage = legacyMessageWithAccountIndex(accountIndex = 5)
        Assert.assertFalse(SolanaTransactionHelper.isTransactionMessage(outOfRangeMessage))
    }

    @Test
    fun test_removeSignaturesPlaceholders_2_placeholders() {
        val transactionWithSignatures = "0200070DB029D4A1DABA852BDD6B73F27FA9DCD303C513F961DBA4626A583B6D21D0E675" +
            "B4B9708B7167F947ACD4BF97CA3C1452DC09B852C9C720CD912894F663D2D664165BFB1C48D1C14F6" +
            "84C6E37F8D77A738DCE5F446E476D72EC049A5251F5099B7791C1EB9E71DF4E70BCDDE813112984D21" +
            "2EACBC2B65B2A529B10F4C167AE9DA5E2AB837012AA9176872040309DC9CF3091DFAA262E3A873ED867" +
            "C0871803030AAFA5AE0D7F739C63CC9E448C0EC75A72BD3437419B8E2A159BD701D663C0C2000000000" +
            "00000000000000000000000000000000000000000000000000000008C97258F4E2489F1BB3D1029148E0" +
            "D830B5A1399DAFF1084048E7BD8DBE9F8590306466FE5211732FFECADBA72C39BE7BC8CE5BBC5F7126B2C" +
            "439B3A40000000C9847712F35CDEE6AF59335101B95ED8BD141182A6DFBF6CBEE178C284456FAAD01B7B146" +
            "3187DC6DDF7CC51B0D7729EE6297541735ED91CCB37B7837B18591AEF6B6E933A4437BBE116733C183BE663AB" +
            "68EC070082AC2C9D643E86B3B7377C06DDF6E1D765A193D9CBE146CEEB79AC1CB485ED5F5B37913A8CF5857EFF0" +
            "0A957DE80DF59DD36BB5C49293EF2EB2E8011B86234A57A3A754290DCCB955861D10508000903400D0300000000000" +
            "7060003000B060C000A0601000509060C100E33449FED4E9E66C0EDAC9A200800000A0801000305090B040C0834847E" +
            "B4FDBFB23B060200020C02000000069D9B0000000000"

        val expectedTransaction = "67AE9DA5E2AB837012AA9176872040309DC9CF3091DFAA262E3A873ED867C0871803030AAFA" +
            "5AE0D7F739C63CC9E448C0EC75A72BD3437419B8E2A159BD701D663C0C200000000000000000000000000000000000000000" +
            "000000000000000000000008C97258F4E2489F1BB3D1029148E0D830B5A1399DAFF1084048E7BD8DBE9F8590306466FE52117" +
            "32FFECADBA72C39BE7BC8CE5BBC5F7126B2C439B3A40000000C9847712F35CDEE6AF59335101B95ED8BD141182A6DFBF6CBE" +
            "E178C284456FAAD01B7B1463187DC6DDF7CC51B0D7729EE6297541735ED91CCB37B7837B18591AEF6B6E933A4437BBE11673" +
            "3C183BE663AB68EC070082AC2C9D643E86B3B7377C06DDF6E1D765A193D9CBE146CEEB79AC1CB485ED5F5B37913A8CF5857EF" +
            "F00A957DE80DF59DD36BB5C49293EF2EB2E8011B86234A57A3A754290DCCB955861D10508000903400D030000000000070600" +
            "03000B060C000A0601000509060C100E33449FED4E9E66C0EDAC9A200800000A0801000305090B040C0834847EB4FDBFB23B0" +
            "60200020C02000000069D9B0000000000"

        val transaction = transactionWithSignatures.hexToBytes()

        val transactionWithRemovedSignatures =
            SolanaTransactionHelper.removeSignaturesPlaceholders(transaction).toHexString()

        Assert.assertEquals(expectedTransaction.lowercase(), transactionWithRemovedSignatures.lowercase())
    }

    @Test
    fun test_removeSignaturesPlaceholders_1_placeholder() {
        val transactionWithSignatures = "010000000000000000000000000000000000000000000000000000000000000000000" +
            "000000000000000000000000000000000000000000000000000000000000001000709457e1b8faf3cc24" +
            "988c7416acef1066088a46411ad5cedc30a70943e008f5f15f5bb7e96e98ac7113d2dd578044070ede30bb" +
            "61ed0ef4e0df49db168657880ba0000000000000000000000000000000000000000000000000000000000000" +
            "000f2afc06308af48267bcade2d22a6332d9607252d1d88b9d4b9d91f7bce6ec4c506a1d8179137542a983437b" +
            "dfe2a7ab2557f535c8a78722b68a49dc00000000006a1d817a502050b680791e6ce6db88e1e5b7150f61fc6790a" +
            "4eb4d10000000006a7d51718c774c928566398691d5eb68b5eb8a39b4b6d5c73555b210000000006a7d517192c5c512" +
            "18cc94c3d4af17f58daee089ba1fd44e3dbd98a0000000006a7d517193584d0feed9bb3431d13206be544281b57b8566" +
            "cc5375ff400000092ba528ef980924d516fa35a49d228a8d451d89a4a149a8988ad5e15325c09f603020200017a03000" +
            "000457e1b8faf3cc24988c7416acef1066088a46411ad5cedc30a70943e008f5f151e00000000000000643430376563313" +
            "538636638333730363034303235383962393865316464c0c62d0000000000c80000000000000006a1d8179137542a983437b" +
            "dfe2a7ab2557f535c8a78722b68a49dc000000000040201077400000000457e1b8faf3cc24988c7416acef1066088a46411ad" +
            "5cedc30a70943e008f5f15457e1b8faf3cc24988c7416acef1066088a46411ad5cedc30a70943e008f5f1500000000000000000" +
            "000000000000000000000000000000000000000000000000000000000000000000000000000000004060103060805000402000000"

        val expectedTransaction = "01000709457e1b8faf3cc24988c7416acef1066088a46411ad5cedc30a70943e008f5f15f5bb7e" +
            "96e98ac7113d2dd578044070ede30bb61ed0ef4e0df49db168657880ba00000000000000000000000000000000000000000" +
            "00000000000000000000000f2afc06308af48267bcade2d22a6332d9607252d1d88b9d4b9d91f7bce6ec4c506a1d8179137" +
            "542a983437bdfe2a7ab2557f535c8a78722b68a49dc00000000006a1d817a502050b680791e6ce6db88e1e5b7150f61fc67" +
            "90a4eb4d10000000006a7d51718c774c928566398691d5eb68b5eb8a39b4b6d5c73555b210000000006a7d517192c5c5121" +
            "8cc94c3d4af17f58daee089ba1fd44e3dbd98a0000000006a7d517193584d0feed9bb3431d13206be544281b57b8566cc537" +
            "5ff400000092ba528ef980924d516fa35a49d228a8d451d89a4a149a8988ad5e15325c09f603020200017a03000000457e1b8" +
            "faf3cc24988c7416acef1066088a46411ad5cedc30a70943e008f5f151e000000000000006434303765633135386366383337" +
            "30363034303235383962393865316464c0c62d0000000000c80000000000000006a1d8179137542a983437bdfe2a7ab2557f5" +
            "35c8a78722b68a49dc000000000040201077400000000457e1b8faf3cc24988c7416acef1066088a46411ad5cedc30a70943" +
            "e008f5f15457e1b8faf3cc24988c7416acef1066088a46411ad5cedc30a70943e008f5f15000000000000000000000000000" +
            "00000000000000000000000000000000000000000000000000000000000000000000004060103060805000402000000"

        val transaction = transactionWithSignatures.hexToBytes()

        val transactionWithRemovedSignatures =
            SolanaTransactionHelper.removeSignaturesPlaceholders(transaction).toHexString()

        Assert.assertEquals(expectedTransaction.lowercase(), transactionWithRemovedSignatures.lowercase())
    }

    // region putSignature

    @Test
    fun `putSignature places signature in the wallet slot preserving other slots and message`() {
        // A two-signer transaction: slot 0 is a co-signer's signature already supplied by the dApp, slot 1 is the
        // zero placeholder for our wallet (signer index 1 — not the fee-payer at index 0).
        val coSignerSignature = ByteArray(SIGNATURE_LENGTH) { 0x11 }
        val ourPlaceholder = ByteArray(SIGNATURE_LENGTH) { 0x00 }
        val ourSignature = ByteArray(SIGNATURE_LENGTH) { 0x77 }

        val transaction = byteArrayOf(0x02) + // signature count = 2
            coSignerSignature + ourPlaceholder +
            TWO_SIGNER_MESSAGE

        val result = SolanaTransactionHelper.putSignature(
            transaction = transaction,
            signerPublicKey = OUR_KEY,
            signature = ourSignature,
        )

        val expected = byteArrayOf(0x02) +
            coSignerSignature + ourSignature + // our signature spliced into slot 1, co-signer's slot untouched
            TWO_SIGNER_MESSAGE
        Assert.assertArrayEquals(expected, result.dataOrFail())
    }

    @Test
    fun `putSignature keeps the signature count and message identical`() {
        val transaction = byteArrayOf(0x02) +
            ByteArray(SIGNATURE_LENGTH) + ByteArray(SIGNATURE_LENGTH) +
            TWO_SIGNER_MESSAGE

        val signed = SolanaTransactionHelper
            .putSignature(transaction, OUR_KEY, ByteArray(SIGNATURE_LENGTH) { 0x77 })
            .dataOrFail()

        Assert.assertEquals(2, signed[0].toInt()) // count unchanged
        Assert.assertArrayEquals(TWO_SIGNER_MESSAGE, SolanaTransactionHelper.extractMessage(signed))
    }

    @Test
    fun `putSignature signs the fee-payer slot when the wallet is signer index 0`() {
        val transaction = byteArrayOf(0x02) +
            ByteArray(SIGNATURE_LENGTH) + ByteArray(SIGNATURE_LENGTH) +
            TWO_SIGNER_MESSAGE
        val feePayerSignature = ByteArray(SIGNATURE_LENGTH) { 0x55 }

        val result = SolanaTransactionHelper.putSignature(transaction, FEE_PAYER_KEY, feePayerSignature)

        val expected = byteArrayOf(0x02) +
            feePayerSignature + ByteArray(SIGNATURE_LENGTH) +
            TWO_SIGNER_MESSAGE
        Assert.assertArrayEquals(expected, result.dataOrFail())
    }

    @Test
    fun `putSignature returns SignerPublicKeyNotFound when the public key is not a required signer`() {
        val transaction = byteArrayOf(0x02) +
            ByteArray(SIGNATURE_LENGTH) + ByteArray(SIGNATURE_LENGTH) +
            TWO_SIGNER_MESSAGE
        val strangerKey = ByteArray(PUBLIC_KEY_LENGTH) { 0xEE.toByte() }

        val result = SolanaTransactionHelper.putSignature(transaction, strangerKey, ByteArray(SIGNATURE_LENGTH))

        Assert.assertTrue(result is Result.Failure)
        Assert.assertTrue((result as Result.Failure).error is BlockchainSdkError.Solana.SignerPublicKeyNotFound)
    }

    @Test
    fun `putSignature does not treat a non-signer account as a signer`() {
        // PROGRAM_KEY is account index 2 — present in the message but not among the two required signers.
        val transaction = byteArrayOf(0x02) +
            ByteArray(SIGNATURE_LENGTH) + ByteArray(SIGNATURE_LENGTH) +
            TWO_SIGNER_MESSAGE

        val result = SolanaTransactionHelper.putSignature(transaction, PROGRAM_KEY, ByteArray(SIGNATURE_LENGTH))

        Assert.assertTrue(result is Result.Failure)
        Assert.assertTrue((result as Result.Failure).error is BlockchainSdkError.Solana.SignerPublicKeyNotFound)
    }

    @Test
    fun `putSignature returns failure for a signature of the wrong length`() {
        val transaction = byteArrayOf(0x02) +
            ByteArray(SIGNATURE_LENGTH) + ByteArray(SIGNATURE_LENGTH) +
            TWO_SIGNER_MESSAGE

        val result = SolanaTransactionHelper.putSignature(transaction, OUR_KEY, ByteArray(SIGNATURE_LENGTH - 1))

        Assert.assertTrue(result is Result.Failure)
    }

    @Test
    fun `putSignature returns failure for an empty transaction`() {
        val result = SolanaTransactionHelper.putSignature(byteArrayOf(), OUR_KEY, ByteArray(SIGNATURE_LENGTH))

        Assert.assertTrue(result is Result.Failure)
        Assert.assertTrue((result as Result.Failure).error is BlockchainSdkError.Solana.TransactionIsEmpty)
    }

    @Test
    fun `extractMessage returns the message of a two-signer transaction`() {
        val transaction = byteArrayOf(0x02) +
            ByteArray(SIGNATURE_LENGTH) + ByteArray(SIGNATURE_LENGTH) +
            TWO_SIGNER_MESSAGE

        Assert.assertArrayEquals(TWO_SIGNER_MESSAGE, SolanaTransactionHelper.extractMessage(transaction))
    }

    // endregion

    private fun Result<ByteArray>.dataOrFail(): ByteArray =
        (this as? Result.Success)?.data ?: error("Expected Result.Success but was $this")

    private companion object {
        const val SIGNATURE_LENGTH = 64
        const val PUBLIC_KEY_LENGTH = 32

        val FEE_PAYER_KEY = ByteArray(PUBLIC_KEY_LENGTH) { 0xA0.toByte() } // signer index 0
        val OUR_KEY = ByteArray(PUBLIC_KEY_LENGTH) { 0xB1.toByte() } // signer index 1
        val PROGRAM_KEY = ByteArray(PUBLIC_KEY_LENGTH) { 0xC2.toByte() } // account index 2, not a signer
        val BLOCKHASH = ByteArray(PUBLIC_KEY_LENGTH) { 0xD3.toByte() }

        /**
         * A well-formed legacy message with 2 required signers and 3 static accounts (fee-payer, our wallet,
         * program), a blockhash and a single instruction referencing accounts 0 and 1.
         */
        val TWO_SIGNER_MESSAGE: ByteArray =
            byteArrayOf(0x02, 0x00, 0x01) + // header: 2 required signatures, 0 readonly signed, 1 readonly unsigned
                byteArrayOf(0x03) + FEE_PAYER_KEY + OUR_KEY + PROGRAM_KEY + // 3 account keys
                BLOCKHASH +
                byteArrayOf(0x01) + // instruction count
                byteArrayOf(0x02) + // program id index -> PROGRAM_KEY
                byteArrayOf(0x02, 0x00, 0x01) + // 2 account indices -> [0, 1]
                byteArrayOf(0x00) // data length 0
    }
}