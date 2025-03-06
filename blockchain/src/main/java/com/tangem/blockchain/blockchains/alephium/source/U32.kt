package com.tangem.blockchain.blockchains.alephium.source

import java.math.BigInteger

@Suppress("MagicNumber")
@JvmInline
internal value class U32(val v: Int) : Comparable<U32> {

    val isZero: Boolean
        get() = v == 0

    fun addUnsafe(that: U32): U32 {
        val underlying = this.v + that.v
        require(U32.checkAdd(this, underlying))
        return U32.unsafe(underlying)
    }

    fun add(that: U32): U32? {
        val underlying = this.v + that.v
        return if (U32.checkAdd(this, underlying)) U32.unsafe(underlying) else null
    }

    fun subUnsafe(that: U32): U32 {
        require(U32.checkSub(this, that))
        return U32.unsafe(this.v - that.v)
    }

    fun sub(that: U32): U32? {
        return if (U32.checkSub(this, that)) {
            U32.unsafe(this.v - that.v)
        } else {
            null
        }
    }

    fun mulUnsafe(that: U32): U32 {
        if (this.v == 0) {
            return U32.Zero
        }
        val underlying = this.v * that.v
        require(U32.checkMul(this, that, underlying))
        return U32.unsafe(underlying)
    }

    fun mul(that: U32): U32? {
        if (this.v == 0) {
            return U32.Zero
        }
        val underlying = this.v * that.v
        return if (U32.checkMul(this, that, underlying)) {
            U32.unsafe(underlying)
        } else {
            null
        }
    }

    fun divUnsafe(that: U32): U32 {
        require(!that.isZero)
        return U32.unsafe(this.v.divideUnsigned(that.v))
    }

    fun div(that: U32): U32? {
        return if (that.isZero) null else U32.unsafe(this.v.divideUnsigned(that.v))
    }

    fun modUnsafe(that: U32): U32 {
        require(!that.isZero)
        return U32.unsafe(this.v.remainderUnsigned(that.v))
    }

    fun mod(that: U32): U32? {
        return if (that.isZero) null else U32.unsafe(this.v.remainderUnsigned(that.v))
    }

    override fun compareTo(other: U32): Int {
        return this.v.compareUnsigned(other.v)
    }

    fun toBigInt(): BigInteger {
        return BigInteger.valueOf(this.v.toUnsignedLong())
    }

    companion object {
        val Zero = unsafe(0)
        val One = unsafe(1)
        val Two = unsafe(2)
        val MaxValue = unsafe(-1)
        val MinValue = Zero

        fun validate(value: BigInteger): Boolean {
            return value >= BigInteger.ZERO && value.bitLength() <= 32
        }

        fun unsafe(value: Int): U32 {
            return U32(value)
        }

        fun from(value: Int): U32? {
            return if (value >= 0) unsafe(value) else null
        }

        fun from(value: BigInteger): U32? {
            return try {
                if (validate(value)) {
                    unsafe(value.toInt())
                } else {
                    null
                }
            } catch (e: ArithmeticException) {
                null
            }
        }

        private fun checkAdd(a: U32, c: Int): Boolean {
            return c.compareUnsigned(a.v) >= 0
        }

        private fun checkSub(a: U32, b: U32): Boolean {
            return a.v.compareUnsigned(b.v) >= 0
        }

        private fun checkMul(a: U32, b: U32, c: Int): Boolean {
            return c.divideUnsigned(a.v) == b.v
        }

        private fun Int.divideUnsigned(other: Int): Int {
            return java.lang.Integer.divideUnsigned(this, other)
        }

        private fun Int.remainderUnsigned(other: Int): Int {
            return java.lang.Integer.remainderUnsigned(this, other)
        }

        private fun Int.compareUnsigned(other: Int): Int {
            return java.lang.Integer.compareUnsigned(this, other)
        }

        private fun Int.toUnsignedLong(): Long {
            return java.lang.Integer.toUnsignedLong(this)
        }
    }
}