package com.tangem.blockchain.blockchains.bitcoincash

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.network.blockchair.BlockchairProvider

class BitcoinCashNetworkManager(blockchairApiKey: String? = null) :
        BlockchairProvider(Blockchain.BitcoinCash, blockchairApiKey)