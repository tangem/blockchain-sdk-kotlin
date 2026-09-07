package com.tangem.blockchain.blockchains.ethereum.network

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.JsonRPCResponse
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.network.MultiNetworkProvider
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Test

/**
 * The raw contract reads of the shared EVM service: `eth_getCode` tells a counterfactual Safe from a
 * deployed one, the raw `eth_call` serves reads whose ABI shape the SDK does not know (e.g. `getOwners()`
 * returning `address[]`). Both return the raw hex untouched — decoding belongs to the caller.
 */
internal class EthereumContractReadsTest {

    private val rpcProvider: EthereumJsonRpcProvider = mockk {
        every { baseUrl } returns "https://rpc.example/"
    }

    private val service = EthereumNetworkService(
        multiJsonRpcProvider = MultiNetworkProvider(
            providers = listOf(rpcProvider),
            blockchain = Blockchain.Ethereum,
        ),
    )

    @Test
    fun `GIVEN no contract at the address WHEN getCode THEN the raw 0x is returned untouched`() = runBlocking {
        coEvery { rpcProvider.getCode(SAFE_ADDRESS) } returns Result.Success(response(result = "0x"))

        val actual = service.getCode(SAFE_ADDRESS)

        assertThat((actual as Result.Success).data).isEqualTo("0x")
    }

    @Test
    fun `GIVEN a deployed contract WHEN getCode THEN its bytecode is returned untouched`() = runBlocking {
        coEvery { rpcProvider.getCode(SAFE_ADDRESS) } returns Result.Success(response(result = "0x6080604052"))

        val actual = service.getCode(SAFE_ADDRESS)

        assertThat((actual as Result.Success).data).isEqualTo("0x6080604052")
    }

    @Test
    fun `GIVEN the node answers an error WHEN getCode THEN a failure instead of a throw`() = runBlocking {
        coEvery { rpcProvider.getCode(SAFE_ADDRESS) } returns Result.Success(
            response(result = null, error = JsonRPCResponse.Error(code = -32000, message = "node is syncing")),
        )

        val actual = service.getCode(SAFE_ADDRESS)

        assertThat(actual).isInstanceOf(Result.Failure::class.java)
    }

    @Test
    fun `GIVEN a contract read WHEN callContract THEN the raw abi hex is returned undecoded`() = runBlocking {
        val ownersAbi = "0x" +
            "0000000000000000000000000000000000000000000000000000000000000020" +
            "0000000000000000000000000000000000000000000000000000000000000001" +
            "0000000000000000000000007e5f4552091a69125d5dfcb7b8c2659029395bdf"
        val callData = ContractCallData(to = SAFE_ADDRESS, data = GET_OWNERS_SELECTOR)
        coEvery { rpcProvider.call(callData) } returns Result.Success(response(result = ownersAbi))

        val actual = service.callContract(callData)

        assertThat((actual as Result.Success).data).isEqualTo(ownersAbi)
    }

    @Test
    fun `GIVEN the provider fails WHEN callContract THEN a failure instead of a throw`() = runBlocking {
        val callData = ContractCallData(to = SAFE_ADDRESS, data = GET_OWNERS_SELECTOR)
        coEvery { rpcProvider.call(callData) } returns Result.Failure(
            BlockchainSdkError.CustomError("no connection"),
        )

        val actual = service.callContract(callData)

        assertThat(actual).isInstanceOf(Result.Failure::class.java)
    }

    @Test
    fun `GIVEN the method registry WHEN read THEN the rpc method names are pinned per chain family`() {
        assertThat(EthereumMethod.getCode).isEqualTo("eth_getCode")
        assertThat(QuaiMethod.getCode).isEqualTo("quai_getCode")
    }

    @Test
    fun `GIVEN a provider without the reads WHEN called THEN the defaults fail instead of forcing overrides`() =
        runBlocking {
            val bareProvider = mockk<EthereumNetworkProvider>()
            coEvery { bareProvider.getCode(any()) } answers { callOriginal() }
            coEvery { bareProvider.callContract(any()) } answers { callOriginal() }

            val code = bareProvider.getCode(SAFE_ADDRESS)
            val call = bareProvider.callContract(ContractCallData(to = SAFE_ADDRESS, data = GET_OWNERS_SELECTOR))

            assertThat(code).isInstanceOf(Result.Failure::class.java)
            assertThat(call).isInstanceOf(Result.Failure::class.java)
        }

    private fun response(result: Any?, error: JsonRPCResponse.Error? = null) = JsonRPCResponse(
        id = "1",
        jsonRpc = "2.0",
        result = result,
        error = error,
    )

    private companion object {
        const val SAFE_ADDRESS = "0x5afe000000000000000000000000000000000001"
        const val GET_OWNERS_SELECTOR = "0xa0e67e2b"
    }
}