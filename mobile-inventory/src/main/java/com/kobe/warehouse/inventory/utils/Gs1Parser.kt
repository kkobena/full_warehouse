package com.kobe.warehouse.inventory.utils

import java.time.LocalDate
import java.time.YearMonth

/**
 * Parsed content of a GS1 DataMatrix (pharmaceutical pack).
 * Aligned with the backend reference implementation
 * (com.kobe.warehouse.service.stock.impl.DataMatrixParserServiceImpl):
 * AI 01 = GTIN-14, AI 17 = expiry (YYMMDD), AI 10 = lot, AI 21 = serial,
 * AI 37 = quantity, AI 710-714 = NHRN (711 = CIP France)
 */
data class Gs1Data(
    val gtin: String? = null,
    val expiryDate: LocalDate? = null,
    val lotNumber: String? = null,
    val serial: String? = null,
    /** CIP transmis via NHRN (AI 711 France en priorité) */
    val nhrnCip: String? = null,
    /** AI 37 — quantité (colisage), 1 par défaut */
    val quantity: Int = 1
) {
    /**
     * CIP13/EAN13 dérivé du GTIN-14 — uniquement si le chiffre indicateur est 0
     * (sinon les 13 chiffres restants ne sont pas un EAN valide)
     */
    val cip13: String?
        get() = gtin?.takeIf { it.length == 14 && it.startsWith("0") }?.substring(1)

    /** Date au format ISO yyyy-MM-dd attendu par le backend */
    val expiryIso: String?
        get() = expiryDate?.toString()

    /** Candidats de code produit à confronter aux CIP/EAN des lignes (priorité CIP NHRN) */
    fun barcodeCandidates(): List<String> = buildList {
        nhrnCip?.let { add(it) }
        cip13?.let { add(it) }
        gtin?.let { add(it) }
    }.distinct()
}

/**
 * GS1 element-string parser for pharmacy DataMatrix codes.
 * Handles symbology identifiers (]d2, ]C1), FNC1/GS separators (ASCII 29)
 * and their textual substitutes (<GS>, {GS}) emitted by some HID scanners,
 * fixed-length AIs (01, 02, 11, 13, 15, 17) and variable-length AIs
 * (10, 21, 37, 710-714).
 */
object Gs1Parser {

    /** Séparateur de groupe FNC1 (ASCII 29) */
    private val GS = 29.toChar()

    /** AI → longueur fixe */
    private val FIXED_LENGTH_AIS = mapOf(
        "01" to 14, // GTIN
        "02" to 14,
        "11" to 6, // date de production
        "13" to 6,
        "15" to 6, // DLUO
        "17" to 6 // date de péremption
    )
    private val VARIABLE_AIS_2 = setOf("10", "21", "22", "30", "37")
    private val VARIABLE_AIS_3 = setOf("240", "241", "710", "711", "712", "713", "714")

    /** AI dont le code commence de façon plausible (détection) */
    private val LEADING_AIS = listOf("01", "10", "17", "11", "21", "710", "711", "712", "713", "714")

    /**
     * Returns parsed GS1 data, or null if the input does not look like a GS1 code
     * (plain EAN8/EAN13/CIP7/CIP13 scans return null and are used as-is).
     */
    fun parse(raw: String): Gs1Data? {
        val data = normalize(raw)
        if (!looksLikeGs1(data)) return null

        var gtin: String? = null
        var expiry: LocalDate? = null
        var lot: String? = null
        var serial: String? = null
        var nhrnCip: String? = null
        var franceCip: String? = null
        var quantity = 1

        var i = 0
        while (i < data.length) {
            if (data[i] == GS) {
                i++
                continue
            }
            val ai2 = data.substring(i, minOf(i + 2, data.length))
            val ai3 = data.substring(i, minOf(i + 3, data.length))

            val fixedLen = FIXED_LENGTH_AIS[ai2]
            when {
                fixedLen != null -> {
                    val start = i + 2
                    val end = minOf(start + fixedLen, data.length)
                    val value = data.substring(start, end)
                    when (ai2) {
                        "01" -> gtin = value
                        "17" -> expiry = parseGs1Date(value)
                    }
                    i = end
                }

                ai3 in VARIABLE_AIS_3 -> {
                    val (value, end) = readVariable(data, i + 3)
                    when (ai3) {
                        "711" -> franceCip = value // NHRN France = CIP
                        "710", "712", "713", "714" -> if (nhrnCip == null) nhrnCip = value
                    }
                    i = end
                }

                ai2 in VARIABLE_AIS_2 -> {
                    val (value, end) = readVariable(data, i + 2)
                    when (ai2) {
                        "10" -> lot = value
                        "21" -> serial = value
                        "37" -> value.trim().toIntOrNull()?.takeIf { it > 0 }?.let { quantity = it }
                    }
                    i = end
                }

                else -> {
                    // AI inconnu : on saute jusqu'au prochain séparateur GS
                    val gsIndex = data.indexOf(GS, i)
                    if (gsIndex < 0) break
                    i = gsIndex + 1
                }
            }
        }

        val hasData = gtin != null || lot != null || expiry != null || franceCip != null || nhrnCip != null
        if (!hasData) return null
        return Gs1Data(
            gtin = gtin,
            expiryDate = expiry,
            lotNumber = lot,
            serial = serial,
            nhrnCip = franceCip ?: nhrnCip,
            quantity = quantity
        )
    }

    /**
     * Retire l'identifiant de symbologie et normalise les représentations
     * textuelles du séparateur GS (douchettes HID)
     */
    private fun normalize(raw: String): String {
        var data = raw.trim()
        if (data.length > 3 && data[0] == ']') {
            data = data.substring(3)
        }
        data = data.replace("<GS>", GS.toString()).replace("{GS}", GS.toString())
        return data.trimStart(GS)
    }

    /**
     * Un code « nu » (EAN8/EAN13/CIP7/CIP13 : 7, 8 ou 13 chiffres) n'est pas un GS1 ;
     * sinon le code doit commencer par un AI connu (01 exige 14 chiffres derrière)
     */
    private fun looksLikeGs1(data: String): Boolean {
        if (data.isEmpty()) return false
        if (data.all { it.isDigit() } && data.length in setOf(7, 8, 13)) return false
        if (data.startsWith("01")) {
            return data.length >= 16 && data.substring(2, 16).all { it.isDigit() }
        }
        return data.contains(GS) || LEADING_AIS.any { data.startsWith(it) }
    }

    /** Lit une valeur à longueur variable, terminée par GS ou fin de chaîne */
    private fun readVariable(data: String, start: Int): Pair<String, Int> {
        val gsIndex = data.indexOf(GS, start)
        val end = if (gsIndex >= 0) gsIndex else data.length
        return data.substring(start, end) to end
    }

    /** YYMMDD ; jour « 00 » = dernier jour du mois (convention GS1) */
    private fun parseGs1Date(value: String): LocalDate? {
        if (value.length != 6 || !value.all { it.isDigit() }) return null
        return try {
            val year = 2000 + value.substring(0, 2).toInt()
            val month = value.substring(2, 4).toInt()
            val day = value.substring(4, 6).toInt()
            if (day == 0) {
                YearMonth.of(year, month).atEndOfMonth()
            } else {
                LocalDate.of(year, month, day)
            }
        } catch (e: Exception) {
            null
        }
    }
}
