package com.tangem.blockchain.yieldsupply.providers.ethereum.yield

import com.google.common.truth.Truth
import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionExtras
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.smartcontract.CompiledSmartContractCallData
import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toHexString
import io.mockk.mockk
import org.junit.Test

internal class EthereumYieldSupplyUpgradeToAndCallCallDataTest {
    private val blockchain = Blockchain.Ethereum
    private val newImplementation = "0xabcdefabcdefabcdefabcdefabcdefabcdefabcd"
    private val innerCallData = CompiledSmartContractCallData("0xdeadbeef".hexToBytes())
    private val sentAmount = mockk<Amount>(relaxed = true)
    private val encodedCallData = "0x4f1ef286" +
        "000000000000000000000000abcdefabcdefabcdefabcdefabcdefabcdefabcd" +
        "0000000000000000000000000000000000000000000000000000000000000040" +
        "0000000000000000000000000000000000000000000000000000000000000004" +
        "deadbeef00000000000000000000000000000000000000000000000000000000"

    @Test
    fun `Is call data correct`() {
        val callData = EthereumYieldSupplyUpgradeToAndCallCallData(
            newImplementation = newImplementation,
            innerCallData = innerCallData,
        )
        Truth.assertThat(callData.data).isEqualTo(encodedCallData.hexToBytes())
    }

    @Test
    fun `Validate call data`() {
        val valid = EthereumYieldSupplyUpgradeToAndCallCallData(
            newImplementation = newImplementation,
            innerCallData = innerCallData,
        )
        Truth.assertThat(valid.validate(blockchain)).isTrue()

        val invalid = EthereumYieldSupplyUpgradeToAndCallCallData(
            newImplementation = EthereumUtils.ZERO_ADDRESS,
            innerCallData = innerCallData,
        )
        Truth.assertThat(invalid.validate(blockchain)).isFalse()
    }

    @Test
    fun `WHEN inner call data is invalid THEN validation fails`() {
        val callData = EthereumYieldSupplyUpgradeToAndCallCallData(
            newImplementation = newImplementation,
            innerCallData = EthereumYieldSupplyEnterCallData(tokenContractAddress = EthereumUtils.ZERO_ADDRESS),
        )

        Truth.assertThat(callData.validate(blockchain)).isFalse()
    }

    @Test
    fun `WHEN wrapped THEN unwrap returns inner call data`() {
        val callData = EthereumYieldSupplyUpgradeToAndCallCallData(
            newImplementation = newImplementation,
            innerCallData = innerCallData,
        )

        Truth.assertThat(callData.unwrapYieldModuleUpgrade()).isSameInstanceAs(innerCallData)
        Truth.assertThat(innerCallData.unwrapYieldModuleUpgrade()).isSameInstanceAs(innerCallData)
    }

    @Test
    fun `WHEN correct data THEN return RESULT`() {
        val actual = EthereumYieldSupplyUpgradeToAndCallCallData.decode(encodedCallData)

        Truth.assertThat(actual).isNotNull()
        Truth.assertThat(actual!!.newImplementation).isEqualTo(newImplementation)
        Truth.assertThat(actual.innerCallData.data.toHexString().lowercase()).isEqualTo("deadbeef")
        Truth.assertThat(actual.data).isEqualTo(encodedCallData.hexToBytes())
    }

    @Test
    fun `WHEN wrong method data THEN return NULL`() {
        val rawCallData = "0x79be55f7" + encodedCallData.removePrefix("0x4f1ef286")

        Truth.assertThat(EthereumYieldSupplyUpgradeToAndCallCallData.decode(rawCallData)).isNull()
    }

    @Test
    fun `WHEN truncated data THEN return NULL`() {
        val rawCallData = encodedCallData.dropLast(64)

        Truth.assertThat(EthereumYieldSupplyUpgradeToAndCallCallData.decode(rawCallData)).isNull()
    }

    @Test
    fun `WHEN length exceeds data THEN return NULL`() {
        val rawCallData = encodedCallData.replace(
            "0000000000000000000000000000000000000000000000000000000000000004",
            "00000000000000000000000000000000000000000000000000000000000000ff",
        )

        Truth.assertThat(EthereumYieldSupplyUpgradeToAndCallCallData.decode(rawCallData)).isNull()
    }

