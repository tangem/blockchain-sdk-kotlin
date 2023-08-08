package com.tangem.blockchain.blockchains.chia.network

import com.tangem.blockchain.network.API_CHIA_FIREACADEMY
import com.tangem.blockchain.network.API_CHIA_FIREACADEMY_TESTNET

class ChiaNetworkService(isTestNet: Boolean, fireAcademyApiKey: String) :
    ChiaJsonRpcProvider(
        if (!isTestNet) API_CHIA_FIREACADEMY else API_CHIA_FIREACADEMY_TESTNET,
        fireAcademyApiKey
    )