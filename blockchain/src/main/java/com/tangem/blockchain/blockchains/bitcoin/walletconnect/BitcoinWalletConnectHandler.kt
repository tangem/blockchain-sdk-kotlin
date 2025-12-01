package com.tangem.blockchain.blockchains.bitcoin.walletconnect

import android.util.Base64
import com.tangem.blockchain.blockchains.bitcoin.BitcoinTransactionExtras
import com.tangem.blockchain.blockchains.bitcoin.BitcoinWalletManager
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkProvider
import com.tangem.blockchain.blockchains.bitcoin.walletconnect.models.*
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.address.AddressType
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.successOr
import fr.acinq.bitcoin.psbt.Psbt
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
class BitcoinWalletConnectHandler(
    private val wallet: Wallet,
    private val walletManager: BitcoinWalletManager,
    private val networkProvider: BitcoinNetworkProvider,
) {

    private val psbtSigner = BitcoinPsbtSigner(wallet, networkProvider)
    private val messageSigner = BitcoinMessageSigner(wallet)

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
        validateAccountAddress(request.account).successOr { return it }
        request.changeAddress?.let { validateChangeAddress(it).successOr { return it } }

        val amountInBtc = parseAndConvertAmount(request.amount).successOr { return it }
        val amount = createAmount(amountInBtc)
        val fee = getFeeForTransaction(amount, request.recipientAddress).successOr { return it }

        val transactionData = buildTransactionData(request, amount, fee)
        val sendResult = walletManager.send(transactionData, signer).successOr { return it }

        return Result.Success(SendTransferResponse(txid = sendResult.hash))
    }

    /**
     * Validates that account address belongs to wallet.
     */
    private fun validateAccountAddress(account: String): Result<Unit> {
        return if (wallet.addresses.any { it.value == account }) {
            Result.Success(Unit)
        } else {
            Result.Failure(
                BlockchainSdkError.CustomError("Account address $account does not belong to this wallet"),
            )
        }
    }

    /**
     * Validates that change address belongs to wallet.
     */
    private fun validateChangeAddress(changeAddress: String): Result<Unit> {
        return if (wallet.addresses.any { it.value == changeAddress }) {
            Result.Success(Unit)
        } else {
            Result.Failure(
                BlockchainSdkError.CustomError("Change address $changeAddress does not belong to this wallet"),
            )
        }
    }

    /**
     * Parses amount string and converts from satoshis to BTC.
     */
    private fun parseAndConvertAmount(amountString: String): Result<BigDecimal> {
        val amountInSatoshis = try {
            BigDecimal(amountString)
        } catch (e: NumberFormatException) {
            return Result.Failure(
                BlockchainSdkError.CustomError("Invalid amount format: $amountString"),
            )
        }

        return Result.Success(convertSatoshisToBtc(amountInSatoshis))
    }

    /**
     * Creates Amount instance for transaction.
     */
    private fun createAmount(amountInBtc: BigDecimal): Amount {
        return Amount(
            currencySymbol = wallet.blockchain.currency,
            value = amountInBtc,
            decimals = wallet.blockchain.decimals(),
            type = AmountType.Coin,
        )
    }

    /**
     * Gets fee estimate for transaction.
     */
    private suspend fun getFeeForTransaction(
        amount: Amount,
        recipientAddress: String,
    ): Result<Fee> {
        val feeResult = walletManager.getFee(amount, recipientAddress).successOr { failure ->
            return Result.Failure(failure.error)
        }
        return Result.Success(Fee.Common(feeResult.normal.amount))
    }

    /**
     * Builds transaction data from request parameters.
     */
    private fun buildTransactionData(
        request: SendTransferRequest,
        amount: Amount,
        fee: Fee,
    ): TransactionData.Uncompiled {
        val extras = BitcoinTransactionExtras(
            memo = request.memo,
            changeAddress = request.changeAddress,
        )

        return TransactionData.Uncompiled(
            amount = amount,
            fee = fee,
            sourceAddress = request.account,
            destinationAddress = request.recipientAddress,
            extras = extras,
        )
    }

    /**
     * Gets account addresses for the wallet.
     *
     * This method implements the WalletConnect `getAccountAddresses` RPC method. It returns
     * the wallet's addresses filtered by intention type (payment/ordinal).
     *
     * According to the specification:
     * - If only "ordinal" intention is requested, return empty array
     * - Otherwise return: Legacy + Default (SegWit) addresses, and all addresses with UTXOs
     *
     * @param request The getAccountAddresses request parameters
     * @return Success with list of addresses, or Failure with error details
     *
     * @see <a href="https://docs.reown.com/advanced/multichain/rpc-reference/bitcoin-rpc#getaccountaddresses">getAccountAddresses Documentation</a>
     */
    fun getAccountAddresses(
        request: GetAccountAddressesRequest,
    ): Result<GetAccountAddressesResponse> {
        // Parse intentions from request
        val intentions = request.intentions?.mapNotNull { AddressIntention.fromString(it) } ?: listOf(
            AddressIntention.PAYMENT,
        )

        // If only "ordinal" intention is requested, return empty array
        if (intentions.size == 1 && intentions.contains(AddressIntention.ORDINAL)) {
            return Result.Success(GetAccountAddressesResponse(addresses = emptyList()))
        }

        // Build list of addresses to return
        val addressList = mutableListOf<AccountAddress>()

        // Add Legacy and Default (SegWit) addresses
        wallet.addresses.forEach { address ->
            when (address.type) {
                AddressType.Legacy, AddressType.Default -> {
                    addressList.add(
                        AccountAddress(
                            address = address.value,
                            publicKey = null, // Don't expose public key for security
                            path = null, // Path can be added if needed
                            intention = AddressIntention.PAYMENT.toApiString(),
                        ),
                    )
                }
                else -> {
                    // Include other address types as well
                    addressList.add(
                        AccountAddress(
                            address = address.value,
                            publicKey = null,
                            path = null,
                            intention = AddressIntention.PAYMENT.toApiString(),
                        ),
                    )
                }
            }
        }

        // Note: For multi-address wallets, we would also:
        // 1. Add all addresses with UTXOs (non-empty addresses)
        // 2. Add 2-20 unused addresses from receive (m/x/x/x/0/x) and change (m/x/x/x/1/x) chains
        // However, this requires UTXO tracking which is handled by BitcoinWalletManager
        // For now, we return the basic addresses that the wallet knows about

        return Result.Success(
            GetAccountAddressesResponse(addresses = addressList),
        )
    }

    /**
     * Signs a PSBT (Partially Signed Bitcoin Transaction).
     *
     * This method implements the WalletConnect `signPsbt` RPC method. It signs specified
     * inputs of a PSBT and optionally broadcasts the transaction.
     *
     * @param request The signPsbt request parameters
     * @param signer The transaction signer (typically Tangem card)
     * @return Success with signed PSBT and optional txid, or Failure with error details
     *
     * @see <a href="https://docs.reown.com/advanced/multichain/rpc-reference/bitcoin-rpc#signpsbt">signPsbt Documentation</a>
     */
    suspend fun signPsbt(
        request: SignPsbtRequest,
        signer: TransactionSigner,
    ): Result<SignPsbtResponse> {
        // Sign the PSBT
        val signedPsbtBase64 = psbtSigner.signPsbt(
            psbtBase64 = request.psbt,
            signInputs = request.signInputs,
            signer = signer,
        ).successOr { failure ->
            return failure
        }
        // If broadcast is requested, extract and send the transaction
        val txid = if (request.broadcast == true) {
            try {
                // Parse signed PSBT
                val psbtBytes = Base64.decode(signedPsbtBase64, Base64.NO_WRAP)

                val psbt = when (val result = Psbt.read(psbtBytes)) {
                    is fr.acinq.bitcoin.utils.Either.Right -> result.value
                    is fr.acinq.bitcoin.utils.Either.Left -> {
                        return Result.Failure(
                            BlockchainSdkError.CustomError("Failed to parse PSBT for broadcast: ${result.value}"),
                        )
                    }
                }

                // Broadcast the transaction
                psbtSigner.broadcastPsbt(psbt).successOr { failure ->
                    return failure
                }
            } catch (e: Exception) {
                return Result.Failure(
                    BlockchainSdkError.CustomError("Failed to broadcast PSBT: ${e.message}"),
                )
            }
        } else {
            null
        }

        return Result.Success(
            SignPsbtResponse(
                psbt = signedPsbtBase64,
                txid = txid,
            ),
        )
    }

    /**
     * Signs a message with a Bitcoin address.
     *
     * This method implements the WalletConnect `signMessage` RPC method. It signs an arbitrary
     * message using Bitcoin message signing format (BIP137 for ECDSA).
     *
     * @param request The signMessage request parameters
     * @param signer The transaction signer (typically Tangem card)
     * @return Success with signature and address, or Failure with error details
     *
     * @see <a href="https://docs.reown.com/advanced/multichain/rpc-reference/bitcoin-rpc#signmessage">signMessage Documentation</a>
     */
    suspend fun signMessage(
        request: SignMessageRequest,
        signer: TransactionSigner,
    ): Result<SignMessageResponse> {
        // Validate that the account address belongs to this wallet
        val isValidAccount = wallet.addresses.any { it.value == request.account }
        if (!isValidAccount) {
            return Result.Failure(
                BlockchainSdkError.CustomError(
                    "Account address ${request.account} does not belong to this wallet",
                ),
            )
        }

        // Determine which address to use for signing
        val signingAddress = request.address ?: request.account

        // Validate signing address belongs to wallet
        val isValidSigningAddress = wallet.addresses.any { it.value == signingAddress }
        if (!isValidSigningAddress) {
            return Result.Failure(
                BlockchainSdkError.CustomError(
                    "Signing address $signingAddress does not belong to this wallet",
                ),
            )
        }

        // Parse protocol
        val protocol = SignMessageProtocol.fromString(request.protocol ?: "ecdsa")

        // Sign the message
        val signResult = messageSigner.signMessage(
            message = request.message,
            address = signingAddress,
            protocol = protocol,
            signer = signer,
        ).successOr { return it }

        return Result.Success(
            SignMessageResponse(
                address = signResult.address,
                signature = signResult.signature,
                messageHash = signResult.messageHash,
            ),
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