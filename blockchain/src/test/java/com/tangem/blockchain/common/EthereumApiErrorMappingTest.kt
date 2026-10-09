package com.tangem.blockchain.common

import com.google.common.truth.Truth
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.network.ResultChecker
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
internal class EthereumApiErrorMappingTest(private val model: Model) {

    @Test
    fun mapsJsonRpcErrorToEthereumError() {
        val actual = BlockchainSdkError.Ethereum.getApiErrorByCode(code = model.code, message = model.message)

        Truth.assertThat(actual).isInstanceOf(BlockchainSdkError.Ethereum.Api::class.java)
        Truth.assertThat(actual is BlockchainSdkError.Ethereum.ExecutionReverted).isEqualTo(model.isReverted)
        Truth.assertThat(actual.code).isEqualTo(BlockchainSdkError.ERROR_CODE_ETHEREUM + model.code)
        Truth.assertThat(actual.customMessage).isEqualTo(model.message)
    }

    @Test
    fun revertDoesNotSwitchProvider() {
        val response = JsonRPCResponse(
            id = "67",
            jsonRpc = "2.0",
            result = null,
            error = JsonRPCResponse.Error(code = model.code, message = model.message),
        )

        val actual = ResultChecker.isNetworkError(Result.Success(response))

        Truth.assertThat(actual).isEqualTo(!model.isReverted)
    }

    data class Model(val code: Int, val message: String, val isReverted: Boolean)

    private companion object {

        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Model> = listOf(
            // [REDACTED_TASK_KEY] log: Polygon token transfer probe for a dust balance
            Model(code = 3, message = "execution reverted: ERC20: transfer amount exceeds balance", isReverted = true),
            // [REDACTED_TASK_KEY]: DEX route revert must not read as insufficient funds
            Model(code = 3, message = "execution reverted: BAL#402", isReverted = true),
            // Some nodes answer a revert with a generic server error code
            Model(code = -32000, message = "Execution reverted", isReverted = true),
            Model(
                code = -32003,
                message = "insufficient funds for gas * price + value: have 1 want 2",
                isReverted = false,
            ),
            Model(code = -32005, message = "rate limit exceeded", isReverted = false),
        )
    }
}