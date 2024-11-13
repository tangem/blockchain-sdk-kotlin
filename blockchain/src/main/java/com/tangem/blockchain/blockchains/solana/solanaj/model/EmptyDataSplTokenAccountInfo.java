package com.tangem.blockchain.blockchains.solana.solanaj.model;

import com.squareup.moshi.Json;

import org.p2p.solanaj.rpc.types.RpcResultObject;

/**
 * Same as [solanaj.rpc.types.SplTokenAccountInfo] but without rentEpoch field and with empty data
 */
public class EmptyDataSplTokenAccountInfo extends RpcResultObject {
    @Json(
            name = "value"
    )
    private NewSolanaTokenResultObjects.EmptyDataValue value;

    public EmptyDataSplTokenAccountInfo() {
    }

    public NewSolanaTokenResultObjects.EmptyDataValue getValue() {
        return this.value;
    }

    public String toString() {
        return "SplTokenAccountInfo(value=" + this.getValue() + ")";
    }
}
