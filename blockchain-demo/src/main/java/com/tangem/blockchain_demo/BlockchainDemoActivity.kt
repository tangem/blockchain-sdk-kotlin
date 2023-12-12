package com.tangem.blockchain_demo

import android.R
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.hedera.hashgraph.sdk.*
import com.tangem.TangemSdk
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain_demo.cardSdk.ScanCardAndDerive
import com.tangem.blockchain_demo.databinding.ActivityBlockchainDemoBinding
import com.tangem.blockchain_demo.extensions.*
import com.tangem.blockchain_demo.model.ScanResponse
import com.tangem.common.CompletionResult
import com.tangem.common.card.CardWallet
import com.tangem.common.core.TangemError
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.calculateSha256
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toHexString
import com.tangem.sdk.extensions.init
import kotlinx.coroutines.*
import java.io.PrintWriter
import java.io.StringWriter
import java.math.BigDecimal
import kotlin.coroutines.CoroutineContext

private val parentJob = Job()
private val coroutineContext: CoroutineContext
    get() = parentJob + Dispatchers.IO + exceptionHandler
private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
    val sw = StringWriter()
    throwable.printStackTrace(PrintWriter(sw))
    val exceptionAsString: String = sw.toString()
    Log.e("Coroutine", exceptionAsString)
    throw throwable
}

val scope = CoroutineScope(coroutineContext)

class BlockchainDemoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBlockchainDemoBinding

    private lateinit var tangemSdk: TangemSdk
    private lateinit var signer: TransactionSigner

    private lateinit var scanResponse: ScanResponse
    private lateinit var walletManager: WalletManager
    private var token: Token? = null

    private lateinit var selectedWallet: CardWallet
    private var selectedBlockchain = getTestedBlockchains()[0]
    private var selectedFee = BigDecimal.ZERO

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initBinding()
        initTangemSdk()
        initViews()
        setupViews()
        setupVisibility()
    }

    private fun getTestedBlockchains(): List<Blockchain> {
//        return listOf(
//            Blockchain.Gnosis,
//            Blockchain.Arbitrum,
//            Blockchain.Fantom,
//        )
        return Blockchain.valuesWithoutUnknown()
    }

    private fun initBinding() {
        binding = ActivityBlockchainDemoBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    private fun initTangemSdk() {
        tangemSdk = TangemSdk.init(this)
        signer = CommonSigner(tangemSdk)
    }

    private fun initViews() = with(binding) {
        btnScan.setOnClickListener { scan() }
        containerSelectWalletWithBlockchain.tvBlockchainAddresses.setOnLongClickListener {
            containerSelectWalletWithBlockchain.tvBlockchainAddresses.copyTextToClipboard()
            showToast("Address was copied")
            return@setOnLongClickListener true
        }
        containerRecipientAddressFee.btnPasteRecipientAddres.setOnClickListener {
            containerRecipientAddressFee.tilEtRecipientAddress.setTextFromClipboard()
        }
        containerRecipientAddressFee.btnLoadFee.setOnClickListener { loadFee() }
    }

    private fun setupViews() = with(binding) {
        containerRecipientAddressFee.tilEtRecipientAddress.setText("")
    }

    private fun setupVisibility() = with(binding) {
        containerScanCard.root.show()
    }

    private fun resetWalletAddress() = with(binding) {
        containerSelectWalletWithBlockchain.tvBlockchainAddresses.text = ""
    }

    private fun resetRecipientAddressFeeValues() = with(binding) {
        containerRecipientAddressFee.tvBalance.text = ""
        containerRecipientAddressFee.tvFeeMin.text = ""
        containerRecipientAddressFee.tvFeeMax.text = ""
        containerRecipientAddressFee.tvFeeAverage.text = ""
        containerRecipientAddressFee.tilEtSumToSend.setText("")
    }

    private fun hideRecipientAddressFeeValues() = with(binding) {
        containerRecipientAddressFee.root.hide()
    }

    private fun scan() {
        scope.launch {
            doHederaStuff()
        }

        tangemSdk.startSessionWithRunnable(ScanCardAndDerive(getTestedBlockchains())) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    scanResponse = result.data
                    scope.launch(Dispatchers.Main) {
                        resetWalletAddress()
                        hideRecipientAddressFeeValues()
                        resetRecipientAddressFeeValues()
                        initWalletsBlockchainContainer()
                    }

                }
                is CompletionResult.Failure -> {
                    when (result.error) {
                        is TangemSdkError.UserCancelled -> {}
                        else -> handleError(result.error)
                    }
                }
            }
        }
    }

    private fun initWalletsBlockchainContainer() = with(binding) {

        fun initSpBlockchain(wallet: CardWallet) = with(containerSelectWalletWithBlockchain) {
            val supportedBlockchains = getTestedBlockchains()
                    .filter { it.getSupportedCurves()[0] == wallet.curve }
                    .filter { it.isTestnet() == scanResponse.card.isTestCard }

            val blockchainsAdapter = ArrayAdapter(
                this@BlockchainDemoActivity,
                R.layout.simple_spinner_item,
                supportedBlockchains.map { it }
            )
            blockchainsAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
            spSelectBlockchain.adapter = blockchainsAdapter
            spSelectBlockchain.onItemSelected<Blockchain> { blockchain, _ ->
                selectedBlockchain = blockchain
            }
        }

        fun initBtnLoadWallet() {
            containerSelectWalletWithBlockchain.btnLoadWallet.setOnClickListener {
                resetWalletAddress()
                hideRecipientAddressFeeValues()
                resetRecipientAddressFeeValues()
                loadWallet(
                    onSuccess = {
                        containerRecipientAddressFee.root.show() { content.beginDelayedTransition() }
                        onWalletLoaded(walletManager)
                    },
                    onFailure = ::handleError
                )
            }
        }
            content.beginDelayedTransition()
            containerScanCard.root.hide()
            containerSelectWalletWithBlockchain.root.show()

            val wallets = scanResponse.card.wallets
            with(containerSelectWalletWithBlockchain) {
                val walletsAdapter = ArrayAdapter(
                    this@BlockchainDemoActivity,
                    R.layout.simple_spinner_item,
                    wallets.map { it.publicKey.toHexString() }
                )
                walletsAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
                spSelectWallet.adapter = walletsAdapter
                spSelectWallet.onItemSelected<String> { hexPublicKey, _ ->
                    selectedWallet = wallets.firstOrNull { it.publicKey.toHexString() == hexPublicKey }
                            ?: throw UnsupportedOperationException()

                    initSpBlockchain(selectedWallet)
                }

                selectedWallet = wallets[0]
                initSpBlockchain(selectedWallet)
                initBtnLoadWallet()
            }
    }

    private fun loadWallet(onSuccess: () -> Unit, onFailure: (BlockchainSdkError) -> Unit) = with(binding) {
        containerSelectWalletWithBlockchain.tvBlockchainAddresses.text = ""
        try {
            walletManager = WalletManagerFactory(BlockchainSdkConfig()).makeWalletManagerForApp(
                    scanResponse = scanResponse,
                    blockchain = Blockchain.fromId(selectedBlockchain.id),
                    derivationParams = scanResponse.card.derivationParams(null)
            )!!
        }catch (ex: Exception) {
            showToast("WalletManager exception: ${ex.localizedMessage ?: "unknown"}")
            return@with
        }

        containerSelectWalletWithBlockchain.tvBlockchainAddresses.text =
            walletManager.wallet.addresses.joinToString(separator = " ") { it.value }

        scope.launch {
            try {
                walletManager.update()
                withMainContext { onSuccess() }
            } catch (er: BlockchainSdkError) {
                withMainContext { onFailure(er) }
            }
        }
    }

    private fun onWalletLoaded(walletManager: WalletManager) = with(binding) {
        val tokenAmount = if (token != null) {
            walletManager.wallet.getTokenAmount(token!!)
        } else {
            walletManager.wallet.amounts[AmountType.Coin]
        }
        val stringValue = tokenAmount?.value?.stripZeroPlainString() ?: "Error"
        containerRecipientAddressFee.tvBalance.text = stringValue
        containerRecipientAddressFee.tilEtSumToSend.setText(stringValue)
        containerRecipientAddressFee.btnLoadFee.isEnabled = true
    }

    private fun doHederaStuff() {
        // val newAccountPrivateKey: PrivateKey = PrivateKey.generateED25519()
        // val newAccountPublicKey: PublicKey = newAccountPrivateKey.publicKey

        val privateKey = PrivateKey.fromString(
            "3030020100300706052b8104000a04220420d22811308cacd33a0d371d93506a01b27060d2314e5f6bda0cd05faae18b418e"
        )
        val accountId = AccountId.fromString("0.0.6753476")

        //Create your Hedera Testnet client
        val client: Client = Client.forTestnet()

        //Set your account as the client's operator
        client.setOperator(accountId, privateKey)

        //Set the default maximum transaction fee (in Hbar)
        client.setDefaultMaxTransactionFee(Hbar(100))

        //Set the maximum payment for queries (in Hbar)
        client.setMaxQueryPayment(Hbar(50))

        //Request the cost of the query
        val queryCost: Hbar = AccountBalanceQuery()
            .setAccountId(accountId)
            .getCost(client)

        println("The cost of this query is: $queryCost")
    }

    private fun loadFee() = with(binding) {
        if (containerRecipientAddressFee.tilEtRecipientAddress.text.isNullOrBlank()) {
            Toast.makeText(this@BlockchainDemoActivity, "Please enter receiver address", Toast.LENGTH_LONG).show()
            return
        } else if (containerRecipientAddressFee.tilEtSumToSend.text.isNullOrBlank()) {
            Toast.makeText(this@BlockchainDemoActivity, "Choose sum to send", Toast.LENGTH_LONG).show()
            return@with
        }

        val valueToSend = containerRecipientAddressFee.tilEtSumToSend.text.toString().toBigDecimal()
        val amountToSend = if (token == null) {
            Amount(walletManager.wallet.amounts[AmountType.Coin]!!, valueToSend)
        } else {
            Amount(walletManager.wallet.getTokenAmount(token!!)!!, valueToSend)
        }

        scope.launch {
            val feeResult = (walletManager as TransactionSender).getFee(
                amount = amountToSend,
                destination = containerRecipientAddressFee.tilEtRecipientAddress.text.toString()
            )
            withContext(Dispatchers.Main) {
                when (feeResult) {
                    is Result.Success -> {
                        when(val fees = feeResult.data) {
                            is TransactionFee.Single -> {
                                containerRecipientAddressFee.tvFeeAverage.text = fees.normal.amount.value.toString()
                                selectedFee = fees.normal.amount.value ?: BigDecimal(0)
                            }
                            is TransactionFee.Choosable -> {
                                containerRecipientAddressFee.tvFeeMin.text =
                                    fees.minimum.amount.value?.stripZeroPlainString()
                                containerRecipientAddressFee.tvFeeAverage.text =
                                    fees.normal.amount.value?.stripZeroPlainString()
                                containerRecipientAddressFee.tvFeeMax.text =
                                    fees.priority.amount.value?.stripZeroPlainString()
                                selectedFee = fees.normal.amount.value ?: BigDecimal(0)
                            }
                        }
                    }
                    is Result.Failure -> handleError(feeResult.error)
                }
            }
        }
    }

