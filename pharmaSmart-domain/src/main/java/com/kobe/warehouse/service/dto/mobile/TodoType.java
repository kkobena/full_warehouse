package com.kobe.warehouse.service.dto.mobile;

/**
 * Todo type enumeration for mobile todo list.
 */
public enum TodoType {
    REORDER("REORDER", "Commander", "package-variant", "CREATE_ORDER"),
    CALL_CLIENT("CALL_CLIENT", "Relancer client", "phone", "CALL"),
    CREATE_DISCOUNT("CREATE_DISCOUNT", "Creer promotion", "tag-outline", "CREATE_DISCOUNT"),
    INVENTORY("INVENTORY", "Inventaire", "clipboard-check-outline", "START_INVENTORY");

    private final String code;
    private final String libelle;
    private final String icon;
    private final String actionType;

    TodoType(String code, String libelle, String icon, String actionType) {
        this.code = code;
        this.libelle = libelle;
        this.icon = icon;
        this.actionType = actionType;
    }

    public String getCode() {
        return code;
    }

    public String getLibelle() {
        return libelle;
    }

    public String getIcon() {
        return icon;
    }

    public String getActionType() {
        return actionType;
    }
}
