package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.RetourBonStatut;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "reponse_retour_bon")
public class ReponseRetourBon implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotNull
  @Column(name = "date_mtv", nullable = false)
  private LocalDateTime dateMtv = LocalDateTime.now();

  @NotNull
  @Column(name = "modified_date", nullable = false)
  private LocalDateTime modifiedDate;

  @ManyToOne(optional = false)
  @NotNull
  private User user;

  @ManyToOne(optional = false)
  @NotNull
  private RetourBon retourBon;

  @NotNull
  @Enumerated(EnumType.ORDINAL)
  @Column(name = "statut", nullable = false, length = 1)
  private RetourBonStatut statut = RetourBonStatut.PROCESSING;

  @Column(name = "commentaire", length = 150)
  private String commentaire;

  @ManyToOne(optional = false)
  @NotNull
  private WarehouseCalendar calendar;

  @OneToMany(mappedBy = "reponseRetourBon")
  private List<ReponseRetourBonItem> reponseRetourBonItems = new ArrayList<>();
}
