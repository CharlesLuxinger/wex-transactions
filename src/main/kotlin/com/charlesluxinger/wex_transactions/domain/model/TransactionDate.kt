package com.charlesluxinger.wex_transactions.domain.model

import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class TransactionDate(
    value: LocalDateTime,
) {
    val value: LocalDateTime = value.withNano(0)

    constructor(rawValue: String) : this(parse(rawValue))

    fun toCanonicalString(): String = value.atOffset(DEFAULT_OFFSET).format(CANONICAL_FORMAT)

    override fun equals(other: Any?): Boolean = this === other || (other is TransactionDate && value == other.value)

    override fun hashCode(): Int = value.hashCode()

    override fun toString(): String = toCanonicalString()

    companion object {
        private val DEFAULT_OFFSET = java.time.ZoneOffset.UTC
        private val CANONICAL_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")

        private fun parse(raw: String): LocalDateTime {
            val input = raw.trim()
            val parsed =
                runCatching { OffsetDateTime.parse(input, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toLocalDateTime() }
                    .recoverCatching {
                        ZonedDateTime
                            .parse(
                                input,
                                DateTimeFormatter.ISO_ZONED_DATE_TIME,
                            ).toLocalDateTime()
                    }.recoverCatching { LocalDateTime.parse(input, DateTimeFormatter.ISO_LOCAL_DATE_TIME) }
                    .getOrElse {
                        throw IllegalArgumentException("Invalid ISO-8601 transaction date format")
                    }

            return parsed.withNano(0)
        }
    }
}
