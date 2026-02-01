package com.kobe.warehouse.sales.data.model

/**
 * PrioriteTiersPayant - Insurance Priority Levels
 * Corresponds to backend: com.kobe.warehouse.domain.enumeration.PrioriteTiersPayant
 */
enum class PrioriteTiersPayant(val value: Int, val code: String) {
    R0(0, "R0"),
    R1(1, "C1"),
    R2(2, "C2"),
    R3(3, "C3");

    companion object {
        fun fromValue(value: Int): PrioriteTiersPayant? {
            return entries.find { it.value == value }
        }

        fun fromCode(code: String): PrioriteTiersPayant? {
            return entries.find { it.code == code }
        }
    }

    fun isPrincipal(): Boolean = this == R0

    fun isComplementaire(): Boolean = this != R0

    fun getDisplayLabel(): String {
        return when (this) {
            R0 -> "R0"
            R1 -> "C1"
            R2 -> "C2"
            R3 -> "C3"
        }
    }
}
