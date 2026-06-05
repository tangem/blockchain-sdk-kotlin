package com.tangem.blockchain.transactionhistory.blockchains.solana

import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.transactionhistory.blockchains.solana.network.*
import com.tangem.blockchain.transactionhistory.models.TransactionHistoryItem
import com.tangem.blockchain.transactionhistory.models.TransactionHistoryItem.*
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

@Suppress("NamedArguments", "ArgumentListWrapping", "TrailingCommaOnCallSite", "FunctionSignature")
class SolanaTransactionHistoryMapperTest {

    private val mapper = SolanaTransactionHistoryMapper(Blockchain.Solana)

    private val walletAddress = "5fcy9woa8Di1QHcce65CsV3XKrxdB2pD4HJx5xx82ipM"
    private val leiaFilterToken = Token(
        name = "Leia the Cat",
        symbol = "LEIA",
        contractAddress = "7usVzynPTUJ9czdS96ezm9C6Z3hCsjb7j6TMKipURyyQ",
        decimals = 6,
    )

    // region Type Filtering

    @Test
    fun `SOL transfer shown for coin, filtered for token`() {
        val tx = buildSimpleSolTransfer(lamports = 100_000_000L)
        val sig = buildSignatureInfo()

        assertNotNull(mapper.mapToHistoryItem(sig, tx, walletAddress, filterToken = null))
        assertNull(mapper.mapToHistoryItem(sig, tx, walletAddress, filterToken = leiaFilterToken))
    }

    @Test
    fun `token transfer shown for token and as operation for coin`() {
        val tx = buildTokenTransfer(mint = leiaFilterToken.contractAddress)
        val sig = buildSignatureInfo()

        val tokenResult = mapper.mapToHistoryItem(sig, tx, walletAddress, filterToken = leiaFilterToken)
        assertNotNull(tokenResult)
        assertEquals(TransactionType.Transfer, tokenResult!!.type)

        val coinResult = mapper.mapToHistoryItem(sig, tx, walletAddress, filterToken = null)
        assertNotNull(coinResult)
        assertEquals(TransactionType.UnknownOperation, coinResult!!.type)
    }

    @Test
    fun `other operation shown for coin, filtered for token`() {
        val tx = buildOtherOperation(solDelta = -5000L)
        val sig = buildSignatureInfo()

        val coinResult = mapper.mapToHistoryItem(sig, tx, walletAddress, filterToken = null)
        assertNotNull(coinResult)
        assertEquals(TransactionType.UnknownOperation, coinResult!!.type)

        assertNull(mapper.mapToHistoryItem(sig, tx, walletAddress, filterToken = leiaFilterToken))
    }

    @Test
    fun `different token transfer filtered when filtering by LEIA`() {
        val otherMint = "DifferentTokenMintAddress1111111111111111111"
        val tx = buildTokenTransfer(mint = otherMint, balanceMint = otherMint)
        val sig = buildSignatureInfo()

        assertNull(mapper.mapToHistoryItem(sig, tx, walletAddress, filterToken = leiaFilterToken))
    }

    // endregion

    // region Amount Calculation

    @Test
    fun `SOL transfer amount and direction calculated from balance delta`() {
        val tx = buildSimpleSolTransfer(lamports = 500_000_000L)
        val sig = buildSignatureInfo()

        val result = mapper.mapToHistoryItem(sig, tx, walletAddress, filterToken = null)!!

        assertTrue(result.isOutgoing)
        assertEquals(BigDecimal("0.5"), result.amount.value!!.stripTrailingZeros())
    }

    @Test
    fun `SOL transfer fee is mapped from meta fee`() {
        val tx = buildSimpleSolTransfer(lamports = 500_000_000L)

        val result = mapper.mapToHistoryItem(buildSignatureInfo(), tx, walletAddress, filterToken = null)!!

        assertEquals(BigDecimal("0.000005"), result.fee.value!!.stripTrailingZeros())
    }

    @Test
    fun `zero SOL delta still shown in coin history with zero amount`() {
        val tx = buildSimpleSolTransfer(lamports = 0L)
        val result = mapper.mapToHistoryItem(buildSignatureInfo(), tx, walletAddress, filterToken = null)
        assertNotNull(result)
        assertEquals(BigDecimal.ZERO, result!!.amount.value!!.stripTrailingZeros())
    }

