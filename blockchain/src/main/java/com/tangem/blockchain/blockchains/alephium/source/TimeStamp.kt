package com.tangem.blockchain.blockchains.alephium.source

import com.tangem.blockchain.blockchains.alephium.source.serde.Serde

@Suppress("MagicNumber")
@JvmInline
internal value class TimeStamp(val millis: Long) : Comparable<TimeStamp> {

    fun isZero(): Boolean = millis == 0L

    fun plusMillis(millisToAdd: Long): TimeStamp? = from(millis + millisToAdd)

    fun plusMillisUnsafe(millisToAdd: Long): TimeStamp = unsafe(millis + millisToAdd)

    fun plusSeconds(secondsToAdd: Long): TimeStamp? = from(millis + secondsToAdd * 1000)

    fun plusSecondsUnsafe(secondsToAdd: Long): TimeStamp = unsafe(millis + secondsToAdd * 1000)

    fun plusMinutes(minutesToAdd: Long): TimeStamp? = from(millis + minutesToAdd * 60 * 1000)

    fun plusMinutesUnsafe(minutesToAdd: Long): TimeStamp = unsafe(millis + minutesToAdd * 60 * 1000)

    fun plusHours(hoursToAdd: Long): TimeStamp? = from(millis + hoursToAdd * 60 * 60 * 1000)

    fun plusHoursUnsafe(hoursToAdd: Long): TimeStamp = unsafe(millis + hoursToAdd * 60 * 60 * 1000)

    fun plusUnsafe(duration: Duration): TimeStamp = unsafe(millis + duration.millis)

    operator fun plus(duration: Duration): TimeStamp = unsafe(millis + duration.millis)

    operator fun minus(duration: Duration): TimeStamp? = from(millis - duration.millis)

    fun minusUnsafe(duration: Duration): TimeStamp = unsafe(millis - duration.millis)

    operator fun minus(another: TimeStamp): Duration? = Duration.from(millis - another.millis)

    fun deltaUnsafe(another: TimeStamp): Duration = Duration.unsafe(millis - another.millis)

    fun isBefore(another: TimeStamp): Boolean = millis < another.millis

    override fun compareTo(other: TimeStamp): Int = millis.compareTo(other.millis)

    override fun toString(): String = "TimeStamp(${millis}ms)"

    companion object {
        val zero: TimeStamp = unsafe(0)

        val Max: TimeStamp = unsafe(Long.MAX_VALUE)

        val serde = Serde.Companion.TimeStampSerde
        const val byteLength: Int = 8

        fun unsafe(millis: Long): TimeStamp {
            require(millis >= 0) { "timestamp should be non-negative" }
            return TimeStamp(millis)
        }

        fun from(millis: Long): TimeStamp? {
            return if (millis >= 0) TimeStamp(millis) else null
        }

        fun now(): TimeStamp = unsafe(System.currentTimeMillis())
    }
}