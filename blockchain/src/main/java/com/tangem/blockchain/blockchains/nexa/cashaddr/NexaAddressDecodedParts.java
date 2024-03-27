package com.tangem.blockchain.blockchains.nexa.cashaddr;

import java.util.Arrays;

public class NexaAddressDecodedParts {

    String prefix;

    NexaAddressType addressType;

    byte[] hash;

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public NexaAddressType getAddressType() {
        return addressType;
    }

    public void setAddressType(NexaAddressType addressType) {
        this.addressType = addressType;
    }

    public byte[] getHash() {
        return hash;
    }

    /**
     * @return script public key hash (as in the <a href="https://explorer.nexa.org/">explorer</a>)
     */
    public byte[] getScriptPublicKeyHash() {
        return Arrays.copyOfRange(hash, 1, hash.length);
    }

    public void setHash(byte[] hash) {
        this.hash = hash;
    }

}