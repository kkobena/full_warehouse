package com.kobe.warehouse.sales.data.model

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

/**
 * Custom Gson deserializer for PrioriteTiersPayant enum
 * Handles deserialization from backend integer values (0, 1, 2, 3)
 */
class PrioriteTiersPayantDeserializer : JsonDeserializer<PrioriteTiersPayant> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): PrioriteTiersPayant? {
        if (json == null || json.isJsonNull) {
            return null
        }

        return try {
            when {
                json.isJsonPrimitive && json.asJsonPrimitive.isNumber -> {
                    // Backend sends integer value (0, 1, 2, 3)
                    PrioriteTiersPayant.fromValue(json.asInt)
                }
                json.isJsonPrimitive && json.asJsonPrimitive.isString -> {
                    // Backend sends string code ("R0", "C1", "C2", "C3") or name ("R0", "R1", "R2", "R3")
                    val stringValue = json.asString
                    PrioriteTiersPayant.fromCode(stringValue)
                        ?: PrioriteTiersPayant.valueOf(stringValue)
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
}
