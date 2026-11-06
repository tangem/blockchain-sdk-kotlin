package com.tangem.blockchain.blockchains.solana.nft

import com.tangem.blockchain.common.TransactionExtras

/** Marks an uncompiled Solana token transfer as an NFT transfer, so its path is picked by the asset on chain. */
data object SolanaNFTTransferExtras : TransactionExtras