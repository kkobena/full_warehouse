package com.kobe.warehouse.domain;

import org.apache.commons.lang3.StringUtils;


public final class AppUserNames {

    private AppUserNames() {
    }

    public static String LastNameFirstName(AppUser user) {
        if (user == null) {
            return null;
        }

        return StringUtils.trimToEmpty(user.getLastName()) + " " + StringUtils.trimToEmpty(
            user.getFirstName());
    }

    public static String fullName(AppUser user) {
        if (user == null) {
            return null;
        }

        return fullName(user.getLastName(), user.getFirstName());
    }


    public static String fullName(String lastName, String firstName) {

        return shortName(lastName, firstName);

    }


    public static String shortName(AppUser user) {
        if (user == null) {
            return null;
        }
        return shortName(user.getLastName(), user.getFirstName());
    }

    /**
     * Abrège le nom en son initiale : « Kone Awa » devient « K.Awa ».
     *
     * <p>Le nom de famille n'est obligatoire nulle part : l'abréger sans vérifier qu'il existe
     * levait une {@code StringIndexOutOfBoundsException} — à l'affichage d'une liste, donc bien
     * loin de la saisie fautive. Sans nom, il ne reste que le prénom.
     */
    public static String shortName(String nom, String prenom) {
        String nomAbrege = StringUtils.trimToEmpty(nom);
        String prenomComplet = StringUtils.trimToEmpty(prenom);
        if (nomAbrege.isEmpty()) {
            return prenomComplet;
        }
        return nomAbrege.charAt(0) + "." + prenomComplet;
    }
}
