package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.RetourBonStatut;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
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
  private LocalDateTime modifiedDate ;
  @ManyToOne(optional = false)
  @NotNull
  private User user;
  @ManyToOne(optional = false)
  @NotNull
  private RetourBon retourBon;
  @NotNull
  @Enumerated(EnumType.ORDINAL)
  @Column(name = "statut", nullable = false,length = 1)
  private RetourBonStatut statut = RetourBonStatut.PROCESSING;
  @Column(name = "commentaire",length = 150)
  private String commentaire;

  @OneToMany(mappedBy = "retourBon")
  private List<RetourBonItem> retourBonItems = new ArrayList<>();
}
