import com.google.protobuf.ByteString
import com.tangem.blockchain.blockchains.cosmos.CosmosTransactionBuilder
import com.tangem.blockchain.blockchains.cosmos.network.CosmosChain
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.extensions.successOr
import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.hexToBytes
import org.junit.Assert.assertEquals
import org.junit.Test
import wallet.core.jni.*
import wallet.core.jni.CoinType.COSMOS
import wallet.core.jni.proto.Cosmos
import wallet.core.jni.proto.Cosmos.SigningOutput
import wallet.core.jni.proto.Cosmos.SigningMode
import wallet.core.java.AnySigner
import java.math.BigDecimal

class CosmosTransactionTest {

    init {
        System.loadLibrary("TrustWalletCore")
    }

    private val seedKey = byteArrayOf(
        2, -30, -43, 25, 85, 93, 103, -26, 0, -37, 27, -102, 51, 106, 41, -109, 82,
        -21, -26, -73, -71, -28, 127, 110, 110, -99, 89, 94, -7, 85, 25, -20, -90
    )

    private val derivedKey = byteArrayOf(
        2, -40, -68, 33, -71, -109, 85, -40, 123, 22, -61, -40, 47, 23, -112, -48, 74,
        -84, -43, 87, -75, 56, -93, -89, 33, 57, -21, -12, -31, 71, 111, 49, 26
    )

    private val signature = byteArrayOf(
        65, 26, -22, 18, -112, -122, -4, -4, 104, -53, 89, 92, -113, 58, 123, 88, -5, 17, 45, 1, 15, 25, 75, 2, 48, 81, 105, 70, -39, 78, 1, -73, 67, 106, 51, 104, 91, -102, -125, 97, -49, 90, 55, 62, -48, 110, -14, 31, -15, -92, 19, -62, -110, 51, -86, -53, -55, -93, 122, 108, -68, 72, -56, 6
    )

    private val publicKey = Wallet.PublicKey(seedKey, derivedKey, null)

    @Test
    fun testTransaction() {
        val transactionBuilder = CosmosTransactionBuilder(
            cosmosChain = CosmosChain.Cosmos(true),
            publicKey = publicKey
        )

        // val input = transactionBuilder.buildForSign(
        //     amount = Amount(
        //         currencySymbol = "ATOM",
        //         value = "0.05".toBigDecimal(),
        //         decimals = 6,
        //         type = AmountType.Coin
        //
        //     ),
        //     source = "cosmos1tqksn8j4kj0feed2sglhfujp5amkndyac4z8jy",
        //     destination = "cosmos1z56v8wqvgmhm3hmnffapxujvd4w4rkw6cxr8xy",
        //     accountNumber = 726521,
        //     sequenceNumber = 15,
        //     feeAmount = Amount(
        //         currencySymbol = "ATOM",
        //         value = BigDecimal.valueOf(0.002717),
        //         decimals = 6,
        //         type = AmountType.Coin
        //     ),
        //     gas = 108700,
        //     extras = null,
        // )

        val message = transactionBuilder.buildForSend(
            amount = Amount(
                currencySymbol = "ATOM",
                value = "0.05".toBigDecimal(),
                decimals = 6,
                type = AmountType.Coin

            ),
            source = "cosmos1tqksn8j4kj0feed2sglhfujp5amkndyac4z8jy",
            destination = "cosmos1z56v8wqvgmhm3hmnffapxujvd4w4rkw6cxr8xy",
            accountNumber = 726521,
            sequenceNumber = 16,
            feeAmount = Amount(
                currencySymbol = "ATOM",
                value = BigDecimal.valueOf(0.002717),
                decimals = 6,
                type = AmountType.Coin
            ),
            gas = 108700,
            extras = null,
            signature = signature
        )

        val expected =
            "{\"mode\":\"BROADCAST_MODE_SYNC\",\"tx_bytes\":\"CpEBCo4BChwvY29zbW9zLmJhbmsudjFiZXRhMS5Nc2dTZW5kEm4KLWNvc21vczF0cWtzbjhqNGtqMGZlZWQyc2dsaGZ1anA1YW1rbmR5YWM0ejhqeRItY29zbW9zMXo1NnY4d3F2Z21obTNobW5mZmFweHVqdmQ0dzRya3c2Y3hyOHh5Gg4KBXVhdG9tEgU1MDAwMBJnClAKRgofL2Nvc21vcy5jcnlwdG8uc2VjcDI1NmsxLlB1YktleRIjCiEC2LwhuZNV2HsWw9gvF5DQSqzVV7U4o6chOev04UdvMRoSBAoCCAEYEBITCg0KBXVhdG9tEgQyNzE3EJzRBhpAQRrqEpCG/Pxoy1lcjzp7WPsRLQEPGUsCMFFpRtlOAbdDajNoW5qDYc9aNz7QbvIf8aQTwpIzqsvJo3psvEjIBg==\"}"
        val actual = message

        assertEquals(expected, actual)
    }

