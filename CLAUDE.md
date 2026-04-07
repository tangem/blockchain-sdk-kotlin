# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Development Commands

```bash
# Build
./gradlew :blockchain:build                  # Build library module

# Tests
./gradlew :blockchain:testDebugUnitTest      # Unit tests
./gradlew :blockchain:testDebugUnitTest --tests "com.tangem.blockchain.SomeTest"  # Single test
./gradlew :blockchain:connectedDebugAndroidTest  # Instrumented tests (requires emulator)

# Publish
./gradlew :blockchain:publishToMavenLocal    # Publish library to local Maven (default version 0.0.1 from publish.properties)

# Code quality
./gradlew detekt                             # Static analysis (max issues = 0, strict)
```

CI runs `test` and `detekt` on PRs to develop/master/releases branches. Both must pass.

## Architecture

### Assembly Pattern

`WalletManagerFactory` is the entry point. Constructor dependencies:

- **`BlockchainSdkConfig`** — API keys and external service configuration (NowNodes, Blockchair, Moralis, etc.)
- **`BlockchainFeatureToggles`** — feature flags (`isYieldSupplyEnabled`, `isPendingTransactionsEnabled`,
  `isSolanaTxHistoryEnabled`)
- **`BlockchainDataStorage`** — persistent key-value storage interface (JSON), implemented by the host app
- **`BlockchainSDKLogger`** — external logger with `NETWORK` and `TRANSACTION` levels
- **`AccountCreator`** — account creation (used by Hedera)

`BlockchainSdkConfig` and `BlockchainFeatureToggles` are stored in `DepsContainer` (singleton) and accessible globally
within the SDK.

The factory selects a blockchain-specific assembly (e.g., `BitcoinWalletManagerAssembly`) that wires up all dependencies
and returns a configured `WalletManager`.

```
WalletManagerFactory.createWalletManager(blockchain)
  -> getAssembly(blockchain)
    -> *WalletManagerAssembly.make(input)
      |-- Wallet with derived addresses
      |-- MultiNetworkProvider (failover across RPC endpoints)
      |-- TransactionBuilder
      |-- Feature providers (history, NFT, token balance, yield)
      +-- Configured *WalletManager
```

### Key Abstractions (in `common/`)

- **`WalletManager`** — abstract base class, central hub. Implements `TransactionSender` and delegates to feature
  providers.
- **`TransactionSender`** — `send()`, `getFee()`, `broadcastTransaction()`
- **`TransactionSigner`** — card-level signing via Tangem SDK
- **`TransactionPreparer`** / **`TransactionValidator`** — pre-broadcast preparation and validation
- **Feature providers** (optional per chain): `TransactionHistoryProvider`, `TokenBalanceProvider`, `NFTProvider`,
  `YieldSupplyProvider`, `NameResolver`, `Approver`

### Data Models

- **`Blockchain`** — enum of 50+ supported networks with chain metadata
- **`Wallet`** — addresses, public key, tokens, amounts, recent transactions
- **`Amount`** / **`AmountType`** — currency values (coin, token, reserve, yield)
- **`TransactionData`** (sealed) — `Uncompiled` (user-facing) or `Compiled` (signed)
- **`Fee`** (sealed hierarchy) — blockchain-specific fee structures (EIP-1559, sat/byte, lamports, etc.)

### Blockchain Implementation Patterns

Each blockchain lives in `blockchains/<name>/` and typically contains:

- `*WalletManager` — extends `WalletManager`
- `*TransactionBuilder` — constructs chain-specific transactions
- `*NetworkProvider` / `*NetworkService` — RPC calls
- Assembly class in `common/assembly/impl/`

Three main patterns:

1. **UTXO (Bitcoin-like)**: `UtxoBlockchainManager`, PSBT support, multiple address types (P2PKH/P2WPKH/P2TR). Used by
   Bitcoin, Litecoin, Dogecoin, Dash, Ravencoin, etc.
2. **EVM (Ethereum-like)**: EIP-1559 + legacy gas, ERC-20/NFT, ENS resolution, `Approver`. Shared by 20+ chains (
   Polygon, Arbitrum, Optimism, BSC, etc.) via `EthereumLikeWalletManager`.
3. **Custom (Solana, TON, XRP, etc.)**: Each has unique transaction construction, signing, and network interaction.

### Feature Providers

Each feature provider is created via a factory that returns either a blockchain-specific or default (no-op)
implementation. Injected into `WalletManager` through the assembly.

- **NFT** (`nft/`) — Moralis API for EVM chains and Solana. `NFTProviderFactory` checks `blockchain.canHandleNFTs()`.
  Other chains receive `DefaultNFTProvider`.
- **Transaction History** (`transactionhistory/`) — `TransactionHistoryProviderFactory` routes by blockchain:
  BlockBook (Bitcoin-like, Ethereum, Tron), Etherscan v2 (Polygon, Optimism, Base, BSC, etc.), Solana RPC, Algorand
  Indexer, Kaspa API. Pagination via `PaginationWrapper`.
- **Yield Supply** (`yieldsupply/`) — Aave V3 via direct smart contract calls (JSON-RPC). EVM chains only. Gated by
  feature toggle (`isYieldSupplyEnabled`). Contract addresses via `AaveV3YieldSupplyContractAddressFactory`.

### Network Layer

`MultiNetworkProvider<T>` provides automatic failover across multiple RPC providers for each blockchain. Supported
backends include JSON-RPC, BlockBook, BlockCypher, BlockChair, Electrum, and chain-specific APIs.

## Project Structure

- **`blockchain/`** — main Android library module (published via GitHub Packages)
- **`blockchain-demo/`** — demo Android app. Scans Tangem NFC cards, selects a blockchain, displays balance, and
  calculates fees. Serves as a reference implementation for SDK integration.
- **`tangem-android-tools/`** — git submodule with shared config (detekt-config.yml)
- **`dependencies.gradle`** — centralized version management

## Code Style

- Kotlin, JVM target 17, minSdk 24, compileSdk 36
- Groovy Gradle (not Kotlin DSL)
- Serialization: kotlinx-serialization (JSON + Protobuf), Moshi
- Networking: Retrofit + OkHttp, gRPC for some chains

## Branching

See @.claude/rules/git-rules.md