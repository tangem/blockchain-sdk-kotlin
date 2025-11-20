package com.tangem.blockchain.blockchains.bitcoin.walletconnect

import com.tangem.blockchain.blockchains.bitcoin.BitcoinTransactionBuilder
import com.tangem.blockchain.blockchains.bitcoin.BitcoinTransactionExtras
import com.tangem.blockchain.blockchains.bitcoin.BitcoinWalletManager
import com.tangem.blockchain.blockchains.bitcoin.walletconnect.models.SendTransferRequest
import com.tangem.blockchain.blockchains.bitcoin.walletconnect.models.SendTransferResponse
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.successOr
import java.math.BigDecimal

/**
 * Handler for WalletConnect Bitcoin RPC methods.
 *
 * This class provides implementation for WalletConnect Bitcoin RPC methods according to
 * the Reown documentation.
 *
 * @property wallet The Bitcoin wallet instance
 * @property walletManager The Bitcoin wallet manager for transaction operations
 *
 * @see <a href="https://docs.reown.com/advanced/multichain/rpc-reference/bitcoin-rpc">Bitcoin RPC Reference</a>
 */
internal class BitcoinWalletConnectHandler(
    private val wallet: Wallet,
    private val walletManager: BitcoinWalletManager,
) {

    /**
     * Sends a Bitcoin transfer transaction.
     *
     * This method implements the WalletConnect `sendTransfer` RPC method. It builds and broadcasts
     * a Bitcoin transaction with optional memo (OP_RETURN) and custom change address support.
     *
     * @param request The sendTransfer request parameters
     * @param signer The transaction signer (typically Tangem card)
     * @return Success with transaction hash, or Failure with error details
     *
     * @see <a href="https://docs.reown.com/advanced/multichain/rpc-reference/bitcoin-rpc#sendtransfer">sendTransfer Documentation</a>
     */
    suspend fun sendTransfer(
        request: SendTransferRequest,
        signer: TransactionSigner,
    ): Result<SendTransferResponse> {
        // Validate that the account address belongs to this wallet
        val isValidAccount = wallet.addresses.any { it.value == request.account }
        if (!isValidAccount) {
            return Result.Failure(
                BlockchainSdkError.CustomError(
                    "Account address ${request.account} does not belong to this wallet",
                ),
            )
        }

        // Validate custom change address if provided
        request.changeAddress?.let { changeAddr ->
            val isValidChangeAddress = wallet.addresses.any { it.value == changeAddr }
            if (!isValidChangeAddress) {
                return Result.Failure(
                    BlockchainSdkError.CustomError(
                        "Change address $changeAddr does not belong to this wallet",
                    ),
                )
            }
        }

        // Convert amount from satoshis to BTC
        val amountInSatoshis = try {
            BigDecimal(request.amount)
        } catch (e: NumberFormatException) {
            return Result.Failure(
                BlockchainSdkError.CustomError("Invalid amount format: ${request.amount}"),
            )
        }

        val amountInBtc = convertSatoshisToBtc(amountInSatoshis)

        // Get current fee estimate
        val amount = Amount(
            currencySymbol = wallet.blockchain.currency,
            value = amountInBtc,
            decimals = wallet.blockchain.decimals(),
            type = AmountType.Coin,
        )

        val feeResult = walletManager.getFee(amount, request.recipientAddress).successOr { failure ->
            return failure
        }

        // Use normal fee as default
        val fee = feeResult.normal

        // Create transaction extras with memo and changeAddress
        val extras = BitcoinTransactionExtras(
            memo = request.memo,
            changeAddress = request.changeAddress,
        )

        // Build transaction data
        val transactionData = TransactionData.Uncompiled(
            amount = amount,
            fee = Fee.Common(fee.amount),
            sourceAddress = request.account,
            destinationAddress = request.recipientAddress,
            extras = extras,
        )

        // Send transaction using wallet manager
        val sendResult = walletManager.send(transactionData, signer).successOr { failure ->
            return failure
        }

        return Result.Success(
            SendTransferResponse(txid = sendResult.hash),
        )
    }

    /**
     * Converts satoshis to BTC.
     *
     * @param satoshis Amount in satoshis
     * @return Amount in BTC
     */
    private fun convertSatoshisToBtc(satoshis: BigDecimal): BigDecimal {
        // 1 BTC = 100,000,000 satoshis
        return satoshis.movePointLeft(SATOSHI_DECIMALS)
    }

    /**
     * Converts BTC to satoshis.
     *
     * @param btc Amount in BTC
     * @return Amount in satoshis
     */
    private fun convertBtcToSatoshis(btc: BigDecimal): BigDecimal {
        return btc.movePointRight(SATOSHI_DECIMALS)
    }

    companion object {
        private const val SATOSHI_DECIMALS = 8
    }
}