    @Test
    fun testAuthStakingTransaction() {
        val key = PrivateKey("c7764249cdf77f8f1d840fa8af431579e5e41cf1af937e1e23afa22f3f4f0ccc".hexToBytes())

        val stakeAuth = Cosmos.Message.StakeAuthorization.newBuilder().apply {
            allowList = Cosmos.Message.StakeAuthorization.Validators.newBuilder().apply {
                addAddress("cosmosvaloper1gjtvly9lel6zskvwtvlg5vhwpu9c9waw7sxzwx")
            }.build()
            authorizationType = Cosmos.Message.AuthorizationType.DELEGATE
        }.build()

        val authStakingMsg = Cosmos.Message.AuthGrant.newBuilder().apply {
            grantee = "cosmos1fs7lu28hx5m9akm7rp0c2422cn8r2f7gurujhf"
            granter = "cosmos13k0q0l7lg2kr32kvt7ly236ppldy8v9dzwh3gd"
            grantStake = stakeAuth
            expiration = 1692309600
        }.build()

        val message = Cosmos.Message.newBuilder().apply {
            authGrant = authStakingMsg
        }.build()

        val feeAmount = Cosmos.Amount.newBuilder().apply {
            amount = "2418"
            denom = "uatom"
        }.build()

        val cosmosFee = Cosmos.Fee.newBuilder().apply {
            gas = 96681
            addAllAmounts(listOf(feeAmount))
        }.build()

        val signingInput = Cosmos.SigningInput.newBuilder().apply {
            signingMode = SigningMode.Protobuf
            accountNumber = 1290826
            chainId = "cosmoshub-4"
            memo = ""
            sequence = 5
            fee = cosmosFee
            privateKey = ByteString.copyFrom(key.data())
            addAllMessages(listOf(message))
        }.build()

        val output = AnySigner.sign(signingInput, COSMOS, SigningOutput.parser())

        assertEquals(
            output.serialized,
            "{\"mode\":\"BROADCAST_MODE_BLOCK\",\"tx_bytes\":\"CvgBCvUBCh4vY29zbW9zLmF1dGh6LnYxYmV0YTEuTXNnR3JhbnQS0gEKLWNvc21vczEzazBxMGw3bGcya3IzMmt2dDdseTIzNnBwbGR5OHY5ZHp3aDNnZBItY29zbW9zMWZzN2x1MjhoeDVtOWFrbTdycDBjMjQyMmNuOHIyZjdndXJ1amhmGnIKaAoqL2Nvc21vcy5zdGFraW5nLnYxYmV0YTEuU3Rha2VBdXRob3JpemF0aW9uEjoSNgo0Y29zbW9zdmFsb3BlcjFnanR2bHk5bGVsNnpza3Z3dHZsZzV2aHdwdTljOXdhdzdzeHp3eCABEgYI4LD6pgYSZwpQCkYKHy9jb3Ntb3MuY3J5cHRvLnNlY3AyNTZrMS5QdWJLZXkSIwohA/fcQw1hCVUx904t+kCXTiiziaLIY8lyssu1ENfzaN1KEgQKAggBGAUSEwoNCgV1YXRvbRIEMjQxOBCp8wUaQIFyfuijGKf87Hz61ZqxasfLI1PZnNge4RDq/tRyB/tZI6p80iGRqHecoV6+84EQkc9GTlNRQOSlApRCsivT9XI=\"}"
        )
    }

