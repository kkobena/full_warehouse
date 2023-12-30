package com.kobe.warehouse.config;

/** Application constants. */
public final class Constants {

  // Regex for acceptable logins
  public static final String LOGIN_REGEX =
      "^(?>[a-zA-Z0-9!$&*+=?^_`{|}~.-]+@[a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)*)|(?>[_.@A-Za-z0-9-]+)$";

  public static final String SYSTEM = "system";
  public static final String DEFAULT_LANGUAGE = "fr";
  public static final String ANONYMOUS_USER = "Anonymous";

  public static final int REFERENCE_TYPE_COMMANDE = 1;
  public static final int REFERENCE_TYPE_VENTE = 0;
  public static final int REFERENCE_PREVENTE_VENTE = 2;
  public static final String MODE_ESP = "CASH";

  private Constants() {}
}
