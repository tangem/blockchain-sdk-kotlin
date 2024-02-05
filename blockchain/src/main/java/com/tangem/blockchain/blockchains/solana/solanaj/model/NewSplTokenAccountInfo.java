package com.tangem.blockchain.blockchains.solana.solanaj.model;

import com.squareup.moshi.Json;

import org.p2p.solanaj.rpc.types.RpcResultObject;

/** Same as [solanaj.rpc.types.SplTokenAccountInfo] but without rentEpoch field */
public class NewSplTokenAccountInfo extends RpcResultObject {
    @Json(
        name = "value"
    )
    private NewSolanaTokenResultObjects.Value value;

    public NewSplTokenAccountInfo() {
    }

    public NewSolanaTokenResultObjects.Value getValue() {
        return this.value;
    }

    public String toString() {
        return "SplTokenAccountInfo(value=" + this.getValue() + ")";
    }
}
