package com.tangem.blockchain.blockchains.ergo.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tangem.blockchain.blockchains.ergo.ErgoUtils
import com.tangem.blockchain.blockchains.ergo.network.api.responses.Assets
import com.tangem.blockchain.blockchains.ergo.network.api.responses.ErgoApiUnspentResponse
import com.tangem.blockchain.extensions.decodeBase58

@JsonClass(generateAdapter = true)
data class ErgoBox(
    @Json(name = "id")
    var id: String? = null,
    @Json(name = "value")
    var value: Long? = null,
    @Json(name = "creationHeight")
    var creationHeight: Long? = null,
    @Json(name = "ergoTree")
    var ergoTree: String? = null,
    @Json(name = "address")
    var address: Address? = null,
    @Json(name = "assets")
    var assets: List<Assets>? = null,
    @Json(name = "additionalRegisters")
    var additionalRegisters: Map<String, String>? = emptyMap(),
)

@JsonClass(generateAdapter = true)
data class Address(
    @Json(name = "address")
    var address: String? = null,
    @Json(name = "addrBytes")
    var addrBytes: ByteArray? = null,
)

fun ErgoApiUnspentResponse.toErgoBox(): ErgoBox {

    val preparedAddress = Address(
        this.address,
        this.address!!.decodeBase58()
    )

    return ErgoBox(
        id = this.id,
        value = this.value,
        creationHeight = this.creationHeight!! - 720L,
        assets = this.assets,
        address = preparedAddress,
        ergoTree = ErgoUtils.ergoTree(preparedAddress),
        additionalRegisters = this.additionalRegisters
    )
}

fun ErgoBox.toInput(): Input {
    return Input(boxId = this.id)
}

