package com.kobe.warehouse.domain.enumeration;

/**
 * The TypeMagasin enumeration.
 */
public enum StorageType {
	PRINCIPAL("Stockage principal"), SAFETY_STOCK("Reserve"),
	POINT_DE_VENTE("Point de vente");

	public final String value;

	public String getValue() {
		return value;
	}

	private StorageType(String value) {
		this.value = value;
	}

	public static StorageType valueOfLabel(String value) {
		for (StorageType e : values()) {
			if (e.value.equals(value)) {
				return e;
			}
		}
		return null;
	}
}
