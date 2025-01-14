package com.kobe.warehouse.service.dto;

public class CodeRemiseDTO {

    private String value;
    private String codeVno;
    private String codeVo;
    private RemiseDTO remise;

    public CodeRemiseDTO() {}

    public CodeRemiseDTO(String value, String codeVno, String codeVo, RemiseDTO remise) {
        this.codeVno = codeVno;
        this.codeVo = codeVo;
        this.remise = remise;
        this.value = value;
    }

    public String getCodeVno() {
        return codeVno;
    }

    public void setCodeVno(String codeVno) {
        this.codeVno = codeVno;
    }

    public String getCodeVo() {
        return codeVo;
    }

    public void setCodeVo(String codeVo) {
        this.codeVo = codeVo;
    }

    public RemiseDTO getRemise() {
        return remise;
    }

    public void setRemise(RemiseDTO remise) {
        this.remise = remise;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
