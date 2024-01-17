package com.tangem.blockchain.blockchains.solana.solanaj.model;

import com.squareup.moshi.Json;

import org.p2p.solanaj.rpc.types.AccountInfo;

import java.util.AbstractMap;

/**
 * Same as [org.p2p.solanaj.rpc.types.AccountInfo] but with space field
 */
public class SolanaAccountInfo {

    @Json(
            name = "value"
    )
    private Value value;

    public SolanaAccountInfo() {
    }

    public Value getValue() {
        return this.value;
    }

    public static class Value extends AccountInfo.Value {

        @Json(
                name = "space"
        )
        private final long space;

        public Value(AbstractMap am) {
            super(am);
            this.space = (long) am.get("space");
        }

        public long getSpace() {
            return this.space;
        }
    }
}