    @Test
    fun `token amount calculated from pre and post token balances`() {
        val tx = buildTokenTransfer(
            mint = leiaFilterToken.contractAddress,
            preBalance = 2_000_000L,
            postBalance = 1_000_000L,
        )
        val result = mapper.mapToHistoryItem(buildSignatureInfo(), tx, walletAddress, filterToken = leiaFilterToken)!!

        assertTrue(result.isOutgoing)
        assertEquals(BigDecimal("1"), result.amount.value!!.stripTrailingZeros())
    }

    @Test
    fun `zero token delta filtered out`() {
        val tx = buildTokenTransfer(
            mint = leiaFilterToken.contractAddress,
            preBalance = 1_000_000L,
            postBalance = 1_000_000L,
        )
        assertNull(mapper.mapToHistoryItem(buildSignatureInfo(), tx, walletAddress, filterToken = leiaFilterToken))
    }

    // endregion

    // region Token-2022

    @Test
    fun `token2022 uses uiAmountString instead of raw amount`() {
        val token2022 = Token(
            name = "Token2022",
            symbol = "T22",
            contractAddress = "Token2022MintAddress11111111111111111111111",
            decimals = 9,
        )
        val tx = buildToken2022Transfer(
            mint = token2022.contractAddress,
            preUiAmount = "10.5",
            postUiAmount = "8.0",
        )
        val result = mapper.mapToHistoryItem(buildSignatureInfo(), tx, walletAddress, filterToken = token2022)!!

        assertTrue(result.isOutgoing)
        assertEquals(BigDecimal("2.5"), result.amount.value!!.stripTrailingZeros())
    }

    // endregion

    // region Token Balance Ownership

    @Test
    fun `balance matched by accountIndex when owner is null`() {
        val tx = buildTokenTransfer(
            mint = leiaFilterToken.contractAddress,
            preBalance = 200L,
            postBalance = 100L,
            ownerInBalance = null,
            accountIndexMatchesWallet = true,
        )
        assertNotNull(mapper.mapToHistoryItem(buildSignatureInfo(), tx, walletAddress, filterToken = leiaFilterToken))
    }

    // endregion

    // region Authority

    @Test
    fun `token transfer uses authority as source address`() {
        val tx = buildTokenTransfer(
            mint = leiaFilterToken.contractAddress,
            preBalance = 200L,
            postBalance = 100L,
            source = "TokenAccountSource1111111111111111111111111",
            authority = walletAddress,
        )
        val result = mapper.mapToHistoryItem(buildSignatureInfo(), tx, walletAddress, filterToken = leiaFilterToken)!!

        assertEquals(walletAddress, (result.sourceType as SourceType.Single).address)
    }

    @Test
    fun `other operation uses counterparty instead of wallet to wallet`() {
        val tx = buildOtherOperation(solDelta = -5000L)

        val result = mapper.mapToHistoryItem(buildSignatureInfo(), tx, walletAddress, filterToken = null)!!

        assertEquals(walletAddress, (result.sourceType as SourceType.Single).address)
        assertEquals(
            "OtherAccount111111111111111111111111111111",
            ((result.destinationType as DestinationType.Single).addressType as TransactionHistoryItem.AddressType.User).address,
        )
    }

    @Test
    fun `stake withdraw uses stake account as source and wallet as destination`() {
        val stakeAccount = "StakeAccount11111111111111111111111111111111"
        val tx = buildStakeWithdraw(stakeAccount = stakeAccount)

        val result = mapper.mapToHistoryItem(buildSignatureInfo(), tx, walletAddress, filterToken = null)!!

        assertEquals(TransactionType.SolanaStakingTransactionType.Withdraw, result.type)
        assertFalse(result.isOutgoing)
        assertEquals(stakeAccount, (result.sourceType as SourceType.Single).address)
        assertEquals(
            walletAddress,
            ((result.destinationType as DestinationType.Single).addressType as TransactionHistoryItem.AddressType.User).address,
        )
    }

