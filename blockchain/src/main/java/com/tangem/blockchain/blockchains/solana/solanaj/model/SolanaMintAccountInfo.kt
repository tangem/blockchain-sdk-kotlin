package com.tangem.blockchain.blockchains.solana.solanaj.model

import com.squareup.moshi.Json
import org.p2p.solanaj.rpc.types.RpcResultObject

/**
 * Response model for getAccountInfo on a mint address with jsonParsed encoding.
 * Used to retrieve token extensions like scaledUiAmountConfig.
 */
internal class SolanaMintAccountInfo : RpcResultObject() {

    @Json(name = "value")
    var value: Value? = null

    class Value {
        @Json(name = "data")
        var data: Data? = null

        @Json(name = "owner")
        var owner: String? = null
    }

    class Data {
        @Json(name = "parsed")
        var parsed: ParsedData? = null

        @Json(name = "program")
        var program: String? = null
    }

    class ParsedData {
        @Json(name = "info")
        var info: MintInfo? = null

        @Json(name = "type")
        var type: String? = null
    }

    class MintInfo {
        @Json(name = "decimals")
        var decimals: Int = 0

        @Json(name = "extensions")
        var extensions: List<MintExtension>? = null

        @Json(name = "supply")
        var supply: String? = null
    }

    class MintExtension {
        @Json(name = "extension")
        var extension: String? = null

        @Json(name = "state")
        var state: MintExtensionState? = null
    }

    class MintExtensionState {
        @Json(name = "multiplier")
        var multiplier: String? = null

        @Json(name = "newMultiplier")
        var newMultiplier: String? = null

        @Json(name = "newMultiplierEffectiveTimestamp")
        var newMultiplierEffectiveTimestamp: Long? = null
    }
}