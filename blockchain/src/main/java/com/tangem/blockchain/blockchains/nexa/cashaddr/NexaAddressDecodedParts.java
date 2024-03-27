package com.tangem.blockchain.blockchains.nexa.cashaddr;

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

    public void setHash(byte[] hash) {
        this.hash = hash;
    }
}