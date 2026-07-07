package com.tangem.blockchain.common

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import org.junit.Test

/**
 * JSON-RPC 2.0 permits the request `id` to be a String or a Number. Some nodes (e.g. Optimism)
 * reject a string id on eth_* requests, so [JsonRPCRequest.id] is typed `Any` to let the EVM
 * providers send a numeric id while other chains keep their string ids untouched.
 */
class JsonRPCRequestTest {

    private val adapter = Moshi.Builder().build().adapter(JsonRPCRequest::class.java)

    @Test
    fun `GIVEN numeric id WHEN serialized THEN id is a JSON number not a string`() {
        val json = adapter.toJson(
            JsonRPCRequest(
                method = "eth_getTransactionCount",
                params = listOf("0x381DA637230Dd63B23D2F5007CB1B5B77742cC79", "latest"),
                id = 67,
            ),
        )

        assertThat(json).contains("\"id\":67")
        assertThat(json).doesNotContain("\"id\":\"67\"")
    }

    @Test
    fun `GIVEN string id WHEN serialized THEN id stays a JSON string`() {
        val json = adapter.toJson(
            JsonRPCRequest(
                method = "state_getMetadata",
                params = emptyList<Any>(),
                id = "4",
            ),
        )

        assertThat(json).contains("\"id\":\"4\"")
    }
}