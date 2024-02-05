package com.tangem.blockchain.blockchains.solana.solanaj.model;

import com.squareup.moshi.Json;

import org.p2p.solanaj.rpc.types.RpcResultObject;

import java.util.AbstractMap;
import java.util.List;

/**
 * Same as [org.p2p.solanaj.rpc.types.AccountInfo] but with space field and without rentEpoch field
 */
public class NewSolanaAccountInfo extends RpcResultObject {

    @Json(
            name = "value"
    )
    private Value value;

    public NewSolanaAccountInfo() {
    }

    public Value getValue() {
        return this.value;
    }

    public static class Value {
        @Json(
                name = "data"
        )
        private final List<String> data;
        @Json(
                name = "executable"
        )
        private final boolean executable;
        @Json(
                name = "lamports"
        )
        private final long lamports;
        @Json(
                name = "owner"
        )
        private final String owner;
        @Json(
                name = "space"
        )
        private final long space;

        public Value(AbstractMap am) {
            this.data = (List)am.get("data");
            this.executable = (Boolean)am.get("executable");
            this.lamports = (long)am.get("lamports");
            this.owner = (String)am.get("owner");
            this.space = (long) am.get("space");
        }

        public List<String> getData() {
            return this.data;
        }

        public boolean isExecutable() {
            return this.executable;
        }

        public long getLamports() {
            return this.lamports;
        }

        public String getOwner() {
            return this.owner;
        }

        public long getSpace() {
            return this.space;
        }

        public String toString() {
            return "AccountInfo.Value(data=" + this.getData() + ", executable=" + this.isExecutable() + ", lamports=" + this.getLamports() + ", owner=" + this.getOwner() + ", space=" + this.getSpace() + ")";
        }
    }
}
