package com.kobe.warehouse.service.pharmaml.dto.response;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.FIELD)
public class NonDispo {

    @XmlAttribute(name = "Additif")
    private String raison;

    @XmlAttribute(name = "Code_Reponse")
    private String codeReponse;

    public String getCodeReponse() {
        return codeReponse;
    }

    public void setCodeReponse(String codeReponse) {
        this.codeReponse = codeReponse;
    }

    public String getRaison() {
        return raison;
    }

    public void setRaison(String raison) {
        this.raison = raison;
    }
}
