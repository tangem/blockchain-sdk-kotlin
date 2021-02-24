package com.tangem.blockchain.blockchains.ducatus.network

import com.tangem.blockchain.blockchains.ducatus.network.bitcore.BitcoreNetworkProvider
import com.tangem.blockchain.network.API_DUCATUS

class DucatusNetworkService : BitcoreNetworkProvider(API_DUCATUS)