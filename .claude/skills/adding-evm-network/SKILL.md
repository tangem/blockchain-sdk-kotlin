---
name: adding-evm-network
description: Use when adding a new EVM-compatible network (chain id, RPC providers, explorer) to blockchain-sdk-kotlin, or when an existing EVM network misbehaves because a registration point was missed — missing chain id, wrong derivation path, no explorer links, no assets discovery, coin shown standalone instead of under ETH.
---

# Adding an EVM network

## Overview

Adding an EVM chain is **pure registration**: no new WalletManager, no new TransactionBuilder.
`EthereumLikeWalletManagerAssembly` + `EthereumWalletManager` already handle everything.

The work is 2 new files + ~16 edit points, mostly branches in existing `when` blocks.

**Core principle: only 8 of those edit points are compiler-enforced. Two fail at runtime, five fail
silently** (`else -> null`, `else -> false`, `else -> DefaultXxx`). A build that compiles proves nothing.

**Derive the checklist mechanically, never from memory:**

```bash
grep -rn "SeiEvm" --include="*.kt" .    # last plain EVM chain — full occurrence list
git log --oneline -- blockchain/src/main/java/com/tangem/blockchain/blockchains/ethereum/Chain.kt
git show --stat <that commit>           # exact file set of a real addition
```

Reference commits: `09f5a5ca` (Robinhood), `e2e2d23b` (Igra, + tx history + tests), `ad8b41bd`
(the L2 follow-up fix that the first commit missed).

## Step 0 — Collect inputs before editing anything

Do not invent these. Ask if not given:

| Input | Used in | Note |
|---|---|---|
| `id` slug, mainnet + testnet | `Blockchain` enum | Must match the backend network id. Convention: `"foo"` / `"foo/test"` |
| `currency` symbol | `Blockchain` enum | If it is `"ETH"` → the chain is an ETH L2, see trap #2 |
| `fullName` | `Blockchain` enum | `"Foo"` / `"Foo Testnet"` |
| chain id, mainnet + testnet | `Chain` enum | decimal, from chainlist |
| decimals | `Blockchain.decimals()` | 18 for almost every EVM chain |
| public RPC URLs | providers builder / tests | mainnet URLs usually come from backend `ProviderType.Public`; testnet URL is hardcoded |
| private RPC (QuickNode/Alchemy/NowNodes) | providers builder + `BlockchainSdkConfig` | only if the chain actually has one |
| explorer base URL | external link provider | Blockscout and Etherscan have different URL shapes |
| EIP-1559 support | `isSupportEIP1559` | if unsure — `eth_feeHistory` must return non-zero `baseFeePerGas` |
| explorer API (Etherscan-compatible?) | `TransactionHistoryProviderFactory` | optional, skip if none |

## Registration checklist

All paths relative to `blockchain/src/main/java/com/tangem/blockchain/`.
"Enforced" = the `when` is exhaustive, so the build breaks if you forget it.

