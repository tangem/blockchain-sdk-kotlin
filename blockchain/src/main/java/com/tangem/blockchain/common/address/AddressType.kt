package com.tangem.blockchain.common.address

// cardano:
// byron = legacy
// Shelley = default

// btc:
// segwit = default
enum class AddressType(val priority: Int) {

    Default(1),

    Legacy(2);

}