//    private fun send() {
//        scope.launch {
//            val result = (walletManager as TransactionSender).send(
//                transactionData = formTransactionData(),
//                signer = signer
//            )
//            withContext(Dispatchers.Main) {
//                when (result) {
//                    is SimpleResult.Success -> showToast("Sending success")
//                    is SimpleResult.Failure -> handleError(result.error)
//                }
//            }
//        }
//    }

//    private fun formTransactionData(): TransactionData = with(binding) {
//        val amount = if (token != null) {
//            walletManager.wallet.getTokenAmount(token!!)!!.copy(
//                value = containerRecipientAddressFee.tilEtSumToSend.text.toString().toBigDecimal()
//            )
//        } else {
//            walletManager.wallet.amounts[AmountType.Coin]!!.copy(
//                value = containerRecipientAddressFee.tilEtSumToSend.text.toString().toBigDecimal() - selectedFee
//            )
//        }
//        return TransactionData(
//            amount,
//            walletManager.wallet.amounts[AmountType.Coin]!!.copy(value = selectedFee),
//            walletManager.wallet.address,
//            containerRecipientAddressFee.tilEtRecipientAddress.text.toString(),
//            contractAddress = token?.contractAddress
//        )
//    }

    private fun handleError(error: TangemError) {
        val message = error.messageResId?.let { this.getString(it) } ?: error.customMessage
        showToast(message)
    }

    private fun handleError(error: BlockchainError) {
        showToast(error.customMessage)
    }

    private fun showToast(message: String?) {
        Toast.makeText(this, message ?: "null", Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}