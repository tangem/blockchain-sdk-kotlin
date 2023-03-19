package com.tangem.blockchain.blockchains.kaspa;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.core.UnsafeByteArrayOutputStream;
import org.bitcoinj.crypto.TransactionSignature;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.bitcoinj.core.Utils.uint16ToByteStreamLE;
import static org.bitcoinj.core.Utils.uint32ToByteStreamLE;
import static org.bitcoinj.core.Utils.uint64ToByteStreamLE;

import com.tangem.blockchain.blockchains.cardano.crypto.Blake2b;

public class KaspaTransaction extends Transaction {
    private final byte[] TRANSACTION_SIGNING_DOMAIN = "TransactionSigningHash".getBytes(StandardCharsets.UTF_8);
    private final byte[] TRANSACTION_SIGNING_ECDSA_DOMAIN_HASH =
            Sha256Hash.of("TransactionSigningHashECDSA".getBytes(StandardCharsets.UTF_8)).getBytes();
    private final int BLAKE2B_DIGEST_LENGTH = 32;

    public KaspaTransaction(NetworkParameters params) {
        super(params);
    }

    public synchronized byte[] hashForSignatureWitness(
            int inputIndex,
            byte[] connectedScript,
            Coin prevValue,
            SigHash type,
            boolean anyoneCanPay)
    {
        byte sigHashType = (byte) TransactionSignature.calcSigHashValue(type, anyoneCanPay);
//        sigHashType |= SIGHASH_FORK_ID;

        ByteArrayOutputStream bos = new UnsafeByteArrayOutputStream(length == UNKNOWN_LENGTH ? 256 : length + 4);
        try {
            byte[] hashPrevouts = new byte[32];
            byte[] hashSequence = new byte[32];
            byte[] hashSigOpCounts = new byte[32];
            byte[] hashOutputs = new byte[32];
            anyoneCanPay = (sigHashType & SIGHASH_ANYONECANPAY_VALUE) == SIGHASH_ANYONECANPAY_VALUE;
            List<TransactionInput> inputs = getInputs();
            List<TransactionOutput> outputs = getOutputs();

            if (!anyoneCanPay) {
                ByteArrayOutputStream bosHashPrevouts = new UnsafeByteArrayOutputStream(256);
                for (int i = 0; i < inputs.size(); ++i) {
                    bosHashPrevouts.write(inputs.get(i).getOutpoint().getHash().getBytes());
                    uint32ToByteStreamLE(inputs.get(i).getOutpoint().getIndex(), bosHashPrevouts);
                }
                hashPrevouts = blake2bDigestOf(bosHashPrevouts.toByteArray());
            }

            if (!anyoneCanPay && type != SigHash.SINGLE && type != SigHash.NONE) {
                ByteArrayOutputStream bosSequence = new UnsafeByteArrayOutputStream(256);
                for (int i = 0; i < inputs.size(); ++i) {
                    uint64ToByteStreamLE(BigInteger.valueOf(0), bosSequence);
                }
                hashSequence = blake2bDigestOf(bosSequence.toByteArray());
            }

            if (!anyoneCanPay) {
                ByteArrayOutputStream bosSigOpCounts = new UnsafeByteArrayOutputStream(256);
                for (int i = 0; i < inputs.size(); ++i) {
                    bosSigOpCounts.write(1);
                }
                hashSigOpCounts = blake2bDigestOf(bosSigOpCounts.toByteArray());
            }

            if (type != SigHash.SINGLE && type != SigHash.NONE) {
                ByteArrayOutputStream bosHashOutputs = new UnsafeByteArrayOutputStream(256);
                for (int i = 0; i < outputs.size(); ++i) {
                    uint64ToByteStreamLE(BigInteger.valueOf(outputs.get(i).getValue().getValue()), bosHashOutputs);
                    uint16ToByteStreamLE(0, bosHashOutputs); // script version
                    uint64ToByteStreamLE(BigInteger.valueOf(outputs.get(i).getScriptBytes().length), bosHashOutputs);
                    bosHashOutputs.write(outputs.get(i).getScriptBytes());
                }
                hashOutputs = blake2bDigestOf(bosHashOutputs.toByteArray());
            } else if (type == SigHash.SINGLE && inputIndex < outputs.size()) {
                ByteArrayOutputStream bosHashOutputs = new UnsafeByteArrayOutputStream(256);
                uint64ToByteStreamLE(BigInteger.valueOf(outputs.get(inputIndex).getValue().getValue()), bosHashOutputs);
                uint16ToByteStreamLE(0, bos); // script version
                uint64ToByteStreamLE(BigInteger.valueOf(outputs.get(inputIndex).getScriptBytes().length), bosHashOutputs);
                bosHashOutputs.write(outputs.get(inputIndex).getScriptBytes());
                hashOutputs = blake2bDigestOf(bosHashOutputs.toByteArray());
            }

            uint16ToByteStreamLE((int) getVersion(), bos);
            bos.write(hashPrevouts);
            bos.write(hashSequence);
            bos.write(hashSigOpCounts);
            bos.write(inputs.get(inputIndex).getOutpoint().getHash().getBytes());
            uint32ToByteStreamLE(inputs.get(inputIndex).getOutpoint().getIndex(), bos);
            uint16ToByteStreamLE(0, bos); // script version
            uint64ToByteStreamLE(BigInteger.valueOf(connectedScript.length), bos);
            bos.write(connectedScript);
            uint64ToByteStreamLE(BigInteger.valueOf(prevValue.getValue()), bos);
            uint64ToByteStreamLE(BigInteger.valueOf(inputs.get(inputIndex).getSequenceNumber()), bos);
            bos.write(1); // sig op count
            bos.write(hashOutputs);
            uint64ToByteStreamLE(BigInteger.valueOf(getLockTime()), bos);
            bos.write(new byte[20]); // subnetwork id
            uint64ToByteStreamLE(BigInteger.valueOf(0), bos); // gas
            bos.write(new byte[32]); // payload hash
            bos.write(sigHashType); // sig op count
        } catch (IOException e) {
            throw new RuntimeException(e);  // Cannot happen.
        }

        byte[] blakeHash = blake2bDigestOf(bos.toByteArray()); // used as is for Schnorr addresses

        ByteArrayOutputStream finalBos = new UnsafeByteArrayOutputStream(64);
        try {
            finalBos.write(TRANSACTION_SIGNING_ECDSA_DOMAIN_HASH);
            finalBos.write(blakeHash);
        } catch (IOException e) {
            throw new RuntimeException(e);  // Cannot happen.
        }

        return Sha256Hash.of(finalBos.toByteArray()).getBytes();
    }

    private byte[] blake2bDigestOf(byte[] input) {
        Blake2b.Mac digest = Blake2b.Mac.newInstance(TRANSACTION_SIGNING_DOMAIN, BLAKE2B_DIGEST_LENGTH);
        return digest.digest(input);
    }
}
