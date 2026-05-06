package com.donabere.amm.model.converters

import androidx.room.TypeConverter
import java.time.LocalDateTime

class LocalDateTimeToString {

    @TypeConverter
    fun fromLocalDateTime(value: LocalDateTime?): String? {
        return value?.toString()
    }

    @TypeConverter
    fun toLocalDateTime(value: String?): LocalDateTime? {
        return value?.let { LocalDateTime.parse(it) }
    }
}