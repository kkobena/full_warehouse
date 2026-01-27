package com.kobe.warehouse.sales.domain.model

/**
 * Type de prescription (Prescription Type)
 *
 * Represents the type of medical prescription or authorization document
 * required for insurance sales.
 */
enum class PrescriptionType(val code: String, val displayName: String) {
    /**
     * Ordonnance (Medical prescription)
     * Standard prescription written by a doctor
     */
    ORDONNANCE("ORD", "Ordonnance"),

    /**
     * Bon de prise en charge (Coverage authorization)
     * Authorization document from insurance company
     */
    BON_PRISE_EN_CHARGE("BPC", "Bon de prise en charge"),

    /**
     * Protocole de soins (Care protocol)
     * Long-term treatment protocol for chronic diseases
     */
    PROTOCOLE_SOINS("PROTO", "Protocole de soins"),

    /**
     * Facture d'hospitalisation (Hospital bill)
     * Hospital discharge prescription
     */
    FACTURE_HOSPITALISATION("HOSP", "Facture d'hospitalisation"),

    /**
     * Ordonnance renouvelable (Renewable prescription)
     * Prescription that can be renewed multiple times
     */
    ORDONNANCE_RENOUVELABLE("ORD_REN", "Ordonnance renouvelable"),

    /**
     * Sans ordonnance (Over-the-counter / Without prescription)
     * For OTC medications or products not requiring prescription
     */
    SANS_ORDONNANCE("SANS", "Sans ordonnance");

    companion object {
        /**
         * Get prescription type from code
         */
        fun fromCode(code: String): PrescriptionType? {
            return entries.find { it.code == code }
        }

        /**
         * Get prescription type from display name
         */
        fun fromDisplayName(displayName: String): PrescriptionType? {
            return entries.find { it.displayName == displayName }
        }

        /**
         * Get all prescription types as list of display names
         */
        fun getAllDisplayNames(): List<String> {
            return entries.map { it.displayName }
        }
    }

    /**
     * Check if this prescription type requires a prescription document
     */
    fun requiresPrescriptionDocument(): Boolean {
        return this != SANS_ORDONNANCE
    }

    /**
     * Check if this prescription type requires a "numero de bon" (authorization number)
     */
    fun requiresNumeroBon(): Boolean {
        return this in listOf(
            BON_PRISE_EN_CHARGE,
            PROTOCOLE_SOINS,
            FACTURE_HOSPITALISATION
        )
    }
}