    @Test
    fun `stake operation uses new account as destination when provided`() {
        val newAccount = "NewStakeAccount1111111111111111111111111111"
        val tx = buildStakeCreateAccount(newAccount = newAccount)

        val result = mapper.mapToHistoryItem(buildSignatureInfo(), tx, walletAddress, filterToken = null)!!

        assertEquals(
            newAccount,
            ((result.destinationType as DestinationType.Single).addressType as TransactionHistoryItem.AddressType.User).address,
        )
    }

    @Test
    fun `stake delegate alongside initialize picks delegate and exposes validator`() {
        val voteAccount = "BbM5kJgvKKaQGAdsHkXmLUTkazUmt2x9"
        val tx = buildStakeInitializeAndDelegate(voteAccount = voteAccount)

        val result = mapper.mapToHistoryItem(buildSignatureInfo(), tx, walletAddress, filterToken = null)!!

        val stakeType = result.type as TransactionType.SolanaStakingTransactionType.Stake
        assertEquals(voteAccount, stakeType.validatorAddress)
        assertEquals(
            voteAccount,
            ((result.destinationType as DestinationType.Single).addressType as AddressType.Validator).address,
        )
    }

    @Test
    fun `transaction with raw string account keys is decoded and mapped`() {
        val moshi = Moshi.Builder()
            .add(SolanaAccountKeyAdapter())
            .add(SolanaInstructionAdapter())
            .build()
        val adapter = moshi.adapter<SolanaRpcResponse<SolanaTransactionResponse>>(
            Types.newParameterizedType(
                SolanaRpcResponse::class.java,
                SolanaTransactionResponse::class.java,
            ),
        )

        val response = adapter.fromJson(
            """
            {
              "result": {
                "blockTime": 1700000000,
                "meta": {
                  "err": null,
                  "fee": 5000,
                  "preBalances": [1500000000, 1000000000],
                  "postBalances": [1000000000, 1000000000],
                  "preTokenBalances": [],
                  "postTokenBalances": [],
                  "innerInstructions": [],
                  "rewards": []
                },
                "transaction": {
                  "message": {
                    "accountKeys": [
                      "$walletAddress",
                      "RecipientAddress111111111111111111111111111"
                    ],
                    "instructions": [
                      {
                        "programId": "11111111111111111111111111111111",
                        "program": "system",
                        "parsed": {
                          "type": "transfer",
                          "info": {
                            "source": "$walletAddress",
                            "destination": "RecipientAddress111111111111111111111111111",
                            "lamports": 500000000
                          }
                        }
                      }
                    ]
                  },
                  "signatures": ["TestSignature1111111111111111111111111111111"]
                }
              }
            }
            """.trimIndent(),
        )!!.result!!

        val result = mapper.mapToHistoryItem(buildSignatureInfo(), response, walletAddress, filterToken = null)

        assertNotNull(result)
        assertEquals(BigDecimal("0.5"), result!!.amount.value!!.stripTrailingZeros())
    }

    @Test(expected = JsonDataException::class)
    fun `invalid account key object fails fast during decode`() {
        val moshi = Moshi.Builder()
            .add(SolanaAccountKeyAdapter())
            .build()
        val adapter = moshi.adapter<SolanaAccountKey>(SolanaAccountKey::class.java)

        val result = adapter.fromJson("""{"signer":true}""")
        assertNotNull(result)
    }

    @Test
    fun `SOL transfer falls back to wallet and counterparty when parsed addresses are missing`() {
        val counterparty = "RecipientAddress111111111111111111111111111"
        val tx = SolanaTransactionResponse(
            blockTime = 1700000000L,
            meta = SolanaTransactionMeta(
                err = null,
                fee = 5000L,
                preBalances = listOf(1_500_000_000L, 1_000_000_000L),
                postBalances = listOf(1_000_000_000L, 1_000_000_000L),
                preTokenBalances = null,
                postTokenBalances = null,
                innerInstructions = null,
                rewards = null,
            ),
            transaction = SolanaTransactionData(
                message = SolanaTransactionMessage(
                    accountKeys = listOf(
                        SolanaAccountKey(pubkey = walletAddress, isSigner = true, isWritable = true, source = null),
                        SolanaAccountKey(pubkey = counterparty, isSigner = false, isWritable = true, source = null),
                    ),
                    instructions = listOf(
                        SolanaInstruction(
                            programId = "11111111111111111111111111111111",
                            program = "system",
                            parsed = SolanaParsedInstruction(
                                type = "transfer",
                                info = SolanaInstructionInfo(
                                    source = null,
                                    destination = null,
                                    lamports = 500_000_000L,
                                    amount = null,
                                    authority = null,
                                    tokenAmount = null,
                                    mint = null,
                                    stakeAccount = null,
                                    voteAccount = null,
                                    stakeAuthority = null,
                                    withdrawAuthority = null,
                                    newAccount = null,
                                ),
                            ),
                        ),
                    ),
                ),
                signatures = listOf("TestSignature1111111111111111111111111111111"),
            ),
        )

        val result = mapper.mapToHistoryItem(buildSignatureInfo(), tx, walletAddress, filterToken = null)!!

        assertEquals(walletAddress, (result.sourceType as SourceType.Single).address)
        assertEquals(
            counterparty,
            ((result.destinationType as DestinationType.Single).addressType as TransactionHistoryItem.AddressType.User).address,
        )
    }

