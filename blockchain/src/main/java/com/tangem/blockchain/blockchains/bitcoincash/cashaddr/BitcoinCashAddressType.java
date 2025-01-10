package com.tangem.blockchain.blockchains.bitcoincash.cashaddr;


/**
 * Copyright (c) 2018 Tobias Brandt
 * 
 * Distributed under the MIT software license, see the accompanying file LICENSE
 * or http://www.opensource.org/licenses/mit-license.php.
 */
public enum BitcoinCashAddressType {

	P2PKH((byte) 0, "q"), P2SH((byte) 8, "p");

	private final byte versionByte;
	private final String addressPrefix;

	BitcoinCashAddressType(byte versionByte, String addressPrefix) {
		this.versionByte = versionByte;
		this.addressPrefix = addressPrefix;
	}

	public byte getVersionByte() {
		return versionByte;
	}

	public String getAddressPrefix() {
		return addressPrefix;
	}
}