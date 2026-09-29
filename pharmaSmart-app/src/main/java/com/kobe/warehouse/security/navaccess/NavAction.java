package com.kobe.warehouse.security.navaccess;

/** Droit {@code nav_item_role} exigé par un endpoint. */
public enum NavAction {
    /** Déduit du verbe HTTP : GET → ACCESS, POST → CREATE, PUT/PATCH → EDIT, DELETE → DELETE. */
    AUTO,
    DISPLAY,
    ACCESS,
    CREATE,
    EDIT,
    DELETE,
    EXPORT,
    EXECUTE;

    public static NavAction fromHttpMethod(String method) {
        return switch (method) {
            case "POST" -> CREATE;
            case "PUT", "PATCH" -> EDIT;
            case "DELETE" -> DELETE;
            default -> ACCESS;
        };
    }
}