| # | File | What to add | Enforced |
|---|---|---|---|
| 1 | `common/Blockchain.kt` — enum entries | `Foo("foo", "SYM", "Foo")`, `FooTestnet("foo/test", "SYM", "Foo Testnet")` | — |
| 2 | `common/Blockchain.kt` — `decimals()` | add to the `-> 18` group | ✅ |
| 3 | `common/Blockchain.kt` — `getAddressService()` | add to the `-> EthereumAddressService()` group | ✅ |
| 4 | `common/Blockchain.kt` — `getTestnetVersion()` | `Foo, FooTestnet -> FooTestnet` | ✅ |
| 5 | `common/Blockchain.kt` — `getSupportedCurves()` | add to `-> listOf(EllipticCurve.Secp256k1)` | ✅ |
| 6 | `common/Blockchain.kt` — `getChainId()` | `Foo -> Chain.Foo.id`, `FooTestnet -> Chain.FooTestnet.id` | ❌ `else -> null` |
| 7 | `common/Blockchain.kt` — `isL2EthereumNetwork()` | **only if `currency == "ETH"`** | ❌ `else -> false` |
| 8 | `blockchains/ethereum/Chain.kt` | `Foo(id = 1234, blockchain = Blockchain.Foo)` + testnet | — |
| 9 | `blockchains/ethereum/eip1559/EthereumLikeBlockchainExt.kt` | add to the `true` or the `false` group of `isSupportEIP1559` | ⚠️ runtime `error("Don't forget about evm here")` |
| 10 | `common/UtxoExt.kt` — `isUTXO` | add to the `-> false` list | ✅ |
| 11 | `common/WalletManagerFactory.kt` | add to the `-> EthereumLikeWalletManagerAssembly(...)` group | ❌ silently falls through |
| 12 | `common/assembly/impl/EthereumLikeWalletManagerAssembly.kt` | `Blockchain.Foo, Blockchain.FooTestnet -> FooProvidersBuilder(providerTypes, config)` | ⚠️ runtime `error("Unsupported blockchain")` |
| 13 | `common/address/EstimationFeeAddressFactory.kt` | add to the shared EVM address group | ✅ |
| 14 | `common/derivation/DerivationConfigV1.kt` / `V2` / `V3` | add to the `m/44'/60'/0'/0/0` group **in all three** | ✅ |
| 15 | `externallinkprovider/ExternalLinkProviderFactory.kt` | `-> FooExternalLinkProvider(isTestnet)` | ✅ |
| 16 | `assetsdiscovery/AssetsDiscoveryServiceFactory.kt` | `createDefaultEvmDiscoveryService(...)` branch **and** the KDoc table row | ❌ `else -> DefaultAssetsDiscoveryService` |
| 17 | `common/BlockchainSdkConfig.kt` | `quickNodeFooCredentials` — only if a private provider is used | — |
| 18 | `transactionhistory/TransactionHistoryProviderFactory.kt` | branch + `createFooExplorerProvider()` — only if the explorer has an Etherscan-compatible API | ❌ `else -> Default...` |

Tokens need no registration: `canHandleTokens()` returns true for any `isEvm()` chain, and
`isEvm()` is just `getChainId() != null` — so point 6 + 8 are the master switch.

## New file 1 — providers builder

`blockchains/foo/FooProvidersBuilder.kt`

```kotlin
internal class FooProvidersBuilder(
    override val providerTypes: List<ProviderType>,
    override val config: BlockchainSdkConfig,
) : EthereumLikeProvidersBuilder(config) {

    override fun createProviders(blockchain: Blockchain): List<EthereumJsonRpcProvider> {
        return providerTypes.mapNotNull { type ->
            when (type) {
                is ProviderType.Public -> EthereumJsonRpcProvider(baseUrl = type.url)
                ProviderType.QuickNode -> createQuickNodeProvider() // drop if unused
                else -> null
            }
        }
    }

    override fun createTestnetProviders(blockchain: Blockchain): List<EthereumJsonRpcProvider> {
        return listOf(EthereumJsonRpcProvider(baseUrl = TESTNET_URL))
    }

    private fun createQuickNodeProvider(): EthereumJsonRpcProvider? {
        return config.quickNodeFooCredentials?.let { credentials ->
            if (credentials.subdomain.isNotBlank() && credentials.apiKey.isNotBlank()) {
                EthereumJsonRpcProvider("https://${credentials.subdomain}/${credentials.apiKey}/")
            } else {
                null
            }
        }
    }

    private companion object {
        const val TESTNET_URL = "https://testnet-rpc.foo.io/"
    }
}
```

Rules: mainnet URLs come from `ProviderType.Public` supplied by the backend — do **not** hardcode
them; testnet URLs are hardcoded because the backend does not ship them.

## New file 2 — external link provider

`externallinkprovider/providers/FooExternalLinkProvider.kt`

```kotlin
internal class FooExternalLinkProvider(isTestnet: Boolean) : ExternalLinkProvider {

    override val explorerBaseUrl: String =
        if (isTestnet) "https://testnet.fooscan.io/" else "https://fooscan.io/"

    override val testNetTopUpUrl: String? = null // faucet URL if one exists

    override fun explorerUrl(walletAddress: String, contractAddress: String?): String =
        explorerBaseUrl + "address/$walletAddress"

    override fun getExplorerTxUrl(transactionHash: String): TxExploreState =
        TxExploreState.Url(explorerBaseUrl + "tx/$transactionHash")

    override fun getNFTExplorerUrl(assetIdentifier: NFTAsset.Identifier): String {
        require(assetIdentifier is NFTAsset.Identifier.EVM)
        return explorerBaseUrl + "nft/${assetIdentifier.tokenAddress}/${assetIdentifier.tokenId}"
    }
}
```

