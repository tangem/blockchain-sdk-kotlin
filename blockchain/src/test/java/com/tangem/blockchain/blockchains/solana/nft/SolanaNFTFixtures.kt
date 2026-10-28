package com.tangem.blockchain.blockchains.solana.nft

import java.util.Base64

/** Mainnet accounts read on 14.10.2026, shared by the pNFT and Metaplex Core tests. */
internal object SolanaNFTFixtures {

    /** `Metadata` account `4SCNiHv6…` of pNFT mint `7Kny3iBH…`; rule set `7j2KxTBn…`. */
    val pnftMetadataAccount: ByteArray = Base64.getDecoder().decode(
        "BH77qzv3ukVp4w6aJ96Mgce9PvztdN1l4+LflSigeGW/XfaM55AZFViiGlCXseIwMiQ5Bt1T9V3MB768Y3M9SJcgAAAAUmVn" +
            "aW9uIDc5ICMyODE2AAAAAAAAAAAAAAAAAAAAAAAKAAAAUkVHSU9OAAAAAMgAAABodHRwczovL2JhZnliZWljeWpxeGU0dmQ3" +
            "eDYzeWQ3a3BnNG1saGtvdXQzN2htYW50N243eXlncHJ0ZzIzMndnNnNpLmlwZnMubmZ0c3RvcmFnZS5saW5rLzI4MTYuanNv" +
            "bgAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" +
            "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAALwCAQUAAABZXAeAI7443IPeej/GveXikl1Vuyy26zPD0j1e0DOzHAEA" +
            "QuHhgi8DuCecYvMQZTpduLVqfiK6BsADaFnYEcoj6ggAVZiZTJbsGtilESlaiM7zaFrBnkDReBTzOR1wHru51Jl2AAWN1wGE" +
            "jE11z4og+rlFP5Ciz14FbxTl3URMRR+2Un/fLwAFBkGcBkUw9tZCZ0LZO/zNm0eC2ydanwqeaU/Slb/O++0ABQEBAf8BBAAA" +
            "AAEAAWPqBArCVN4aRmyL44LQN1kb3ktQly8QTVeSUE9d3KjZAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" +
            "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA==",
    )

    /** Collector Crypt card `3xYi4gjZ…` (`AssetV1`, update authority = collection `CCryptUf…`). */
    val coreAssetAccount: ByteArray = Base64.getDecoder().decode(
        "AaqG1ub7Gwt3NtdoDiVAXjJ0HulDNsf8P34LNgJ+V16pAqZ6dRaY8L8+Hhp++hRPRifaIbCLArWnKuZKz/eETB71IAAAADIw" +
            "MDIgUG9rZW1vbiBOZW8gRGVzdGlueSBVbm93biBUPwAAAGh0dHBzOi8vYXJ3ZWF2ZS5uZXQvZVRKMXZhUWtvY1Y3Nk1vTkdK" +
            "SFVyMXhBRk1tSm1jT2Y5OVprOE85QUdzUQADtQAAAAAAAAAFAAQBAAAABQPJjkAsrQawTeH+/5LTROkPW7AiNa7dg+uqIX9a" +
            "uuKPRbMAAAAAAAAAAAAAAA==",
    )

    val pnftMetadataAccountBase64: String = Base64.getEncoder().encodeToString(pnftMetadataAccount)
    val coreAssetAccountBase64: String = Base64.getEncoder().encodeToString(coreAssetAccount)
}