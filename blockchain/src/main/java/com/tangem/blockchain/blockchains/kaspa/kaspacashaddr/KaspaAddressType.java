package com.tangem.blockchain.blockchains.kaspa.kaspacashaddr;


/**
 * Copyright (c) 2018 Tobias Brandt
 * 
 * Distributed under the MIT software license, see the accompanying file LICENSE
 * or http://www.opensource.org/licenses/mit-license.php.
 */
public enum KaspaAddressType {

	P2PK_SCHNORR((byte) 0), P2PK_ECDSA((byte) 1), P2SH((byte) 8);

	private final byte versionByte;

	KaspaAddressType(byte versionByte) {
		this.versionByte = versionByte;
	}

	public byte getVersionByte() {
		return versionByte;
	}
}