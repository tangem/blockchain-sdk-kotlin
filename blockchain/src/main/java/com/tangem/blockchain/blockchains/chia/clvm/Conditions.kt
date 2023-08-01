package com.tangem.blockchain.blockchains.chia.clvm

interface Condition {
    val conditionCode: Long

    fun toProgram(): Program
}

class CreateCoinCondition(
    private val destinationPuzzleHash: ByteArray,
    private val amount: Long,
    private val memos: List<ByteArray> = emptyList() // doesn't seem to be used by exchanges or wallets right now
) : Condition {
    override val conditionCode = 51L

    override fun toProgram(): Program {
        val programList = mutableListOf<Program>(
            Program.fromLong(conditionCode),
            Program.fromBytes(destinationPuzzleHash),
            Program.fromLong(amount)
        )

        if (memos.isNotEmpty()) {
            programList.add(
                Program.fromList(
                    memos.map { Program.fromBytes(it) }
                )
            )
        }

        return Program.fromList(programList)
    }
}

// always valid condition
class RemarkCondition() : Condition {
    override val conditionCode = 1L

    override fun toProgram(): Program {
        return Program.fromList(listOf(Program.fromLong(conditionCode)))
    }
}