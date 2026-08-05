package com.tangem.blockchain.blockchains.ethereum.tokenmethods

import com.google.common.truth.Truth
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import org.junit.Test

/**
 * Test for the [TransferERC20TokenCallData] decoder
 */
internal class TransferERC20TokenCallDataTest {

    private val destination = "0x5678901234567890123456789012345678901234"
    private val destinationWord = "0000000000000000000000005678901234567890123456789012345678901234"
    private val amountWord = "0000000000000000000000000000000000000000000000056bc75e2d63100000"
    private val rawAmount = "100000000000000000000".toBigDecimal()

    private val validCallData = "0xa9059cbb$destinationWord$amountWord"

    // transferFrom(address,address,uint256), padded to the very same length as a valid transfer
    private val foreignMethodIdCallData = "0x23b872dd$destinationWord$amountWord"

    // a non-zero byte inside the 12 high-order bytes of the address word
    private val dirtyPaddingWord = "0000000000000000000000015678901234567890123456789012345678901234"
    private val zeroDestinationWord = "0".repeat(n = 64)

    // an address whose own leading byte is zero - it must survive decoding as 20 bytes
    private val leadingZeroDestination = "0x00ab567890123456789012345678901234567890"
    private val leadingZeroDestinationWord = "00000000000000000000000000ab567890123456789012345678901234567890"

    @Test
    fun `WHEN correct call data THEN return decoded transfer`() {
        val actual = TransferERC20TokenCallData(validCallData)

        Truth.assertThat(actual).isNotNull()
        Truth.assertThat(actual!!.destination).isEqualTo(destination)
        Truth.assertThat(actual.amount.value).isEqualTo(rawAmount)
        Truth.assertThat(actual.methodId).isEqualTo(TransferERC20TokenCallData.METHOD_ID)
    }

    @Test
    fun `WHEN correct call data THEN decoded amount is in raw minimal units`() {
        val actual = TransferERC20TokenCallData(validCallData)

        // call data carries no token decimals, so the decoded amount is raw and re-encodes to the same bytes
        Truth.assertThat(actual).isNotNull()
        Truth.assertThat(actual!!.amount.decimals).isEqualTo(0)
    }

    @Test
    fun `WHEN encoded transfer is decoded THEN data round trips`() {
        val encoded = TransferERC20TokenCallData(
            destination = destination,
            amount = Amount(Blockchain.Ethereum).copy(value = "100".toBigDecimal()),
        ).data

        val decoded = TransferERC20TokenCallData(encoded)

        Truth.assertThat(decoded).isNotNull()
        Truth.assertThat(decoded!!.data).isEqualTo(encoded)
        Truth.assertThat(decoded.destination).isEqualTo(destination)
    }

    @Test
    fun `WHEN address has leading zero byte THEN it is kept intact`() {
        val actual = TransferERC20TokenCallData("0xa9059cbb$leadingZeroDestinationWord$amountWord")

        Truth.assertThat(actual).isNotNull()
        Truth.assertThat(actual!!.destination).isEqualTo(leadingZeroDestination)
    }

    @Test
    fun `WHEN foreign method id THEN return NULL`() {
        Truth.assertThat(TransferERC20TokenCallData(foreignMethodIdCallData)).isNull()
    }

    @Test
    fun `WHEN call data is truncated THEN return NULL`() {
        // the amount word is missing entirely
        Truth.assertThat(TransferERC20TokenCallData("0xa9059cbb$destinationWord")).isNull()
        // one byte short
        Truth.assertThat(TransferERC20TokenCallData(validCallData.dropLast(n = 2))).isNull()
        // odd hex length
        Truth.assertThat(TransferERC20TokenCallData(validCallData.dropLast(n = 1))).isNull()
        // the method id alone
        Truth.assertThat(TransferERC20TokenCallData("0xa9059cbb")).isNull()
        // nothing at all
        Truth.assertThat(TransferERC20TokenCallData("")).isNull()
    }

    @Test
    fun `WHEN call data has trailing bytes THEN return NULL`() {
        Truth.assertThat(TransferERC20TokenCallData(validCallData + "00")).isNull()
        Truth.assertThat(TransferERC20TokenCallData(validCallData + amountWord)).isNull()
        // a single trailing nibble: hexToBytes() would silently drop it and yield a valid 68 bytes,
        // so only the odd length check rejects this one
        Truth.assertThat(TransferERC20TokenCallData(validCallData + "0")).isNull()
    }

    @Test
    fun `WHEN address padding is dirty THEN return NULL`() {
        Truth.assertThat(TransferERC20TokenCallData("0xa9059cbb$dirtyPaddingWord$amountWord")).isNull()
    }

    @Test
    fun `WHEN destination is zero address THEN return NULL`() {
        Truth.assertThat(TransferERC20TokenCallData("0xa9059cbb$zeroDestinationWord$amountWord")).isNull()
    }

    @Test
    fun `WHEN call data is not hex THEN return NULL`() {
        Truth.assertThat(TransferERC20TokenCallData("0xa9059cbz$destinationWord$amountWord")).isNull()
        Truth.assertThat(TransferERC20TokenCallData("not a hex string at all")).isNull()
        // Integer.parseInt() accepts a sign, so hexToBytes() would turn these pairs into 0xff bytes and
        // report success - only the hex digit check rejects this one
        Truth.assertThat(TransferERC20TokenCallData("0xa9059cbb$destinationWord${"-1".repeat(n = 32)}")).isNull()
    }

    @Test
    fun `WHEN any hex prefix is used THEN decoding is the same`() {
        val body = "a9059cbb$destinationWord$amountWord"

        val lowercasePrefix = TransferERC20TokenCallData("0x$body")
        val uppercasePrefix = TransferERC20TokenCallData("0X$body")
        val noPrefix = TransferERC20TokenCallData(body)

        Truth.assertThat(lowercasePrefix).isNotNull()
        Truth.assertThat(uppercasePrefix).isEqualTo(lowercasePrefix)
        Truth.assertThat(noPrefix).isEqualTo(lowercasePrefix)
    }

    @Test
    fun `WHEN method id is uppercase THEN it is still recognized`() {
        val actual = TransferERC20TokenCallData("0xA9059CBB$destinationWord$amountWord")

        Truth.assertThat(actual).isNotNull()
        Truth.assertThat(actual!!.destination).isEqualTo(destination)
    }
}