package com.tangem.blockchain.blockchains.icp.network

import com.tangem.blockchain.common.*
import com.tangem.blockchain.extensions.Result
import com.tangem.common.CompletionResult
import com.tangem.common.extensions.hexToBytes
import io.github.andreypfau.kotlinx.crypto.sha2.sha256
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.provider.JCEECPublicKey
import org.bouncycastle.jce.spec.ECPublicKeySpec
import org.ic4j.agent.Agent
import org.ic4j.agent.AgentBuilder
import org.ic4j.agent.ProxyBuilder
import org.ic4j.agent.ReplicaTransport
import org.ic4j.agent.http.ReplicaOkHttpTransport
import org.ic4j.agent.icp.IcpSystemCanisters
import org.ic4j.agent.identity.ExternalMultiSignIdentity
import java.math.BigDecimal

internal class ICPIc4jAgentNetworkProvider(
    override val baseUrl: String,
    private val publicKey: Wallet.PublicKey,
) : ICPNetworkProvider {

    private var transport: ReplicaTransport = ReplicaOkHttpTransport.create(baseUrl)
    private var anonymousAgent: Agent = AgentBuilder().transport(transport).build()

    override suspend fun getBalance(address: String): Result<BigDecimal> {
        return try {
            withContext(Dispatchers.IO) {
                val icpLedger: ICPLedgerProxy = ProxyBuilder.create(anonymousAgent, IcpSystemCanisters.LEDGER)
                    .getProxy(ICPLedgerProxy::class.java)
                val value = icpLedger.getBalance(ICPBalanceRequest(address.hexToBytes())).value
                    ?: return@withContext Result.Failure(
                        IllegalStateException("balance value is null").toBlockchainSdkError(),
                    )

                Result.Success(value.toBigDecimal().movePointLeft(Blockchain.InternetComputer.decimals()))
            }
        } catch (exception: Throwable) {
            Result.Failure(Exception(exception.message).toBlockchainSdkError())
        }
    }

    // ic4j-agent is hard to disassemble to sign and send as different actions, so we do it their way
    override suspend fun signAndSendTransaction(transferWithSigner: ICPTransferWithSigner): Result<Long?> {
        return try {
            withContext(Dispatchers.IO) {
                val signFunction = { messages: List<ByteArray> -> sign(messages, transferWithSigner.signer) }

                val spec = ECNamedCurveTable.getParameterSpec("secp256k1")
                val point = spec.curve.decodePoint(publicKey.blockchainKey)
                val derPublicKey = JCEECPublicKey("EC", ECPublicKeySpec(point, spec))

                val identity = ExternalMultiSignIdentity(derPublicKey.encoded, signFunction)
                val signerAgent = AgentBuilder().transport(transport).identity(identity).build()
                val icpLedger: ICPLedgerProxy = ProxyBuilder.create(signerAgent, IcpSystemCanisters.LEDGER)
                    .getProxy(ICPLedgerProxy::class.java)

                val result = icpLedger.transfer(transferWithSigner.transfer).get()
                    ?: return@withContext Result.Failure(BlockchainSdkError.CustomError("Empty send response"))

                when (result) {
                    ICPTransferResponse.Ok -> Result.Success(result.blockIndex)
                    ICPTransferResponse.Err -> Result.Failure(result.errValue.toBlockchainSdkError())
                }
            }
        } catch (exception: Throwable) {
            Result.Failure(Exception(exception.message).toBlockchainSdkError())
        }
    }

    private fun sign(messages: List<ByteArray>, signer: TransactionSigner): List<ByteArray> {
        val hashesToSign = messages.map { sha256(it) }
        return runBlocking {
            when (val result = signer.sign(hashesToSign, publicKey)) {
                is CompletionResult.Success -> result.data
                is CompletionResult.Failure -> throw result.error
            }
        }
    }

    @Suppress("CyclomaticComplexMethod")
    private fun ICPTransferError?.toBlockchainSdkError(): BlockchainSdkError {
        val message = when (this) {
            ICPTransferError.InsufficientFunds ->
                this.insufficientFundsError?.balance?.let { "Insufficient funds, current balance - $it" }
                    ?: "Insufficient funds"
            ICPTransferError.BadFee ->
                this.badFeeError?.expectedFee?.value?.let { "Invalid fee, expected - $it" }
                    ?: "Invalid fee"
            ICPTransferError.TxTooOld ->
                this.txTooOldError?.allowedWindowNanos?.let {
                    "Transaction too old, allowed window - $it. Try setting system clock to the correct time"
                } ?: "Transaction too old. Try setting system clock to the correct time"
            ICPTransferError.TxDuplicate ->
                this.txDuplicateError?.blockIndex?.let { "Duplicate transaction, previous block index - $it" }
                    ?: "Duplicate transaction"
            ICPTransferError.TxCreatedInFuture ->
                "Transaction timestamp is in the future. Set system clock to the correct time"
            null -> return BlockchainSdkError.FailedToSendException
        }

        return BlockchainSdkError.CustomError(message)
    }
}