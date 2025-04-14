package com.tangem.blockchain.blockchains.kaspa.kaspacashaddr;
 
// Helper class for CashAddr

public class KaspaAddressDecodedParts {

	String prefix;

	KaspaAddressType addressType;

	byte[] hash;

	public String getPrefix() {
		return prefix;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	public KaspaAddressType getAddressType() {
		return addressType;
	}

	public void setAddressType(KaspaAddressType addressType) {
		this.addressType = addressType;
	}

	public byte[] getHash() {
		return hash;
	}

	public void setHash(byte[] hash) {
		this.hash = hash;
	}

}