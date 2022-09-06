package com.tangem.blockchain_demo

import android.R
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.tangem.TangemSdk
import com.tangem.blockchain.common.*
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain_demo.cardSdk.ScanCardAndDerive
import com.tangem.blockchain_demo.databinding.ActivityBlockchainDemoBinding
import com.tangem.blockchain_demo.extensions.*
import com.tangem.blockchain_demo.model.ScanResponse
import com.tangem.common.CompletionResult
import com.tangem.common.card.CardWallet
import com.tangem.common.core.TangemError
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.toHexString
import com.tangem.tangem_sdk_new.extensions.init
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

    private fun resetWalletValues() = with(binding) {
        containerRecipientAddressFee.tvBalance.text = ""
        containerRecipientAddressFee.tvFeeMin.text = ""
        containerRecipientAddressFee.tvFeeMax.text = ""
        containerRecipientAddressFee.tvFeeAverage.text = ""
        containerRecipientAddressFee.tilEtSumToSend.setText("")
    }

    private fun scan() {
        tangemSdk.startSessionWithRunnable(ScanCardAndDerive(getTestedBlockchains())) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    scanResponse = result.data
                    initWalletsBlockchainContainer()
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
                containerRecipientAddressFee.root.hide()
                resetWalletValues()
                loadWallet(
                    onSuccess = {
                        containerRecipientAddressFee.root.show() { content.beginDelayedTransition() }
                        onWalletLoaded(walletManager)
                    },
                    onFailure = ::handleError
                )
            }
        }
        scope.launch(Dispatchers.Main) {
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
    }

    private fun loadWallet(onSuccess: () -> Unit, onFailure: (BlockchainSdkError) -> Unit) = with(binding) {
        walletManager = WalletManagerFactory().makeWalletManagerForApp(
            scanResponse = scanResponse,
            blockchain = Blockchain.fromId(selectedBlockchain.id),
            derivationParams = scanResponse.card.derivationParams(null)
        )!!
        containerSelectWalletWithBlockchain.tvBlockchainAddress.text = walletManager.wallet.address

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
//                        btnSend.isEnabled = true
                        val fees = feeResult.data
                        if (fees.size == 1) {
                            containerRecipientAddressFee.tvFeeAverage.text = fees[0].value.toString()
                            selectedFee = fees[0].value ?: BigDecimal(0)
                        } else {
                            containerRecipientAddressFee.tvFeeMin.text = fees[0].value?.stripZeroPlainString()
                            containerRecipientAddressFee.tvFeeAverage.text = fees[1].value?.stripZeroPlainString()
                            containerRecipientAddressFee.tvFeeMax.text = fees[2].value?.stripZeroPlainString()
                            selectedFee = fees[1].value ?: BigDecimal(0)
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