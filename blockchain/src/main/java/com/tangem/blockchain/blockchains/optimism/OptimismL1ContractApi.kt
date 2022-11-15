package com.tangem.blockchain.blockchains.optimism

import org.kethereum.contract.abi.types.encodeTypes
import org.kethereum.contract.abi.types.model.type_params.BitsTypeParams
import org.kethereum.contract.abi.types.model.types.AddressETHType
import org.kethereum.contract.abi.types.model.types.DynamicSizedBytesETHType
import org.kethereum.contract.abi.types.model.types.UIntETHType
import org.kethereum.model.Address
import org.kethereum.model.Transaction
import org.kethereum.model.createEmptyTransaction
import java.math.BigInteger

public val FourByteDecimals: ByteArray = byteArrayOf(49, 60, -27, 103)

public val FourByteGasPrice: ByteArray = byteArrayOf(-2, 23, 59, -105)

public val FourByteGetL1Fee: ByteArray = byteArrayOf(73, -108, -114, 14)

public val FourByteGetL1GasUsed: ByteArray = byteArrayOf(-34, 38, -60, -95)

public val FourByteL1BaseFee: ByteArray = byteArrayOf(81, -101, 75, -45)

public val FourByteOverhead: ByteArray = byteArrayOf(12, 24, -63, 98)

public val FourByteOwner: ByteArray = byteArrayOf(-115, -91, -53, 91)

public val FourByteRenounceOwnership: ByteArray = byteArrayOf(113, 80, 24, -90)

public val FourByteScalar: ByteArray = byteArrayOf(-12, 94, 101, -40)

public val FourByteSetDecimals: ByteArray = byteArrayOf(-116, -120, -123, -56)

public val FourByteSetGasPrice: ByteArray = byteArrayOf(-65, 31, -28, 32)

public val FourByteSetL1BaseFee: ByteArray = byteArrayOf(-66, -34, 57, -75)

public val FourByteSetOverhead: ByteArray = byteArrayOf(53, 119, -81, -59)

public val FourByteSetScalar: ByteArray = byteArrayOf(112, 70, 85, -105)

public val FourByteTransferOwnership: ByteArray = byteArrayOf(-14, -3, -29, -117)

