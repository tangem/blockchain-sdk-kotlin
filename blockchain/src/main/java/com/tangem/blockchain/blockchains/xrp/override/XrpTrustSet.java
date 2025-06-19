package com.tangem.blockchain.blockchains.xrp.override;


import com.ripple.core.types.known.tx.txns.TrustSet;

public class XrpTrustSet extends TrustSet {

    public XrpTrustSet() {
        super();
    }

    public XrpSignedTransaction prepare(byte[] pubKeyBytes) {
        XrpSignedTransaction tx = XrpSignedTransaction.fromTx(this);
        tx.prepare(pubKeyBytes);
        return tx;
    }

}
