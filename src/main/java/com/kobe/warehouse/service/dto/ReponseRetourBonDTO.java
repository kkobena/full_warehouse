package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.ReponseRetourBon;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ReponseRetourBonDTO {

    private Integer id;
    private LocalDateTime dateMtv;
    private UserDTO user;
    private Integer retourBonId;
    private List<ReponseRetourBonItemDTO> reponseRetourBonItems = new ArrayList<>();

    public ReponseRetourBonDTO() {}

    public ReponseRetourBonDTO(ReponseRetourBon reponseRetourBon) {
        this.id = reponseRetourBon.getId();
        this.dateMtv = reponseRetourBon.getDateMtv();
        this.user = new UserDTO(reponseRetourBon.getUser());
        if (reponseRetourBon.getRetourBon() != null) {
            this.retourBonId = reponseRetourBon.getRetourBon().getId();
        }
        this.reponseRetourBonItems = reponseRetourBon.getReponseRetourBonItems().stream().map(ReponseRetourBonItemDTO::new).toList();
    }

    public Integer getId() {
        return id;
    }

    public ReponseRetourBonDTO setId(Integer id) {
        this.id = id;
        return this;
    }

    public LocalDateTime getDateMtv() {
        return dateMtv;
    }

    public ReponseRetourBonDTO setDateMtv(LocalDateTime dateMtv) {
        this.dateMtv = dateMtv;
        return this;
    }

    public UserDTO getUser() {
        return user;
    }

    public ReponseRetourBonDTO setUser(UserDTO user) {
        this.user = user;
        return this;
    }

    public Integer getRetourBonId() {
        return retourBonId;
    }

    public ReponseRetourBonDTO setRetourBonId(Integer retourBonId) {
        this.retourBonId = retourBonId;
        return this;
    }

    public List<ReponseRetourBonItemDTO> getReponseRetourBonItems() {
        return reponseRetourBonItems;
    }

    public ReponseRetourBonDTO setReponseRetourBonItems(List<ReponseRetourBonItemDTO> reponseRetourBonItems) {
        this.reponseRetourBonItems = reponseRetourBonItems;
        return this;
    }
}