public class optimism_gas_l1TransactionGenerator(
    address: Address
) {
    private val tx: Transaction = createEmptyTransaction().apply { to = address }

    internal fun decimalsETHTyped() = tx.copy(input = FourByteDecimals + encodeTypes())

    /**
     * Signature: decimals()
     * 4Byte: 313ce567
     */
    public fun decimals(): Transaction = decimalsETHTyped()

    internal fun gasPriceETHTyped() = tx.copy(input = FourByteGasPrice + encodeTypes())

    /**
     * Signature: gasPrice()
     * 4Byte: fe173b97
     */
    public fun gasPrice(): Transaction = gasPriceETHTyped()

    internal fun getL1FeeETHTyped(_data: DynamicSizedBytesETHType) = tx.copy(input =
    FourByteGetL1Fee + encodeTypes(_data))

    /**
     * Signature: getL1Fee(bytes)
     * 4Byte: 49948e0e
     */
    public fun getL1Fee(_data: ByteArray): Transaction =
        getL1FeeETHTyped(DynamicSizedBytesETHType.ofNativeKotlinType(_data))

    internal fun getL1GasUsedETHTyped(_data: DynamicSizedBytesETHType) = tx.copy(input =
    FourByteGetL1GasUsed + encodeTypes(_data))

    /**
     * Signature: getL1GasUsed(bytes)
     * 4Byte: de26c4a1
     */
    public fun getL1GasUsed(_data: ByteArray): Transaction =
        getL1GasUsedETHTyped(DynamicSizedBytesETHType.ofNativeKotlinType(_data))

    internal fun l1BaseFeeETHTyped() = tx.copy(input = FourByteL1BaseFee + encodeTypes())

    /**
     * Signature: l1BaseFee()
     * 4Byte: 519b4bd3
     */
    public fun l1BaseFee(): Transaction = l1BaseFeeETHTyped()

    internal fun overheadETHTyped() = tx.copy(input = FourByteOverhead + encodeTypes())

    /**
     * Signature: overhead()
     * 4Byte: 0c18c162
     */
    public fun overhead(): Transaction = overheadETHTyped()

    internal fun ownerETHTyped() = tx.copy(input = FourByteOwner + encodeTypes())

    /**
     * Signature: owner()
     * 4Byte: 8da5cb5b
     */
    public fun owner(): Transaction = ownerETHTyped()

    internal fun renounceOwnershipETHTyped() = tx.copy(input = FourByteRenounceOwnership +
            encodeTypes())

    /**
     * Signature: renounceOwnership()
     * 4Byte: 715018a6
     */
    public fun renounceOwnership(): Transaction = renounceOwnershipETHTyped()

    internal fun scalarETHTyped() = tx.copy(input = FourByteScalar + encodeTypes())

    /**
     * Signature: scalar()
     * 4Byte: f45e65d8
     */
    public fun scalar(): Transaction = scalarETHTyped()

    internal fun setDecimalsETHTyped(_decimals: UIntETHType) = tx.copy(input = FourByteSetDecimals +
            encodeTypes(_decimals))

    /**
     * Signature: setDecimals(uint256)
     * 4Byte: 8c8885c8
     */
    public fun setDecimals(_decimals: BigInteger): Transaction =
        setDecimalsETHTyped(UIntETHType.ofNativeKotlinType(_decimals,BitsTypeParams(bits=256)))

    internal fun setGasPriceETHTyped(_gasPrice: UIntETHType) = tx.copy(input = FourByteSetGasPrice +
            encodeTypes(_gasPrice))

    /**
     * Signature: setGasPrice(uint256)
     * 4Byte: bf1fe420
     */
    public fun setGasPrice(_gasPrice: BigInteger): Transaction =
        setGasPriceETHTyped(UIntETHType.ofNativeKotlinType(_gasPrice,BitsTypeParams(bits=256)))

    internal fun setL1BaseFeeETHTyped(_baseFee: UIntETHType) = tx.copy(input = FourByteSetL1BaseFee +
            encodeTypes(_baseFee))

    /**
     * Signature: setL1BaseFee(uint256)
     * 4Byte: bede39b5
     */
    public fun setL1BaseFee(_baseFee: BigInteger): Transaction =
        setL1BaseFeeETHTyped(UIntETHType.ofNativeKotlinType(_baseFee,BitsTypeParams(bits=256)))

    internal fun setOverheadETHTyped(_overhead: UIntETHType) = tx.copy(input = FourByteSetOverhead +
            encodeTypes(_overhead))

    /**
     * Signature: setOverhead(uint256)
     * 4Byte: 3577afc5
     */
    public fun setOverhead(_overhead: BigInteger): Transaction =
        setOverheadETHTyped(UIntETHType.ofNativeKotlinType(_overhead,BitsTypeParams(bits=256)))

    internal fun setScalarETHTyped(_scalar: UIntETHType) = tx.copy(input = FourByteSetScalar +
            encodeTypes(_scalar))

    /**
     * Signature: setScalar(uint256)
     * 4Byte: 70465597
     */
    public fun setScalar(_scalar: BigInteger): Transaction =
        setScalarETHTyped(UIntETHType.ofNativeKotlinType(_scalar,BitsTypeParams(bits=256)))

    internal fun transferOwnershipETHTyped(newOwner: AddressETHType) = tx.copy(input =
    FourByteTransferOwnership + encodeTypes(newOwner))

    /**
     * Signature: transferOwnership(address)
     * 4Byte: f2fde38b
     */
    public fun transferOwnership(newOwner: Address): Transaction =
        transferOwnershipETHTyped(AddressETHType.ofNativeKotlinType(newOwner))
}

