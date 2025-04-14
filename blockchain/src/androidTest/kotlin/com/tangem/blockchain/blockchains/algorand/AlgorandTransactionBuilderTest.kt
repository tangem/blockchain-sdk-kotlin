package com.tangem.blockchain.blockchains.algorand

import com.google.common.truth.Truth
import com.tangem.blockchain.blockchains.algorand.models.AlgorandTransactionBuildParams
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.extensions.preparePublicKeyByType
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toHexString
import org.junit.Test
import wallet.core.jni.CoinType
import wallet.core.jni.PublicKey
import java.math.BigDecimal

class AlgorandTransactionBuilderTest {

    init {
        System.loadLibrary("TrustWalletCore")
    }

    // https://app.dappflow.org/explorer/transaction/M4PBLJ5ANB2FSH537727CHB44IQGNFXSFUJOFV55ZXTHLS4KNCHA
    @Test
    fun testTransactionBuilder() {
        val blockchain = Blockchain.Algorand
        val amount = Amount(BigDecimal("0.001"), blockchain)
        val fee = Fee.Common(Amount(BigDecimal("0.001000"), blockchain))

        val walletPublicKey = "67CFA0C50B5A46A3FF6FD38CB6D6E45725EAC937A79E3528A13A71BC006F877E".hexToBytes()
        val coinType = CoinType.ALGORAND
        val publicKey = PublicKey(coinType.preparePublicKeyByType(walletPublicKey), coinType.publicKeyType())
        val transactionBuilder = AlgorandTransactionBuilder(
            publicKey = publicKey.data(),
            blockchain = Blockchain.Algorand,
        )

        val transaction = TransactionData.Uncompiled(
            amount = amount,
            fee = fee,
            sourceAddress = "M7H2BRILLJDKH73P2OGLNVXEK4S6VSJXU6PDKKFBHJY3YADPQ57HN4EI64",
            destinationAddress = "Q7AUUQCAO3O6CLPHMPTWN3VTCWLLWZJSI6QDO5XEC4ZZR5JZWXWZL5YWOM",
        )
        val buildParams = AlgorandTransactionBuildParams(
            genesisId = "mainnet-v1.0",
            genesisHash = "wGHE2Pwdvd7S12BL5FaOP20EGYesN73ktiC1qzkkit8=",
            firstRound = 36253878,
            lastRound = 36254878,
        )

        val buildForSign = transactionBuilder.buildForSign(transaction, buildParams)
        val expectedBuildForSign =
            "545889A3616D74CD03E8A3666565CD03E8A26676CE022930B6A367656EAC6D61696E6E65742D76312E30A26768C420C061C4D8FC1DBDDED2D7604BE4568E3F6D041987AC37BDE4B620B5AB39248ADFA26C76CE0229349EA3726376C42087C14A404076DDE12DE763E766EEB31596BB653247A03776E4173398F539B5EDA3736E64C42067CFA0C50B5A46A3FF6FD38CB6D6E45725EAC937A79E3528A13A71BC006F877EA474797065A3706179"
        Truth.assertThat(buildForSign.toHexString()).isEqualTo(expectedBuildForSign)

        val signature =
            "98008C07BB037AD601681CA5B1A48C3A40CD3879DC277035D06064D31459C11D04F53A46CE1909260A330204F16A448BAF11D6B8E5101C1817AD61C17ADC1103"
        val buildForSend = transactionBuilder.buildForSend(transaction, buildParams, signature.hexToBytes())
        val expectedSendResult =
            "82A3736967C44098008C07BB037AD601681CA5B1A48C3A40CD3879DC277035D06064D31459C11D04F53A46CE1909260A330204F16A448BAF11D6B8E5101C1817AD61C17ADC1103A374786E89A3616D74CD03E8A3666565CD03E8A26676CE022930B6A367656EAC6D61696E6E65742D76312E30A26768C420C061C4D8FC1DBDDED2D7604BE4568E3F6D041987AC37BDE4B620B5AB39248ADFA26C76CE0229349EA3726376C42087C14A404076DDE12DE763E766EEB31596BB653247A03776E4173398F539B5EDA3736E64C42067CFA0C50B5A46A3FF6FD38CB6D6E45725EAC937A79E3528A13A71BC006F877EA474797065A3706179"
        Truth.assertThat(buildForSend.toHexString()).isEqualTo(expectedSendResult)
    }
}