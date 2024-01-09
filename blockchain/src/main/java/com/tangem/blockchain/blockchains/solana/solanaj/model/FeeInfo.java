package com.tangem.blockchain.blockchains.solana.solanaj.model;

import com.squareup.moshi.Json;

import org.p2p.solanaj.rpc.types.RpcResultObject;

public class FeeInfo extends RpcResultObject {

    @Json(name = "value")
    private long value;

    public FeeInfo() {
    }

    public long getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "FeeInfo(value=" + value + ")";
    }
}