    // endregion

    // region Helpers

    private fun buildSignatureInfo(err: Any? = null) = SolanaSignatureInfo(
        signature = "TestSignature1111111111111111111111111111111",
        blockTime = 1700000000L,
        confirmationStatus = "finalized",
        err = err,
    )

    private fun buildSimpleSolTransfer(lamports: Long): SolanaTransactionResponse {
        val destination = "RecipientAddress111111111111111111111111111"
        return SolanaTransactionResponse(
            blockTime = 1700000000L,
            meta = SolanaTransactionMeta(
                err = null,
                fee = 5000L,
                preBalances = listOf(1_000_000_000L + lamports, 1_000_000_000L),
                postBalances = listOf(1_000_000_000L, 1_000_000_000L),
                preTokenBalances = null,
                postTokenBalances = null,
                innerInstructions = null,
                rewards = null,
            ),
            transaction = SolanaTransactionData(
                message = SolanaTransactionMessage(
                    accountKeys = listOf(
                        SolanaAccountKey(pubkey = walletAddress, isSigner = true, isWritable = true, source = null),
                        SolanaAccountKey(pubkey = destination, isSigner = false, isWritable = true, source = null),
                    ),
                    instructions = listOf(
                        SolanaInstruction(
                            programId = "11111111111111111111111111111111",
                            program = "system",
                            parsed = SolanaParsedInstruction(
                                type = "transfer",
                                info = SolanaInstructionInfo(
                                    source = walletAddress,
                                    destination = destination,
                                    lamports = lamports,
                                    amount = null,
                                    authority = null,
                                    tokenAmount = null,
                                    mint = null,
                                    stakeAccount = null,
                                    voteAccount = null,
                                    stakeAuthority = null,
                                    withdrawAuthority = null,
                                    newAccount = null,
                                ),
                            ),
                        ),
                    ),
                ),
                signatures = listOf("TestSignature1111111111111111111111111111111"),
            ),
        )
    }

