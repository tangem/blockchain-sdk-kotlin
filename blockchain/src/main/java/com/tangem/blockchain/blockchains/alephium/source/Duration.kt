package com.tangem.blockchain.blockchains.alephium.source

@Suppress("MagicNumber")
internal data class Duration(val millis: Long) : Comparable<Duration> {

    fun toSeconds(): Long = millis / 1000

    fun toMinutes(): Long = toSeconds() / 60

    fun toHours(): Long = toSeconds() / (60 * 60)

    operator fun plus(another: Duration): Duration = unsafe(millis + another.millis)

    operator fun minus(another: Duration): Duration? = from(millis - another.millis)

    fun timesUnsafe(scale: Long): Duration = unsafe(millis * scale)
    operator fun times(scale: Long): Duration? = from(millis * scale)

    fun divUnsafe(scale: Long): Duration = unsafe(millis / scale)
    operator fun div(scale: Long): Duration? = from(millis / scale)

    override fun compareTo(other: Duration): Int = millis.compareTo(other.millis)

    override fun toString(): String = "Duration(${millis}ms)"

    companion object {
        val zero: Duration = unsafe(0)

        fun unsafe(millis: Long): Duration {
            require(millis >= 0) { "duration should be positive" }
            return Duration(millis)
        }

        fun from(millis: Long): Duration? {
            return if (millis >= 0) Duration(millis) else null
        }
    }
}