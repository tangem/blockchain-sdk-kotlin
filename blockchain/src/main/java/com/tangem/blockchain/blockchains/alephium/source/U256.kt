package com.tangem.blockchain.blockchains.alephium.source

import kotlinx.io.bytestring.ByteString
import java.math.BigDecimal
import java.math.BigInteger

@Suppress("MagicNumber")
internal data class U256(val v: BigInteger) : Comparable<U256> {
    init {
        require(validate(v))
    }

    val isZero: Boolean
        get() = v.signum() == 0

    fun addUnsafe(that: U256): U256 {
        val underlying = this.v.add(that.v)
        require(validate(underlying))
        return unsafe(underlying)
    }

    fun add(that: U256): U256? {
        val underlying = this.v.add(that.v)
        return if (validate(underlying)) unsafe(underlying) else null
    }

    fun subUnsafe(that: U256): U256 {
        val underlying = this.v.subtract(that.v)
        require(validate(underlying))
        return unsafe(underlying)
    }

    fun sub(that: U256): U256? {
        val underlying = this.v.subtract(that.v)
        return if (validate(underlying)) unsafe(underlying) else null
    }

    fun mulUnsafe(that: U256): U256 {
        val underlying = this.v.multiply(that.v)
        require(validate(underlying))
        return unsafe(underlying)
    }

    fun div(that: U256): U256? {
        return if (that.isZero) null else unsafe(this.v.divide(that.v))
    }

    override fun compareTo(other: U256): Int = this.v.compareTo(other.v)

    fun toByte(): Byte? = if (v.bitLength() <= 7) v.toInt().toByte() else null

    companion object {
        private val upperBound = BigInteger.ONE.shiftLeft(256)
        val Zero get() = unsafe(BigInteger.ZERO)

        fun boundNonNegative(value: BigInteger): U256 {
            require(value.signum() >= 0)
            val raw = value.toByteArray()
            val boundedRaw = if (raw.size > 32) raw.takeLast(32).toByteArray() else raw
            return unsafe(BigInteger(1, boundedRaw))
        }

        fun boundSub(value: BigInteger): U256 {
            return if (value.signum() < 0) unsafe(value.add(upperBound)) else unsafe(value)
        }

        fun validate(value: BigInteger): Boolean {
            return value.signum() >= 0 && value.bitLength() <= 256
        }

        fun unsafe(value: BigInteger): U256 {
            require(validate(value))
            return U256(value)
        }

        fun unsafe(value: Int): U256 = unsafe(value.toLong())

        fun unsafe(value: Long): U256 {
            require(value >= 0)
            return U256(BigInteger.valueOf(value))
        }

        fun unsafe(value: BigDecimal): U256 {
            val bigInteger = value.toBigInteger()
            require(validate(bigInteger))
            return U256(bigInteger)
        }

        fun from(bytes: ByteString): U256? = from(BigInteger(1, bytes.toByteArray()))

        fun from(value: BigInteger): U256? = if (validate(value)) U256(value) else null
    }
}