    @Test
    fun testRemoveAuthStakingTransaction() {
        val key =
            PrivateKey("c7764249cdf77f8f1d840fa8af431579e5e41cf1af937e1e23afa22f3f4f0ccc".hexToBytes())

        val removeAuthStakingMsg = Cosmos.Message.AuthRevoke.newBuilder().apply {
            grantee = "cosmos1fs7lu28hx5m9akm7rp0c2422cn8r2f7gurujhf"
            granter = "cosmos13k0q0l7lg2kr32kvt7ly236ppldy8v9dzwh3gd"
            msgTypeUrl = "/cosmos.staking.v1beta1.MsgDelegate"
        }.build()

        val message = Cosmos.Message.newBuilder().apply {
            authRevoke = removeAuthStakingMsg
        }.build()

        val feeAmount = Cosmos.Amount.newBuilder().apply {
            amount = "2194"
            denom = "uatom"
        }.build()

        val cosmosFee = Cosmos.Fee.newBuilder().apply {
            gas = 87735
            addAllAmounts(listOf(feeAmount))
        }.build()

        val signingInput = Cosmos.SigningInput.newBuilder().apply {
            signingMode = SigningMode.Protobuf
            accountNumber = 1290826
            chainId = "cosmoshub-4"
            memo = ""
            sequence = 4
            fee = cosmosFee
            privateKey = ByteString.copyFrom(key.data())
            addAllMessages(listOf(message))
        }.build()

        val output = AnySigner.sign(signingInput, COSMOS, SigningOutput.parser())

        assertEquals(
            output.serialized,
            "{\"mode\":\"BROADCAST_MODE_BLOCK\",\"tx_bytes\":\"CqoBCqcBCh8vY29zbW9zLmF1dGh6LnYxYmV0YTEuTXNnUmV2b2tlEoMBCi1jb3Ntb3MxM2swcTBsN2xnMmtyMzJrdnQ3bHkyMzZwcGxkeTh2OWR6d2gzZ2QSLWNvc21vczFmczdsdTI4aHg1bTlha203cnAwYzI0MjJjbjhyMmY3Z3VydWpoZhojL2Nvc21vcy5zdGFraW5nLnYxYmV0YTEuTXNnRGVsZWdhdGUSZwpQCkYKHy9jb3Ntb3MuY3J5cHRvLnNlY3AyNTZrMS5QdWJLZXkSIwohA/fcQw1hCVUx904t+kCXTiiziaLIY8lyssu1ENfzaN1KEgQKAggBGAQSEwoNCgV1YXRvbRIEMjE5NBC3rQUaQI7K+W7MMBoD6FbFZxRBqs9VTjErztjWTy57+fvrLaTCIZ+eBs7CuaKqfUZdSN8otjubSHVTQID3k9DpPAX0yDo=\"}"
        )
    }