//public class optimism_gas_l1RPCConnector(
//    private val address: Address,
//    private val rpc: EthereumRPC
//) {
//    private val txGenerator: optimism_gas_l1TransactionGenerator =
//        optimism_gas_l1TransactionGenerator(address)
//
//    private fun decimalsETHTyped(blockSpec: String = "latest"): UIntETHType? {
//        val tx = txGenerator.decimalsETHTyped()
//        return UIntETHType.ofPaginatedByteArray(PaginatedByteArray(rpc.call(tx,
//            blockSpec)),BitsTypeParams(bits=256))
//    }
//
//    /**
//     * Signature: decimals()
//     * 4Byte: 313ce567
//     */
//    public fun decimals(blockSpec: String = "latest"): BigInteger? =
//        decimalsETHTyped(blockSpec)?.toKotlinType()
//
//    private fun gasPriceETHTyped(blockSpec: String = "latest"): UIntETHType? {
//        val tx = txGenerator.gasPriceETHTyped()
//        return UIntETHType.ofPaginatedByteArray(PaginatedByteArray(rpc.call(tx,
//            blockSpec)),BitsTypeParams(bits=256))
//    }
//
//    /**
//     * Signature: gasPrice()
//     * 4Byte: fe173b97
//     */
//    public fun gasPrice(blockSpec: String = "latest"): BigInteger? =
//        gasPriceETHTyped(blockSpec)?.toKotlinType()
//
//    private fun getL1FeeETHTyped(_data: DynamicSizedBytesETHType, blockSpec: String = "latest"):
//            UIntETHType? {
//        val tx = txGenerator.getL1FeeETHTyped(_data)
//        return UIntETHType.ofPaginatedByteArray(PaginatedByteArray(rpc.call(tx,
//            blockSpec)),BitsTypeParams(bits=256))
//    }
//
//    /**
//     * Signature: getL1Fee(bytes)
//     * 4Byte: 49948e0e
//     */
//    public fun getL1Fee(_data: ByteArray, blockSpec: String = "latest"): BigInteger? =
//        getL1FeeETHTyped(DynamicSizedBytesETHType.ofNativeKotlinType(_data),blockSpec)?.toKotlinType()
//
//    private fun getL1GasUsedETHTyped(_data: DynamicSizedBytesETHType, blockSpec: String = "latest"):
//            UIntETHType? {
//        val tx = txGenerator.getL1GasUsedETHTyped(_data)
//        return UIntETHType.ofPaginatedByteArray(PaginatedByteArray(rpc.call(tx,
//            blockSpec)),BitsTypeParams(bits=256))
//    }
//
//    /**
//     * Signature: getL1GasUsed(bytes)
//     * 4Byte: de26c4a1
//     */
//    public fun getL1GasUsed(_data: ByteArray, blockSpec: String = "latest"): BigInteger? =
//        getL1GasUsedETHTyped(DynamicSizedBytesETHType.ofNativeKotlinType(_data),blockSpec)?.toKotlinType()
//
//    private fun l1BaseFeeETHTyped(blockSpec: String = "latest"): UIntETHType? {
//        val tx = txGenerator.l1BaseFeeETHTyped()
//        return UIntETHType.ofPaginatedByteArray(PaginatedByteArray(rpc.call(tx,
//            blockSpec)),BitsTypeParams(bits=256))
//    }
//
//    /**
//     * Signature: l1BaseFee()
//     * 4Byte: 519b4bd3
//     */
//    public fun l1BaseFee(blockSpec: String = "latest"): BigInteger? =
//        l1BaseFeeETHTyped(blockSpec)?.toKotlinType()
//
//    private fun overheadETHTyped(blockSpec: String = "latest"): UIntETHType? {
//        val tx = txGenerator.overheadETHTyped()
//        return UIntETHType.ofPaginatedByteArray(PaginatedByteArray(rpc.call(tx,
//            blockSpec)),BitsTypeParams(bits=256))
//    }
//
//    /**
//     * Signature: overhead()
//     * 4Byte: 0c18c162
//     */
//    public fun overhead(blockSpec: String = "latest"): BigInteger? =
//        overheadETHTyped(blockSpec)?.toKotlinType()
//
//    private fun ownerETHTyped(blockSpec: String = "latest"): AddressETHType? {
//        val tx = txGenerator.ownerETHTyped()
//        return AddressETHType.ofPaginatedByteArray(PaginatedByteArray(rpc.call(tx, blockSpec)))
//    }
//
//    /**
//     * Signature: owner()
//     * 4Byte: 8da5cb5b
//     */
//    public fun owner(blockSpec: String = "latest"): Address? =
//        ownerETHTyped(blockSpec)?.toKotlinType()
//
//    private fun renounceOwnershipETHTyped(blockSpec: String = "latest"): Unit {
//        val tx = txGenerator.renounceOwnershipETHTyped()
//        rpc.call(tx, blockSpec)
//    }
//
//    /**
//     * Signature: renounceOwnership()
//     * 4Byte: 715018a6
//     */
//    public fun renounceOwnership(blockSpec: String = "latest"): Unit {
//        renounceOwnershipETHTyped(blockSpec)
//    }
//
//    private fun scalarETHTyped(blockSpec: String = "latest"): UIntETHType? {
//        val tx = txGenerator.scalarETHTyped()
//        return UIntETHType.ofPaginatedByteArray(PaginatedByteArray(rpc.call(tx,
//            blockSpec)),BitsTypeParams(bits=256))
//    }
//
//    /**
//     * Signature: scalar()
//     * 4Byte: f45e65d8
//     */
//    public fun scalar(blockSpec: String = "latest"): BigInteger? =
//        scalarETHTyped(blockSpec)?.toKotlinType()
//
//    private fun setDecimalsETHTyped(_decimals: UIntETHType, blockSpec: String = "latest"): Unit {
//        val tx = txGenerator.setDecimalsETHTyped(_decimals)
//        rpc.call(tx, blockSpec)
//    }
//
//    /**
//     * Signature: setDecimals(uint256)
//     * 4Byte: 8c8885c8
//     */
//    public fun setDecimals(_decimals: BigInteger, blockSpec: String = "latest"): Unit {
//        setDecimalsETHTyped(UIntETHType.ofNativeKotlinType(_decimals,BitsTypeParams(bits=256)),blockSpec)
//    }
//
//    private fun setGasPriceETHTyped(_gasPrice: UIntETHType, blockSpec: String = "latest"): Unit {
//        val tx = txGenerator.setGasPriceETHTyped(_gasPrice)
//        rpc.call(tx, blockSpec)
//    }
//
//    /**
//     * Signature: setGasPrice(uint256)
//     * 4Byte: bf1fe420
//     */
//    public fun setGasPrice(_gasPrice: BigInteger, blockSpec: String = "latest"): Unit {
//        setGasPriceETHTyped(UIntETHType.ofNativeKotlinType(_gasPrice,BitsTypeParams(bits=256)),blockSpec)
//    }
//
//    private fun setL1BaseFeeETHTyped(_baseFee: UIntETHType, blockSpec: String = "latest"): Unit {
//        val tx = txGenerator.setL1BaseFeeETHTyped(_baseFee)
//        rpc.call(tx, blockSpec)
//    }
//
//    /**
//     * Signature: setL1BaseFee(uint256)
//     * 4Byte: bede39b5
//     */
//    public fun setL1BaseFee(_baseFee: BigInteger, blockSpec: String = "latest"): Unit {
//        setL1BaseFeeETHTyped(UIntETHType.ofNativeKotlinType(_baseFee,BitsTypeParams(bits=256)),blockSpec)
//    }
//
//    private fun setOverheadETHTyped(_overhead: UIntETHType, blockSpec: String = "latest"): Unit {
//        val tx = txGenerator.setOverheadETHTyped(_overhead)
//        rpc.call(tx, blockSpec)
//    }
//
//    /**
//     * Signature: setOverhead(uint256)
//     * 4Byte: 3577afc5
//     */
//    public fun setOverhead(_overhead: BigInteger, blockSpec: String = "latest"): Unit {
//        setOverheadETHTyped(UIntETHType.ofNativeKotlinType(_overhead,BitsTypeParams(bits=256)),blockSpec)
//    }
//
//    private fun setScalarETHTyped(_scalar: UIntETHType, blockSpec: String = "latest"): Unit {
//        val tx = txGenerator.setScalarETHTyped(_scalar)
//        rpc.call(tx, blockSpec)
//    }
//
//    /**
//     * Signature: setScalar(uint256)
//     * 4Byte: 70465597
//     */
//    public fun setScalar(_scalar: BigInteger, blockSpec: String = "latest"): Unit {
//        setScalarETHTyped(UIntETHType.ofNativeKotlinType(_scalar,BitsTypeParams(bits=256)),blockSpec)
//    }
//
//    private fun transferOwnershipETHTyped(newOwner: AddressETHType, blockSpec: String = "latest"):
//            Unit {
//        val tx = txGenerator.transferOwnershipETHTyped(newOwner)
//        rpc.call(tx, blockSpec)
//    }
//
//    /**
//     * Signature: transferOwnership(address)
//     * 4Byte: f2fde38b
//     */
//    public fun transferOwnership(newOwner: Address, blockSpec: String = "latest"): Unit {
//        transferOwnershipETHTyped(AddressETHType.ofNativeKotlinType(newOwner),blockSpec)
//    }
//}