Verify the paths against the real explorer — Blockscout uses `address/`, `tx/`, `token/`;
some Etherscan forks differ. Open the URL, don't guess.

## Silent traps

**1. `getChainId()` forgotten.** `isEvm()` is `getChainId() != null`. Without it the chain is not an
EVM chain at all: tokens off, EIP-1559 off, fees broken. Compiles fine.

**2. ETH-native chain not in `isL2EthereumNetwork()`.** If `currency == "ETH"`, the app shows a
standalone `<network>-ethereum` coin instead of listing the network under ETH. This is exactly the bug
fixed in `ad8b41bd`. The invariant is covered by `BlockchainTypeTest.testEthNativeMainnetsMatchL2EthereumNetworks`
— that test will catch it, run it.

**3. `WalletManagerFactory` forgotten.** Falls through to another branch or throws at wallet creation,
not at build time.

**4. Only one `DerivationConfig` updated.** V1/V2/V3 are three separate `when` blocks; cards with a
different derivation style get a wrong address. All three are exhaustive, so the build breaks — but
only if you didn't paste the chain into a wrong existing group.

**5. Assets discovery KDoc table.** `AssetsDiscoveryServiceFactory` carries a `| Network | coins + tokens |`
table in its KDoc. Add the row — reviewers use it as the source of truth.

## Tests

Mirror `blockchain/src/test/java/com/tangem/blockchain/blockchains/igra/` (commit `e2e2d23b`).
Minimum set in `FooTest.kt`, one test per silent trap:

- chain id: `Chain.entries.find { it.blockchain == Blockchain.Foo }?.id` **and** `Blockchain.Foo.getChainId()`
- `isEvm()` true, `isUTXO` false
- `isSupportEIP1559` matches what you registered
- decimals / currency / fullName
- `getTestnetVersion()` + `isTestnet()`
- `getSupportedCurves()` == `[Secp256k1]`
- derivation path `m/44'/60'/0'/0/0` in V1, V2 **and** V3
- `WalletManagerFactory(...).createLegacyWalletManager(...)` returns `EthereumWalletManager`
- `AssetsDiscoveryServiceFactory(...).create(blockchain) != DefaultAssetsDiscoveryService`

Plus `FooProvidersBuilderTest` (mainnet from `ProviderType.Public`, testnet URL, unsupported types
ignored) and `FooExternalLinkProviderTest`. Truth (`com.google.common.truth.Truth`) + JUnit4.

## Verification — required before claiming done

```bash
./gradlew :blockchain:testDebugUnitTest --tests "com.tangem.blockchain.blockchains.foo.*"
./gradlew :blockchain:testDebugUnitTest --tests "com.tangem.blockchain.common.BlockchainTypeTest"
./gradlew :blockchain:testDebugUnitTest
./gradlew detekt          # maxIssues = 0, CI blocks on it
```

Then diff-check the registration set against a reference chain (`SeiEvm` = plain EVM, no tx history;
use `Igra` if your chain also has an Etherscan-compatible explorer API):

```bash
for f in $(grep -rl "SeiEvm" --include="*.kt" blockchain/src/main | grep -viE "seievm"); do
  grep -q "Foo" "$f" || echo "MISSING: $f"
done
```

Empty output = every shared registration point is covered. Any `MISSING:` line is a forgotten edit
point — fix it or state explicitly why it does not apply.

## Common mistakes

| Mistake | Consequence |
|---|---|
| Hardcoding mainnet RPC URLs in the builder | Backend cannot rotate providers; duplicates `ProviderType.Public` |
| Adding a `quickNode*Credentials` field with no actual provider | Dead config surface in the public SDK API |
| Copying a chain from a *non-plain* group (Optimism, Mantle, Scroll, Quai) | Those use their own assemblies/address services — the L2/rollup fee logic will be wrong |
| Guessing `id` slug | Wallet cannot match the network with the backend; tokens and rates break |
| `isSupportEIP1559 = true` without checking `eth_feeHistory` | Fee estimation returns zeros, transactions get stuck |
| Skipping the testnet | `getTestnetVersion()` and the derivation configs are exhaustive — mainnet-only additions won't compile cleanly anyway |