    @Test
    fun testSigningTransaction() {
        val key =
            PrivateKey("80e81ea269e66a0a05b11236df7919fb7fbeedba87452d667489d7403a02f005".hexToBytes())
        val publicKey = key.getPublicKeySecp256k1(true)
        val from = AnyAddress(publicKey, COSMOS).description()

        val txAmount = Cosmos.Amount.newBuilder().apply {
            amount = "1"
            denom = "muon"
        }.build()

        val sendCoinsMsg = Cosmos.Message.Send.newBuilder().apply {
            fromAddress = from
            toAddress = "cosmos1zt50azupanqlfam5afhv3hexwyutnukeh4c573"
            addAllAmounts(listOf(txAmount))
        }.build()

        val message = Cosmos.Message.newBuilder().apply {
            sendCoinsMessage = sendCoinsMsg
        }.build()

        val feeAmount = Cosmos.Amount.newBuilder().apply {
            amount = "200"
            denom = "muon"
        }.build()

        val cosmosFee = Cosmos.Fee.newBuilder().apply {
            gas = 200000
            addAllAmounts(listOf(feeAmount))
        }.build()

        val signingInput = Cosmos.SigningInput.newBuilder().apply {
            signingMode = SigningMode.Protobuf
            accountNumber = 1037
            chainId = "gaia-13003"
            memo = ""
            sequence = 8
            fee = cosmosFee
            privateKey = ByteString.copyFrom(key.data())
            addAllMessages(listOf(message))
        }.build()

        val output = AnySigner.sign(signingInput, COSMOS, SigningOutput.parser())

        assertEquals(
            output.serialized,
            "{\"mode\":\"BROADCAST_MODE_BLOCK\",\"tx_bytes\":\"CowBCokBChwvY29zbW9zLmJhbmsudjFiZXRhMS5Nc2dTZW5kEmkKLWNvc21vczFoc2s2anJ5eXFqZmhwNWRoYzU1dGM5anRja3lneDBlcGg2ZGQwMhItY29zbW9zMXp0NTBhenVwYW5xbGZhbTVhZmh2M2hleHd5dXRudWtlaDRjNTczGgkKBG11b24SATESZQpQCkYKHy9jb3Ntb3MuY3J5cHRvLnNlY3AyNTZrMS5QdWJLZXkSIwohAlcobsPzfTNVe7uqAAsndErJAjqplnyudaGB0f+R+p3FEgQKAggBGAgSEQoLCgRtdW9uEgMyMDAQwJoMGkD54fQAFlekIAnE62hZYl0uQelh/HLv0oQpCciY5Dn8H1SZFuTsrGdu41PH1Uxa4woptCELi/8Ov9yzdeEFAC9H\"}"
        )
    }

    @Test
    fun testSigningJSON() {
        val json = """
        {
            "accountNumber": "8733",
            "chainId": "cosmoshub-2",
            "fee": {
                "amounts": [{
                    "denom": "uatom",
                    "amount": "5000"
                }],
                "gas": "200000"
            },
            "memo": "Testing",
            "messages": [{
                "sendCoinsMessage": {
                    "fromAddress": "cosmos1ufwv9ymhqaal6xz47n0jhzm2wf4empfqvjy575",
                    "toAddress": "cosmos135qla4294zxarqhhgxsx0sw56yssa3z0f78pm0",
                    "amounts": [{
                        "denom": "uatom",
                        "amount": "995000"
                    }]
                }
            }]
        }
        """
        val key = "c9b0a273831931aa4a5f8d1a570d5021dda91d3319bd3819becdaabfb7b44e3b".hexToBytes()
        val result = AnySigner.signJSON(json, key, COSMOS.value())
        assertEquals(
            result,
            """{"mode":"block","tx":{"fee":{"amount":[{"amount":"5000","denom":"uatom"}],"gas":"200000"},"memo":"Testing","msg":[{"type":"cosmos-sdk/MsgSend","value":{"amount":[{"amount":"995000","denom":"uatom"}],"from_address":"cosmos1ufwv9ymhqaal6xz47n0jhzm2wf4empfqvjy575","to_address":"cosmos135qla4294zxarqhhgxsx0sw56yssa3z0f78pm0"}}],"signatures":[{"pub_key":{"type":"tendermint/PubKeySecp256k1","value":"A6EsukEXB53GhohQVeDpxtkeH8KQIayd/Co/ApYRYkTm"},"signature":"ULEpUqNzoAnYEx2x22F3ANAiPXquAU9+mqLWoAA/ZOUGTMsdb6vryzsW6AKX2Kqj1pGNdrTcQ58Z09JPyjpgEA=="}]}}"""
        )
    }
}