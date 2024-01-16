package com.tangem.blockchain.common.smartcontract

interface SmartContractMethod {
    val prefix: String
    val data: ByteArray
}
