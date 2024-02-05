package com.tangem.blockchain.blockchains.solana.solanaj.model;

import com.squareup.moshi.Json;

import org.p2p.solanaj.rpc.types.RpcResultObject;

import java.util.List;

public class NewSolanaTokenAccountInfo extends RpcResultObject {

    @Json(
            name = "value"
    )
    private List<Value> value;

    public NewSolanaTokenAccountInfo() {
    }

    public List<Value> getValue() {
        return this.value;
    }

    public static class Value {
        @Json(
                name = "account"
        )
        private NewSolanaTokenResultObjects.Value account;
        @Json(
                name = "pubkey"
        )
        private String pubkey;

        public Value() {
        }

        public NewSolanaTokenResultObjects.Value getAccount() {
            return this.account;
        }

        public String getPubkey() {
            return this.pubkey;
        }
    }
}
