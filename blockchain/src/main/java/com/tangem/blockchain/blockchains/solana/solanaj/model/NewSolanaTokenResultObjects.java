package com.tangem.blockchain.blockchains.solana.solanaj.model;

import com.squareup.moshi.Json;

import org.p2p.solanaj.rpc.types.TokenResultObjects;

import java.util.AbstractMap;

/** Same as [org.p2p.solanaj.rpc.types.TokenResultObjects] but without rentEpoch field */
public class NewSolanaTokenResultObjects {

    public NewSolanaTokenResultObjects() {
    }

    public static class Value {
        @Json(
                name = "data"
        )
        private TokenResultObjects.Data data;
        @Json(
                name = "executable"
        )
        private boolean executable;
        @Json(
                name = "lamports"
        )
        private long lamports;
        @Json(
                name = "owner"
        )
        private String owner;

        public Value() {
        }

        public TokenResultObjects.Data getData() {
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

        public String toString() {
            return "TokenResultObjects.Value(data=" + this.getData() + ", executable=" + this.isExecutable() + ", lamports=" + this.getLamports() + ", owner=" + this.getOwner() + ")";
        }
    }

    public static class Data {
        @Json(
                name = "parsed"
        )
        private TokenResultObjects.ParsedData parsed;
        @Json(
                name = "program"
        )
        private String program;
        @Json(
                name = "space"
        )
        private Integer space;

        public Data() {
        }

        public TokenResultObjects.ParsedData getParsed() {
            return this.parsed;
        }

        public String getProgram() {
            return this.program;
        }

        public Integer getSpace() {
            return this.space;
        }

        public String toString() {
            return "TokenResultObjects.Data(parsed=" + this.getParsed() + ", program=" + this.getProgram() + ", space=" + this.getSpace() + ")";
        }
    }

    public static class ParsedData {
        @Json(
                name = "info"
        )
        private TokenResultObjects.TokenInfo info;
        @Json(
                name = "type"
        )
        private String type;

        public ParsedData() {
        }

        public TokenResultObjects.TokenInfo getInfo() {
            return this.info;
        }

        public String getType() {
            return this.type;
        }

        public String toString() {
            return "TokenResultObjects.ParsedData(info=" + this.getInfo() + ", type=" + this.getType() + ")";
        }
    }

    public static class TokenInfo {
        @Json(
                name = "isNative"
        )
        private Boolean isNative;
        @Json(
                name = "mint"
        )
        private String mint;
        @Json(
                name = "owner"
        )
        private String owner;
        @Json(
                name = "state"
        )
        private String state;
        @Json(
                name = "tokenAmount"
        )
        private TokenResultObjects.TokenAmountInfo tokenAmount;

        public TokenInfo() {
        }

        public Boolean getIsNative() {
            return this.isNative;
        }

        public String getMint() {
            return this.mint;
        }

        public String getOwner() {
            return this.owner;
        }

        public String getState() {
            return this.state;
        }

        public TokenResultObjects.TokenAmountInfo getTokenAmount() {
            return this.tokenAmount;
        }

        public String toString() {
            return "TokenResultObjects.TokenInfo(isNative=" + this.getIsNative() + ", mint=" + this.getMint() + ", owner=" + this.getOwner() + ", state=" + this.getState() + ", tokenAmount=" + this.getTokenAmount() + ")";
        }
    }

    public static class TokenAccount extends TokenResultObjects.TokenAmountInfo {
        @Json(
                name = "address"
        )
        private String address;

        public TokenAccount(AbstractMap am) {
            super(am);
            this.address = (String)am.get("address");
        }

        public String getAddress() {
            return this.address;
        }

        public String toString() {
            return "TokenResultObjects.TokenAccount(address=" + this.getAddress() + ")";
        }

        public TokenAccount() {
        }
    }

    public static class TokenAmountInfo {
        @Json(
                name = "amount"
        )
        private String amount;
        @Json(
                name = "decimals"
        )
        private int decimals;
        @Json(
                name = "uiAmount"
        )
        private Double uiAmount;
        @Json(
                name = "uiAmountString"
        )
        private String uiAmountString;

        public TokenAmountInfo(AbstractMap am) {
            this.amount = (String)am.get("amount");
            this.decimals = (int)am.get("decimals");
            this.uiAmount = (Double)am.get("uiAmount");
            this.uiAmountString = (String)am.get("uiAmountString");
        }

        public String getAmount() {
            return this.amount;
        }

        public int getDecimals() {
            return this.decimals;
        }

        public Double getUiAmount() {
            return this.uiAmount;
        }

        public String getUiAmountString() {
            return this.uiAmountString;
        }

        public String toString() {
            return "TokenResultObjects.TokenAmountInfo(amount=" + this.getAmount() + ", decimals=" + this.getDecimals() + ", uiAmount=" + this.getUiAmount() + ", uiAmountString=" + this.getUiAmountString() + ")";
        }

        public TokenAmountInfo() {
        }
    }

}
