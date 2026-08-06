package com.tangem.blockchain.blockchains.tron

import com.tangem.blockchain.common.TransactionExtras
import com.tangem.blockchain.common.smartcontract.SmartContractCallData

/**
 * @param callData contract call to execute. `null` (or empty) means the transaction is a plain
 *   transfer that carries no contract call — it may still carry a [memo].
 * @param memo raw bytes for `Transaction.raw.data`. TRON's memo is arbitrary transaction-level data;
 *   cross-chain swap providers use it to identify the deposit that funds the swap.
 */
class TronTransactionExtras(
    val callData: SmartContractCallData? = null,
    val memo: ByteArray? = null,
) : TransactionExtras

/**
 * Call data that is present but empty means the same thing as no call data at all: a null-only
 * check would build a `TriggerSmartContract` with nothing to run, which the network rejects when the
 * destination is an ordinary account.
 */
internal fun SmartContractCallData?.orNullIfEmpty(): SmartContractCallData? = this?.takeIf { it.data.isNotEmpty() }

internal fun TronTransactionExtras?.callDataOrNull(): SmartContractCallData? = this?.callData.orNullIfEmpty()