package com.tangem.blockchain.blockchains.ergo

import com.tangem.blockchain.blockchains.ergo.models.Address
import com.tangem.common.extensions.toHexString
import org.spongycastle.util.encoders.Hex
import kotlin.experimental.and

class ErgoUtils {
    companion object {
        fun ergoTree(address: Address): String {
            return if ((address.addrBytes!![0] and 0xF).toInt() == 1)
                Hex.decode("0008cd").plus(address.addrBytes!!.sliceArray(1..33)).toHexString()
            else
                address.addrBytes!!.sliceArray(1..address.addrBytes!!.size - 5).toHexString()
        }
    }
}
