package com.tangem.blockchain.blockchains.filecoin.network.request

/**
 * Filecoin rpc method
 *
 * @property name method name
 */
internal sealed class FilecoinRpcMethod(val name: String) {

    data object GetActorInfo : FilecoinRpcMethod(name = "Filecoin.StateGetActor")

    data object GetMessageGas : FilecoinRpcMethod(name = "Filecoin.GasEstimateMessageGas")

    data object SubmitTransaction : FilecoinRpcMethod(name = "Filecoin.MpoolPush")
}