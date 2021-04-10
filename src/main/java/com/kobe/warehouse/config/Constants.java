package com.kobe.warehouse.config;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import com.kobe.warehouse.domain.DateDimension;
import com.kobe.warehouse.domain.PaymentMode;
import com.kobe.warehouse.domain.Produit;

/**
 * Application constants.
 */
public final class Constants {

	// Regex for acceptable logins
	public static final String LOGIN_REGEX = "^(?>[a-zA-Z0-9!$&*+=?^_`{|}~.-]+@[a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)*)|(?>[_.@A-Za-z0-9-]+)$";

	public static final String SYSTEM_ACCOUNT = "system";
	public static final String DEFAULT_LANGUAGE = "fr";
	public static final String ANONYMOUS_USER = "anonymoususer";
	public static final int REFERENCE_TYPE_COMMANDE = 1;
	public static final int REFERENCE_TYPE_VENTE = 0;
	public static final long MODE_ESP = 1;

	private Constants() {
	}

	public static DateDimension DateDimension(LocalDate date) {
		int dateKey = Integer.valueOf(date.format(DateTimeFormatter.ofPattern("yyyyMMdd")));
		DateDimension dateDimension = new DateDimension();
		dateDimension.setDateKey(dateKey);
		return dateDimension;

	}

	public static Produit produitFromId(Long id) {
		Produit produit = new Produit();
		produit.setId(id);
		return produit;

	}

	public static PaymentMode getPaymentMode(Long id) {
		PaymentMode paymentMode = new PaymentMode();
		paymentMode.setId(id);
		return paymentMode;

	}

    public static Integer DateDimensionKey(LocalDate date) {
        return  Integer.valueOf(date.format(DateTimeFormatter.ofPattern("yyyyMMdd")));

    }
}
