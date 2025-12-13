package com.kobe.warehouse.reports.data.local

import androidx.room.TypeConverter

/**
 * Type converters for Room database.
 */
class Converters {

    @TypeConverter
    fun fromTimestamp(value: Long?): Long? {
        return value
    }

    @TypeConverter
    fun dateToTimestamp(date: Long?): Long? {
        return date
    }
}
