package com.makstuff.minimalistcaloriecounter.persistence.room

import androidx.room.TypeConverter
import java.time.LocalDate
import java.time.LocalDateTime

class AppTypeConverters {
    @TypeConverter
    fun localDateToString(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun stringToLocalDate(value: String?): LocalDate? = value?.takeIf { it.isNotBlank() }?.let(LocalDate::parse)

    @TypeConverter
    fun localDateTimeToString(value: LocalDateTime?): String? = value?.toString()

    @TypeConverter
    fun stringToLocalDateTime(value: String?): LocalDateTime? = value?.takeIf { it.isNotBlank() }?.let(LocalDateTime::parse)
}