public class optimism_gas_l1TransactionDecoder() {
    public fun isDecimals(tx: Transaction): Boolean =
        tx.input.sliceArray(0..3).contentEquals(FourByteDecimals)

    public fun isGasPrice(tx: Transaction): Boolean =
        tx.input.sliceArray(0..3).contentEquals(FourByteGasPrice)

    public fun isGetL1Fee(tx: Transaction): Boolean =
        tx.input.sliceArray(0..3).contentEquals(FourByteGetL1Fee)

    public fun isGetL1GasUsed(tx: Transaction): Boolean =
        tx.input.sliceArray(0..3).contentEquals(FourByteGetL1GasUsed)

    public fun isL1BaseFee(tx: Transaction): Boolean =
        tx.input.sliceArray(0..3).contentEquals(FourByteL1BaseFee)

    public fun isOverhead(tx: Transaction): Boolean =
        tx.input.sliceArray(0..3).contentEquals(FourByteOverhead)

    public fun isOwner(tx: Transaction): Boolean =
        tx.input.sliceArray(0..3).contentEquals(FourByteOwner)

    public fun isRenounceOwnership(tx: Transaction): Boolean =
        tx.input.sliceArray(0..3).contentEquals(FourByteRenounceOwnership)

    public fun isScalar(tx: Transaction): Boolean =
        tx.input.sliceArray(0..3).contentEquals(FourByteScalar)

    public fun isSetDecimals(tx: Transaction): Boolean =
        tx.input.sliceArray(0..3).contentEquals(FourByteSetDecimals)

    public fun isSetGasPrice(tx: Transaction): Boolean =
        tx.input.sliceArray(0..3).contentEquals(FourByteSetGasPrice)

    public fun isSetL1BaseFee(tx: Transaction): Boolean =
        tx.input.sliceArray(0..3).contentEquals(FourByteSetL1BaseFee)

    public fun isSetOverhead(tx: Transaction): Boolean =
        tx.input.sliceArray(0..3).contentEquals(FourByteSetOverhead)

    public fun isSetScalar(tx: Transaction): Boolean =
        tx.input.sliceArray(0..3).contentEquals(FourByteSetScalar)

    public fun isTransferOwnership(tx: Transaction): Boolean =
        tx.input.sliceArray(0..3).contentEquals(FourByteTransferOwnership)
}