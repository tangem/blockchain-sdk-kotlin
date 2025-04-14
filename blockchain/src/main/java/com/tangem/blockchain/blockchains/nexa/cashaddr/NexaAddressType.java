package com.tangem.blockchain.blockchains.nexa.cashaddr;

public enum NexaAddressType {

    P2PKH((byte) 0),

    /**
     * Not supported.
     * Nexa developers also don't support it according to the library
     * <a href="https://gitlab.com/nexa/otoplo/nexa-libs/nexcore-lib">nexcore-lib</a>
     */
    SCRIPT((byte) (1 << 3)),

    TEMPLATE((byte) (19 << 3)),

    /**
     * Not supported.
     * Nexa developers also don't support it according to the library
     * <a href="https://gitlab.com/nexa/otoplo/nexa-libs/nexcore-lib">nexcore-lib</a>
     */
    GROUP((byte) (11 << 3));

    private final byte versionByte;

    NexaAddressType(byte versionByte) {
        this.versionByte = versionByte;
    }

    public byte getVersionByte() {
        return versionByte;
    }

}
