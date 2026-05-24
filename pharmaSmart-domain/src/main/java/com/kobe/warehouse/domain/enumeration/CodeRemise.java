package com.kobe.warehouse.domain.enumeration;

import com.kobe.warehouse.service.dto.CodeRemiseDTO;
import com.kobe.warehouse.service.dto.RemiseDTO;
import java.util.Arrays;
import java.util.List;

public enum CodeRemise {
    NONE("0", CodeGrilleRemise.NONE, CodeGrilleRemise.NONE),
    CODE_0("1", CodeGrilleRemise.CODE_12, CodeGrilleRemise.CODE_17),
    CODE_1("2", CodeGrilleRemise.CODE_13, CodeGrilleRemise.CODE_18),
    CODE_2("3", CodeGrilleRemise.CODE_14, CodeGrilleRemise.CODE_19),
    CODE_3("4", CodeGrilleRemise.CODE_15, CodeGrilleRemise.CODE_20),
    CODE_4("5", CodeGrilleRemise.CODE_16, CodeGrilleRemise.CODE_21),
    CODE_5("6", CodeGrilleRemise.CODE_22, CodeGrilleRemise.CODE_23),
    CODE_6("7", CodeGrilleRemise.CODE_24, CodeGrilleRemise.CODE_25),
    CODE_7("8", CodeGrilleRemise.CODE_26, CodeGrilleRemise.CODE_27),
    CODE_8("9", CodeGrilleRemise.CODE_28, CodeGrilleRemise.CODE_29);

    private final String value;
    private final CodeGrilleRemise codeVno;
    private final CodeGrilleRemise codeVo;

    CodeRemise(String value, CodeGrilleRemise codeVno, CodeGrilleRemise codeVo) {
        this.value = value;
        this.codeVno = codeVno;
        this.codeVo = codeVo;
    }

    public static CodeRemise fromValue(String value) {
        for (CodeRemise codeRemise : values()) {
            if (codeRemise.getValue().equals(value)) {
                return codeRemise;
            }
        }
        return NONE;
    }

    public static CodeRemise fromGrille(CodeGrilleRemise codeGrilleRemise) {
        for (CodeRemise codeRemise : values()) {
            if (codeRemise.getCodeVno().equals(codeGrilleRemise) || codeRemise.getCodeVo().equals(codeGrilleRemise)) {
                return codeRemise;
            }
        }
        throw new IllegalArgumentException("Invalid CodeGrilleRemise");
    }

    public static CodeRemiseDTO toDTO(CodeRemise codeRemise) {
        return new CodeRemiseDTO(codeRemise.getValue(), codeRemise.getCodeVno().getValue(), codeRemise.getCodeVo().getValue(), null);
    }

    public static CodeRemiseDTO toDTO(CodeRemise codeRemise, RemiseDTO remise) {
        return new CodeRemiseDTO(codeRemise.getValue(), codeRemise.getCodeVno().getValue(), codeRemise.getCodeVo().getValue(), remise);
    }

    public static List<CodeRemiseDTO> toListDTO() {
        return Arrays.stream(CodeRemise.values()).map(CodeRemise::toDTO).toList();
    }

    public CodeGrilleRemise getCodeVno() {
        return codeVno;
    }

    public CodeGrilleRemise getCodeVo() {
        return codeVo;
    }

    public String getValue() {
        return value;
    }
}
