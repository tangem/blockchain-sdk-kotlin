package com.tangem.blockchain.blockchains.solana.alt

import android.annotation.SuppressLint
import com.tangem.blockchain.blockchains.solana.alt.borsh.BorshDecoder
import foundation.metaplex.solanapublickeys.PUBLIC_KEY_LENGTH
import foundation.metaplex.solanapublickeys.PublicKey

@SuppressLint("UnsafeOptInUsageError")
data class AddressLookupTableState(
    val typeIndex: UInt,
    val deactivationSlot: ULong,
    val lastExtendedSlot: ULong,
    val lastExtendedSlotStartIndex: UByte,
    val authority: ByteArray,
    val addresses: List<ByteArray>,
) {
    companion object {

        fun fromReader(reader: BorshDecoder): AddressLookupTableState {
            val typeIndex = reader.decodeInt().toUInt()
            val deactivationSlot = reader.decodeLong().toULong()
            val lastExtendedSlot = reader.decodeLong().toULong()
            val lastExtendedSlotStartIndex = reader.decodeByte().toUByte()
            reader.decodeByte() // skip
            val authority = reader.decodeBytes(PublicKey.PUBLIC_KEY_LENGTH)
            reader.decodeByte() // skip
            reader.decodeByte() // skip
            val addresses = mutableListOf<ByteArray>()
            while (reader.remaining() >= PUBLIC_KEY_LENGTH) {
                addresses.add(reader.decodeBytes(PublicKey.PUBLIC_KEY_LENGTH))
            }

            return AddressLookupTableState(
                typeIndex = typeIndex,
                deactivationSlot = deactivationSlot,
                lastExtendedSlot = lastExtendedSlot,
                lastExtendedSlotStartIndex = lastExtendedSlotStartIndex,
                authority = authority,
                addresses = addresses,
            )
        }
    }
}