    @Suppress("LongParameterList")
    private fun buildTokenTransfer(
        mint: String,
        preBalance: Long = 2_000_000L,
        postBalance: Long = 1_000_000L,
        balanceMint: String = mint,
        ownerInBalance: String? = walletAddress,
        accountIndexMatchesWallet: Boolean = false,
        source: String = "TokenAccountSource1111111111111111111111111",
        destination: String = "TokenAccountDest11111111111111111111111111",
        authority: String? = walletAddress,
    ): SolanaTransactionResponse {
        val balanceAccountIndex = if (accountIndexMatchesWallet) 0 else 2

        return SolanaTransactionResponse(
            blockTime = 1700000000L,
            meta = SolanaTransactionMeta(
                err = null,
                fee = 5000L,
                preBalances = listOf(1_000_000_000L, 0L, 0L),
                postBalances = listOf(999_995_000L, 0L, 0L),
                preTokenBalances = listOf(
                    SolanaTokenBalance(
                        accountIndex = balanceAccountIndex,
                        mint = balanceMint,
                        owner = ownerInBalance,
                        programId = null,
                        uiTokenAmount = SolanaTokenAmount(
                            amount = preBalance.toString(),
                            decimals = 6,
                            uiAmount = null,
                            uiAmountString = null,
                        ),
                    ),
                ),
                postTokenBalances = listOf(
                    SolanaTokenBalance(
                        accountIndex = balanceAccountIndex,
                        mint = balanceMint,
                        owner = ownerInBalance,
                        programId = null,
                        uiTokenAmount = SolanaTokenAmount(
                            amount = postBalance.toString(),
                            decimals = 6,
                            uiAmount = null,
                            uiAmountString = null,
                        ),
                    ),
                ),
                innerInstructions = listOf(
                    SolanaInnerInstruction(
                        index = 0,
                        instructions = listOf(
                            SolanaInstruction(
                                programId = null,
                                program = "spl-token",
                                parsed = SolanaParsedInstruction(
                                    type = "transfer",
                                    info = SolanaInstructionInfo(
                                        source = source,
                                        destination = destination,
                                        lamports = null,
                                        amount = "1000000",
                                        authority = authority,
                                        tokenAmount = null,
                                        mint = null,
                                        stakeAccount = null,
                                        voteAccount = null,
                                        stakeAuthority = null,
                                        withdrawAuthority = null,
                                        newAccount = null,
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
                rewards = null,
            ),
            transaction = SolanaTransactionData(
                message = SolanaTransactionMessage(
                    accountKeys = listOf(
                        SolanaAccountKey(pubkey = walletAddress, isSigner = true, isWritable = true, source = null),
                        SolanaAccountKey(
                            pubkey = "SplTokenProgram111111111111111111111111111",
                            isSigner = false,
                            isWritable = false,
                            source = null,
                        ),
                        SolanaAccountKey(pubkey = source, isSigner = false, isWritable = true, source = null),
                    ),
                    instructions = listOf(
                        SolanaInstruction(programId = null, program = "spl-associated-token-account", parsed = null),
                    ),
                ),
                signatures = listOf("TestSignature1111111111111111111111111111111"),
            ),
        )
    }

    private fun buildToken2022Transfer(
        mint: String,
        preUiAmount: String,
        postUiAmount: String,
    ): SolanaTransactionResponse {
        return SolanaTransactionResponse(
            blockTime = 1700000000L,
            meta = SolanaTransactionMeta(
                err = null,
                fee = 5000L,
                preBalances = listOf(1_000_000_000L, 0L),
                postBalances = listOf(999_995_000L, 0L),
                preTokenBalances = listOf(
                    SolanaTokenBalance(
                        accountIndex = 1,
                        mint = mint,
                        owner = walletAddress,
                        programId = "TokenzQdBNbLqP5VEhdkAS6EPFLC1PHnBqCXEpPxuEb",
                        uiTokenAmount = SolanaTokenAmount(
                            amount = "0",
                            decimals = 9,
                            uiAmount = preUiAmount.toDouble(),
                            uiAmountString = preUiAmount,
                        ),
                    ),
                ),
                postTokenBalances = listOf(
                    SolanaTokenBalance(
                        accountIndex = 1,
                        mint = mint,
                        owner = walletAddress,
                        programId = "TokenzQdBNbLqP5VEhdkAS6EPFLC1PHnBqCXEpPxuEb",
                        uiTokenAmount = SolanaTokenAmount(
                            amount = "0",
                            decimals = 9,
                            uiAmount = postUiAmount.toDouble(),
                            uiAmountString = postUiAmount,
                        ),
                    ),
                ),
                innerInstructions = listOf(
                    SolanaInnerInstruction(
                        index = 0,
                        instructions = listOf(
                            SolanaInstruction(
                                programId = null,
                                program = "spl-token",
                                parsed = SolanaParsedInstruction(
                                    type = "transferChecked",
                                    info = SolanaInstructionInfo(
                                        source = "TokenAccount111111111111111111111111111111",
                                        destination = "TokenAccount222222222222222222222222222222",
                                        lamports = null, amount = null, authority = walletAddress,
                                        tokenAmount = SolanaTokenAmount(
                                            amount = "2500000000",
                                            decimals = 9,
                                            uiAmount = 2.5,
                                            uiAmountString = "2.5",
                                        ),
                                        mint = mint, stakeAccount = null, voteAccount = null,
                                        stakeAuthority = null, withdrawAuthority = null, newAccount = null,
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
                rewards = null,
            ),
            transaction = SolanaTransactionData(
                message = SolanaTransactionMessage(
                    accountKeys = listOf(
                        SolanaAccountKey(pubkey = walletAddress, isSigner = true, isWritable = true, source = null),
                        SolanaAccountKey(
                            pubkey = "TokenAccount111111111111111111111111111111",
                            isSigner = false,
                            isWritable = true,
                            source = null,
                        ),
                    ),
                    instructions = listOf(
                        SolanaInstruction(programId = null, program = "spl-associated-token-account", parsed = null),
                    ),
                ),
                signatures = listOf("TestSignature1111111111111111111111111111111"),
            ),
        )
    }

    private fun buildOtherOperation(solDelta: Long): SolanaTransactionResponse {
        val walletPre = 2_000_000_000L
        return SolanaTransactionResponse(
            blockTime = 1700000000L,
            meta = SolanaTransactionMeta(
                err = null,
                fee = 5000L,
                preBalances = listOf(walletPre, 0L),
                postBalances = listOf(walletPre + solDelta, 0L),
                preTokenBalances = listOf(
                    SolanaTokenBalance(
                        accountIndex = 1,
                        mint = "SomeMint1111111111111111111111111111111111",
                        owner = "SomeOwner111111111111111111111111111111111",
                        programId = null,
                        uiTokenAmount = SolanaTokenAmount(
                            amount = "100",
                            decimals = 6,
                            uiAmount = null,
                            uiAmountString = null,
                        ),
                    ),
                ),
                postTokenBalances = listOf(
                    SolanaTokenBalance(
                        accountIndex = 1,
                        mint = "SomeMint1111111111111111111111111111111111",
                        owner = "SomeOwner111111111111111111111111111111111",
                        programId = null,
                        uiTokenAmount = SolanaTokenAmount(
                            amount = "100",
                            decimals = 6,
                            uiAmount = null,
                            uiAmountString = null,
                        ),
                    ),
                ),
                innerInstructions = listOf(
                    SolanaInnerInstruction(
                        index = 0,
                        instructions = listOf(
                            SolanaInstruction(
                                programId = "UnknownProgram111111111111111111111111111",
                                program = null,
                                parsed = null,
                            ),
                        ),
                    ),
                ),
                rewards = null,
            ),
            transaction = SolanaTransactionData(
                message = SolanaTransactionMessage(
                    accountKeys = listOf(
                        SolanaAccountKey(pubkey = walletAddress, isSigner = true, isWritable = true, source = null),
                        SolanaAccountKey(
                            pubkey = "OtherAccount111111111111111111111111111111",
                            isSigner = false,
                            isWritable = true,
                            source = null,
                        ),
                    ),
                    instructions = listOf(
                        SolanaInstruction(
                            programId = "UnknownProgram111111111111111111111111111",
                            program = null,
                            parsed = null,
                        ),
                    ),
                ),
                signatures = listOf("TestSignature1111111111111111111111111111111"),
            ),
        )
    }

    private fun buildStakeWithdraw(stakeAccount: String): SolanaTransactionResponse {
        return SolanaTransactionResponse(
            blockTime = 1700000000L,
            meta = SolanaTransactionMeta(
                err = null,
                fee = 5000L,
                preBalances = listOf(30_000_000L, 0L),
                postBalances = listOf(50_000_000L, 0L),
                preTokenBalances = null,
                postTokenBalances = null,
                innerInstructions = null,
                rewards = null,
            ),
            transaction = SolanaTransactionData(
                message = SolanaTransactionMessage(
                    accountKeys = listOf(
                        SolanaAccountKey(pubkey = walletAddress, isSigner = true, isWritable = true, source = null),
                        SolanaAccountKey(pubkey = stakeAccount, isSigner = false, isWritable = true, source = null),
                    ),
                    instructions = listOf(
                        SolanaInstruction(
                            programId = STAKE_PROGRAM_ID,
                            program = "stake",
                            parsed = SolanaParsedInstruction(
                                type = "withdraw",
                                info = SolanaInstructionInfo(
                                    source = stakeAccount,
                                    destination = walletAddress,
                                    lamports = 20_000_000L,
                                    amount = null,
                                    authority = null,
                                    tokenAmount = null,
                                    mint = null,
                                    stakeAccount = stakeAccount,
                                    voteAccount = null,
                                    stakeAuthority = null,
                                    withdrawAuthority = walletAddress,
                                    newAccount = null,
                                ),
                            ),
                        ),
                    ),
                ),
                signatures = listOf("TestSignature1111111111111111111111111111111"),
            ),
        )
    }

    private fun buildStakeCreateAccount(newAccount: String): SolanaTransactionResponse {
        return SolanaTransactionResponse(
            blockTime = 1700000000L,
            meta = SolanaTransactionMeta(
                err = null,
                fee = 25000L,
                preBalances = listOf(56_548_606L, 0L),
                postBalances = listOf(36_523_606L, 20_000_000L),
                preTokenBalances = null,
                postTokenBalances = null,
                innerInstructions = null,
                rewards = null,
            ),
            transaction = SolanaTransactionData(
                message = SolanaTransactionMessage(
                    accountKeys = listOf(
                        SolanaAccountKey(pubkey = walletAddress, isSigner = true, isWritable = true, source = null),
                        SolanaAccountKey(pubkey = newAccount, isSigner = false, isWritable = true, source = null),
                    ),
                    instructions = listOf(
                        SolanaInstruction(
                            programId = STAKE_PROGRAM_ID,
                            program = "stake",
                            parsed = SolanaParsedInstruction(
                                type = "initialize",
                                info = SolanaInstructionInfo(
                                    source = null,
                                    destination = null,
                                    lamports = null,
                                    amount = null,
                                    authority = null,
                                    tokenAmount = null,
                                    mint = null,
                                    stakeAccount = null,
                                    voteAccount = null,
                                    stakeAuthority = walletAddress,
                                    withdrawAuthority = walletAddress,
                                    newAccount = newAccount,
                                ),
                            ),
                        ),
                    ),
                ),
                signatures = listOf("TestSignature1111111111111111111111111111111"),
            ),
        )
    }

    private fun buildStakeInitializeAndDelegate(voteAccount: String): SolanaTransactionResponse {
        val stakeAccount = "StakeAccount22222222222222222222222222222222"
        return SolanaTransactionResponse(
            blockTime = 1700000000L,
            meta = SolanaTransactionMeta(
                err = null,
                fee = 25000L,
                preBalances = listOf(56_548_606L, 0L),
                postBalances = listOf(36_523_606L, 20_000_000L),
                preTokenBalances = null,
                postTokenBalances = null,
                innerInstructions = null,
                rewards = null,
            ),
            transaction = SolanaTransactionData(
                message = SolanaTransactionMessage(
                    accountKeys = listOf(
                        SolanaAccountKey(pubkey = walletAddress, isSigner = true, isWritable = true, source = null),
                        SolanaAccountKey(pubkey = stakeAccount, isSigner = false, isWritable = true, source = null),
                    ),
                    instructions = listOf(
                        SolanaInstruction(
                            programId = STAKE_PROGRAM_ID,
                            program = "stake",
                            parsed = SolanaParsedInstruction(
                                type = "initialize",
                                info = SolanaInstructionInfo(
                                    source = null, destination = null, lamports = null, amount = null,
                                    authority = null, tokenAmount = null, mint = null,
                                    stakeAccount = null, voteAccount = null,
                                    stakeAuthority = walletAddress, withdrawAuthority = walletAddress,
                                    newAccount = stakeAccount,
                                ),
                            ),
                        ),
                        SolanaInstruction(
                            programId = STAKE_PROGRAM_ID,
                            program = "stake",
                            parsed = SolanaParsedInstruction(
                                type = "delegate",
                                info = SolanaInstructionInfo(
                                    source = null, destination = null, lamports = null, amount = null,
                                    authority = null, tokenAmount = null, mint = null,
                                    stakeAccount = stakeAccount, voteAccount = voteAccount,
                                    stakeAuthority = walletAddress, withdrawAuthority = null,
                                    newAccount = null,
                                ),
                            ),
                        ),
                    ),
                ),
                signatures = listOf("TestSignature1111111111111111111111111111111"),
            ),
        )
    }

    private companion object {
        const val STAKE_PROGRAM_ID = "Stake11111111111111111111111111111111111111"
    }

    // endregion
}