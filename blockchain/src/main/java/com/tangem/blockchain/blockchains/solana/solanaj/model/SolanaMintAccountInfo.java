package com.tangem.blockchain.blockchains.solana.solanaj.model;

import com.squareup.moshi.Json;

import org.p2p.solanaj.rpc.types.RpcResultObject;

import java.util.List;

/**
 * Response model for getAccountInfo on a mint address with jsonParsed encoding.
 * Used to retrieve token extensions like scaledUiAmountConfig.
 */
public class SolanaMintAccountInfo extends RpcResultObject {

    @Json(name = "value")
    public Value value;

    public static class Value {
        @Json(name = "data")
        public Data data;

        @Json(name = "owner")
        public String owner;
    }

    public static class Data {
        @Json(name = "parsed")
        public ParsedData parsed;

        @Json(name = "program")
        public String program;
    }

    public static class ParsedData {
        @Json(name = "info")
        public MintInfo info;

        @Json(name = "type")
        public String type;
    }

    public static class MintInfo {
        @Json(name = "decimals")
        public int decimals;

        @Json(name = "extensions")
        public List<MintExtension> extensions;

        @Json(name = "supply")
        public String supply;
    }

    public static class MintExtension {
        @Json(name = "extension")
        public String extension;

        @Json(name = "state")
        public MintExtensionState state;
    }

    public static class MintExtensionState {
        @Json(name = "multiplier")
        public String multiplier;

        @Json(name = "newMultiplier")
        public String newMultiplier;

        @Json(name = "newMultiplierEffectiveTimestamp")
        public Long newMultiplierEffectiveTimestamp;
    }
}
