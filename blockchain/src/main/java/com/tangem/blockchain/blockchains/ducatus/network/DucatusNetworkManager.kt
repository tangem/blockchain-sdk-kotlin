package com.tangem.blockchain.blockchains.ducatus.network

import com.tangem.blockchain.blockchains.ducatus.network.bitcore.BitcoreProvider
import com.tangem.blockchain.network.API_DUCATUS

class DucatusNetworkManager : BitcoreProvider(API_DUCATUS)