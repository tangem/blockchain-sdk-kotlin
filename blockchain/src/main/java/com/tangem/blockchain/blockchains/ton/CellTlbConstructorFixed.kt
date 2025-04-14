package com.tangem.blockchain.blockchains.ton

import org.ton.bitstring.BitString
import org.ton.cell.Cell
import org.ton.cell.CellBuilder
import org.ton.cell.CellSlice
import org.ton.cell.invoke
import org.ton.tlb.TlbCodec
import org.ton.tlb.TlbConstructor

/**
 * This is the fixed implementation of [TlbCodec] for cell from [Cell.Companion.tlbCodec].
 * Codec from library, read cell as reference and this is incorrect behavior.
 */
internal object CellTlbConstructorFixed : TlbConstructor<Cell>(
    schema = "_ _:Cell = Cell;",
    id = BitString.empty(),
) {

    override fun storeTlb(cellBuilder: CellBuilder, value: Cell) = cellBuilder {
        storeSlice(value.beginParse())
    }

    override fun loadTlb(cellSlice: CellSlice): Cell = cellSlice {
        Cell(loadBits(remainingBits))
    }
}