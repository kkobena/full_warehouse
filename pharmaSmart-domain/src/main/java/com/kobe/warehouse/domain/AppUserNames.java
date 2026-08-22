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

    public static String shortName(String nom, String prenom) {

        return StringUtils.trimToEmpty(nom).charAt(0) + "." + StringUtils.trimToEmpty(prenom);
    }
}