    @Test
    fun `WHEN length overflows int THEN return NULL`() {
        val rawCallData = encodedCallData.replace(
            "0000000000000000000000000000000000000000000000000000000000000004",
            "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff",
        )

        Truth.assertThat(EthereumYieldSupplyUpgradeToAndCallCallData.decode(rawCallData)).isNull()
    }

    @Test
    fun `WHEN unexpected data offset THEN return NULL`() {
        val rawCallData = encodedCallData.replace(
            "0000000000000000000000000000000000000000000000000000000000000040",
            "0000000000000000000000000000000000000000000000000000000000000060",
        )

        Truth.assertThat(EthereumYieldSupplyUpgradeToAndCallCallData.decode(rawCallData)).isNull()
    }

    @Test
    fun `WHEN inner call is shorter than selector THEN return NULL`() {
        val rawCallData = "0x4f1ef286" +
            "000000000000000000000000abcdefabcdefabcdefabcdefabcdefabcdefabcd" +
            "0000000000000000000000000000000000000000000000000000000000000040" +
            "0000000000000000000000000000000000000000000000000000000000000000"

        Truth.assertThat(EthereumYieldSupplyUpgradeToAndCallCallData.decode(rawCallData)).isNull()
    }

    @Test
    fun `WHEN implementation starts with zero byte THEN decode keeps the full address`() {
        val implementation = "0x00ababababababababababababababababababab"
        val rawCallData = EthereumYieldSupplyUpgradeToAndCallCallData(
            newImplementation = implementation,
            innerCallData = innerCallData,
        ).dataHex

        val decoded = EthereumYieldSupplyUpgradeToAndCallCallData.decode(rawCallData)

        Truth.assertThat(decoded?.newImplementation).isEqualTo(implementation)
    }

    @Test
    fun `WHEN transaction carries upgrade-wrapped call THEN unwrapped transaction carries inner call`() {
        val wrapped = EthereumYieldSupplyUpgradeToAndCallCallData(
            newImplementation = newImplementation,
            innerCallData = innerCallData,
        )

        val actual = uncompiledWithCallData(wrapped).unwrapYieldModuleUpgrade()

        Truth.assertThat(actual).isEqualTo(uncompiledWithCallData(innerCallData))
    }

    @Test
    fun `WHEN transaction carries bare call THEN same transaction is returned`() {
        val transaction = uncompiledWithCallData(innerCallData)

        Truth.assertThat(transaction.unwrapYieldModuleUpgrade()).isSameInstanceAs(transaction)
    }

    @Test
    fun `WHEN extras have no call data THEN same transaction is returned`() {
        val transaction = uncompiledWithCallData(callData = null)

        Truth.assertThat(transaction.unwrapYieldModuleUpgrade()).isSameInstanceAs(transaction)
    }

    @Test
    fun `WHEN transaction has no extras THEN same transaction is returned`() {
        val transaction = TransactionData.Uncompiled(
            amount = sentAmount,
            fee = null,
            sourceAddress = SOURCE_ADDRESS,
            destinationAddress = newImplementation,
        )

        Truth.assertThat(transaction.unwrapYieldModuleUpgrade()).isSameInstanceAs(transaction)
    }

    @Test
    fun `WHEN transaction is compiled THEN same transaction is returned`() {
        val transaction = TransactionData.Compiled(value = TransactionData.Compiled.Data.RawString(data = "0x"))

        Truth.assertThat(transaction.unwrapYieldModuleUpgrade()).isSameInstanceAs(transaction)
    }

    private fun uncompiledWithCallData(callData: SmartContractCallData?) = TransactionData.Uncompiled(
        amount = sentAmount,
        fee = null,
        sourceAddress = SOURCE_ADDRESS,
        destinationAddress = newImplementation,
        extras = EthereumTransactionExtras(callData = callData),
    )

    private companion object {
        const val SOURCE_ADDRESS = "0x1111111111111111111111111111111111111111"
    }
}