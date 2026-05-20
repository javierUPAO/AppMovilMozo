package com.donabere.amm.model.converters

import java.time.LocalDateTime

class LocalDateTimeToString {

    fun fromLocalDateTime(value: LocalDateTime?): String? {
        return value?.toString()
    }

    fun toLocalDateTime(value: String?): LocalDateTime? {
        return value?.let { LocalDateTime.parse(it) }
    }
}