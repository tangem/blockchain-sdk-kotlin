package com.tangem.blockchain.nft.providers.moralis.solana

internal object MoralisSolanaFixtures {

    val coreResponse = """
    {
      "meta": { "syncedAt": { "solana-mainnet": "latest" }, "unsupportedChains": [], "failedChains": [] },
      "cursor": "next-page",
      "result": [
        {
          "tokenAddress": "CoreAsset1111111111111111111111111111111111",
          "tokenStandard": "MplCore",
          "amount": "1",
          "name": "Card #1",
          "metadata": {
            "name": "Card #1",
            "description": "A graded card",
            "image": "https://example.com/1-metadata.png",
            "animationUrl": null,
            "attributes": [{ "traitType": "Background", "value": "Purple" }, { "traitType": "Grade", "value": 10 }]
          },
          "media": {
            "mimetype": "image/png",
            "originalMediaUrl": "https://example.com/1.png",
            "mediaCollection": {
              "low": { "url": "https://media.moralis.io/low.png" },
              "medium": { "url": "https://media.moralis.io/medium.png" },
              "high": { "url": "https://media.moralis.io/high.png" }
            }
          },
          "collection": { "address": "CoreCollection11111111111111111111111111111", "name": "Collector Crypt" }
        },
        {
          "tokenAddress": "CoreAsset2222222222222222222222222222222222",
          "tokenStandard": "MplCore",
          "name": "Video card",
          "metadata": { "image": "ipfs://QmImage/2.png", "attributes": [] },
          "media": { "mimetype": "video/mp4", "originalMediaUrl": "https://example.com/2.mp4" },
          "collection": { "address": null, "name": null }
        },
        {
          "tokenAddress": "Compressed33333333333333333333333333333333",
          "tokenStandard": "Compressed",
          "name": "Not core"
        },
        { "tokenAddress": null, "tokenStandard": "MplCore" }
      ]
    }
    """.trimIndent()

    val failedChainResponse = """
    {
      "meta": { "failedChains": [{ "chainId": "solana-mainnet", "code": "INTERNAL_SERVER_ERROR", "error": "x" }] },
      "cursor": null,
      "result": []
    }
    """.trimIndent()

    val legacyOneAddressVariousNames = """
    [
      { "mint": "Mint1", "tokenStandard": 4, "name": "Named", "collection": { "collectionAddress": "CC", "name": "Collector Crypt" } },
      { "mint": "Mint2", "tokenStandard": 4, "name": "Empty-named", "collection": { "collectionAddress": "CC", "name": "" } },
      { "mint": "Mint3", "tokenStandard": 4, "name": "Unnamed", "collection": { "collectionAddress": "CC", "name": null } }
    ]
    """.trimIndent()

    val legacyAddressless = """
    [
      {
        "mint": "Mint4",
        "tokenStandard": 4,
        "name": "Named loose",
        "collection": { "collectionAddress": null, "name": "Loose", "description": "Named description" }
      },
      { "mint": "Mint5", "tokenStandard": 4, "name": "Empty address", "collection": { "collectionAddress": "", "name": "Empty" } },
      { "mint": "Mint6", "tokenStandard": 4, "name": "No collection" }
    ]
    """.trimIndent()
}