package com.tangem.blockchain.blockchains.chia
// TODO blst library doesn't work with tests yet
//
// import com.google.common.truth.Truth
// import com.tangem.blockchain.blockchains.chia.network.ChiaCoin
// import com.tangem.blockchain.blockchains.chia.network.ChiaCoinSpend
// import com.tangem.blockchain.blockchains.chia.network.ChiaSpendBundle
// import com.tangem.blockchain.common.Amount
// import com.tangem.blockchain.common.AmountType
// import com.tangem.blockchain.common.Blockchain
// import com.tangem.blockchain.common.TransactionData
// import com.tangem.blockchain.common.transaction.Fee
// import com.tangem.blockchain.extensions.Result
// import com.tangem.common.extensions.hexToBytes
// import org.junit.Test
//
// class ChiaTransactionTest {
//     private val blockchain = Blockchain.Chia
//     private val decimals = blockchain.decimals()
//     private val addressService = ChiaAddressService(blockchain)
//
//     @Test
//     fun buildCorrectTransaction() {
//         // arrange
//         val walletPublicKey = "B6B57E5E5BFDE70E404CE83732548DB6E5BD1740C96F878B699E896FD99269B0EF10BD1C6A64E65A87C9AD444BA6E3CC"
//             .hexToBytes()
//         val signature1 =
//             "8C7470BEE98156B48A0909F6EF321DE86F073101399ACD160ACFEF57B943B6E76E22DC89D9C75ABBFAC97DC317FEA3CC0AD744F55E2EAA3AE3C099AFC89FE652B8B054C5AB1F6A11559A9BCFD132EE0F434BA4D7968A33EA1807CFAB097789B7"
//                 .hexToBytes()
//         val signature2 =
//             "93CFBA81239EAD3358E780073DCC9553097F377B217A8FE04CB07D4FC634F2A094425D8A9E8E2373880AD944EDB55ECF16D59F031986E9EFEB92290C3E7285227890E7FC3EAFFC84B84F225E62CFA5ED681DCE6993C9845543AA493180B28B04"
//                 .hexToBytes()
//         val sendValue = "0.1".toBigDecimal()
//         val feeValue = "0.000000105869".toBigDecimal()
//         val destinationAddress = "xch1wd52fhrnp2jjyxsqecfvkzq6geu3kxg9trq7m49ff0aadyxlclns7es9ph"
//
//         val sourceAddress = addressService.makeAddress(walletPublicKey)
//
//         val transactionBuilder = ChiaTransactionBuilder(walletPublicKey, blockchain)
//         val unspentCoins = listOf(
//             ChiaCoin(
//                 amount = 99790386L,
//                 parentCoinInfo = "0x380ae38b677990a085ad7a9501da51a548f4241dbe73f78e969f019585e268a4",
//                 puzzleHash = "0x5d467bdc46c20f175024977ef0c2ae985abf9aea5151acbd6c54071de87a402b"
//             ),
//             ChiaCoin(
//                 amount = 114599994000L,
//                 parentCoinInfo = "114599994000",
//                 puzzleHash = "0x5d467bdc46c20f175024977ef0c2ae985abf9aea5151acbd6c54071de87a402b"
//             )
//         )
//         transactionBuilder.unspentCoins = unspentCoins
//
//         val amountToSend = Amount(sendValue, blockchain, AmountType.Coin)
//         val fee = Fee.Common(Amount(amountToSend, feeValue))
//         val transactionData = TransactionData(
//             sourceAddress = sourceAddress,
//             destinationAddress = destinationAddress,
//             amount = amountToSend,
//             fee = fee
//         )
//
//         val expectedHashToSign1 =
//             "A3A6136282A97B09CAE57DFAD492B78EAE685E2D55E3279D18B41CB11D2B0260EF6E5B2AE15E98956B0C4E652F86714203FFF9EFA7FED9D3ACF053FE697EE4D832B21484A711EB70989E2720FD262E8AD3E474909E7098DABD33870EF5DBC13A"
//                 .hexToBytes().toList()
//         val expectedHashToSign2 =
//             "8A4D295B39FF301BD565A98FC44E3F07D6B16421BC03FAF1B340AC7DF6230F979AA26CFA7086E9E3C4C09F2904FF5E8614AEA5F7F5883A2F7F20FF5F18C1BAAD0F895B2ABC8BBFF36254A5442CD6A6700A4E8ADFA1C9F7F8558EE23327592036"
//                 .hexToBytes().toList()
//         val expectedSignedTransaction = ChiaSpendBundle(
//             aggregatedSignature = "93821E46F8A8FD5F38A63EF8B31D5DC0575B537DFE90CB2C03ADCF49C0AC3864BB5510B7908AC7D9B6DA41DFB09AEFC317C264697D40CA58C93A6EF8C66E0310B72389FFFCE69022FDC63E5B0CDD911FFDC4B45E56EE55ACBA936C4B26A8E71F",
//             coinSpends = listOf(
//                 ChiaCoinSpend(
//                     coin = unspentCoins[0],
//                     puzzleReveal = "FF02FFFF01FF02FFFF01FF04FFFF04FF04FFFF04FF05FFFF04FFFF02FF06FFFF04FF02FFFF04FF0BFF80808080FF80808080FF0B80FFFF04FFFF01FF32FF02FFFF03FFFF07FF0580FFFF01FF0BFFFF0102FFFF02FF06FFFF04FF02FFFF04FF09FF80808080FFFF02FF06FFFF04FF02FFFF04FF0DFF8080808080FFFF01FF0BFFFF0101FF058080FF0180FF018080FFFF04FFFF01B0B6B57E5E5BFDE70E404CE83732548DB6E5BD1740C96F878B699E896FD99269B0EF10BD1C6A64E65A87C9AD444BA6E3CCFF018080",
//                     solution = "FFFFFF33FFA07368A4DC730AA5221A00CE12CB081A46791B190558C1EDD4A94BFBD690DFC7E7FF85174876E80080FFFF33FFA05D467BDC46C20F175024977EF0C2AE985ABF9AEA5151ACBD6C54071DE87A402BFF85036C2B4B35808080"
//                 ),
//                 ChiaCoinSpend(
//                     coin = unspentCoins[0],
//                     puzzleReveal = "FF02FFFF01FF02FFFF01FF04FFFF04FF04FFFF04FF05FFFF04FFFF02FF06FFFF04FF02FFFF04FF0BFF80808080FF80808080FF0B80FFFF04FFFF01FF32FF02FFFF03FFFF07FF0580FFFF01FF0BFFFF0102FFFF02FF06FFFF04FF02FFFF04FF09FF80808080FFFF02FF06FFFF04FF02FFFF04FF0DFF8080808080FFFF01FF0BFFFF0101FF058080FF0180FF018080FFFF04FFFF01B0B6B57E5E5BFDE70E404CE83732548DB6E5BD1740C96F878B699E896FD99269B0EF10BD1C6A64E65A87C9AD444BA6E3CCFF018080",
//                     solution = "FFFFFF01808080"
//                 )
//             )
//         )
//
//         // act
//         val buildToSignResult = transactionBuilder.buildToSign(transactionData) as Result.Success
//         val signedTransaction = transactionBuilder.buildToSend(listOf(signature1, signature2))
//
//         // assert
//         Truth.assertThat(buildToSignResult.data.map { it.toList() })
//             .containsExactly(expectedHashToSign1, expectedHashToSign2)
//         Truth.assertThat(signedTransaction).isEqualTo(expectedSignedTransaction)
//     }
// }