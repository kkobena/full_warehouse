package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.AjustementStatut;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
@Entity
@Table(name = "ajust")
public class Ajust implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotNull
  @Column(name = "date_mtv", nullable = false)
  private LocalDateTime dateMtv = LocalDateTime.now();

  @ManyToOne(optional = false)
  @NotNull
  private User user;

  @NotNull
  @Enumerated(EnumType.ORDINAL)
  @Column(name = "statut", nullable = false)
  private AjustementStatut statut = AjustementStatut.PENDING;

  @ManyToOne(optional = false)
  @NotNull
  private Storage storage;

  @Column(name = "commentaire")
  private String commentaire;

  @ManyToOne(optional = false)
  @NotNull
  private WarehouseCalendar calendar;

  @Getter
  @OneToMany(
      mappedBy = "ajust",
      cascade = {CascadeType.REMOVE})
  private List<Ajustement> ajustements = new ArrayList<>();

  public void setId(Long id) {
    this.id = id;
  }

  public void setStatut(AjustementStatut statut) {
    this.statut = statut;
  }

  public Ajust setCommentaire(String commentaire) {
    this.commentaire = commentaire;
    return this;
  }

  public Ajust setCalendar(WarehouseCalendar calendar) {
    this.calendar = calendar;
    return this;
  }

  public void setDateMtv(LocalDateTime dateMtv) {
    this.dateMtv = dateMtv;
  }

  public Ajust setAjustements(List<Ajustement> ajustements) {
    this.ajustements = ajustements;
    return this;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public Ajust setStorage(Storage storage) {
    this.storage = storage;
    return this;
  }
}
