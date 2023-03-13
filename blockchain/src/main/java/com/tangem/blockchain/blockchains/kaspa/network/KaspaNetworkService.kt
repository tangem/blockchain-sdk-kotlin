package com.tangem.blockchain.blockchains.tezos.network

import com.tangem.blockchain.blockchains.kaspa.network.KaspaRestApiNetworkProvider
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.network.API_KASPA
import com.tangem.blockchain.network.MultiNetworkProvider

class KaspaNetworkService() : KaspaRestApiNetworkProvider(API_KASPA)