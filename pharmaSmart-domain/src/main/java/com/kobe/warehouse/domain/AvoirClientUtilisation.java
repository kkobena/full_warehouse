package com.kobe.warehouse.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "avoir_client_utilisation")
public class AvoirClientUtilisation implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "avoir_client_id", nullable = false)
    private AvoirClient avoirClient;

    @NotNull
    @Column(name = "montant_utilise", nullable = false)
    private int montantUtilise;

    @NotNull
    @Column(name = "utilise_le", nullable = false)
    private LocalDateTime utiliseLe = LocalDateTime.now();

    @Column(name = "commentaire", length = 500)
    private String commentaire;

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "utilise_par_id", nullable = false)
    private AppUser utilisePar;

    public Integer getId() { return id; }
    public AvoirClientUtilisation setId(Integer id) { this.id = id; return this; }

    public AvoirClient getAvoirClient() { return avoirClient; }
    public AvoirClientUtilisation setAvoirClient(AvoirClient avoirClient) { this.avoirClient = avoirClient; return this; }

    public int getMontantUtilise() { return montantUtilise; }
    public AvoirClientUtilisation setMontantUtilise(int montantUtilise) { this.montantUtilise = montantUtilise; return this; }

    public LocalDateTime getUtiliseLe() { return utiliseLe; }
    public AvoirClientUtilisation setUtiliseLe(LocalDateTime utiliseLe) { this.utiliseLe = utiliseLe; return this; }

    public String getCommentaire() { return commentaire; }
    public AvoirClientUtilisation setCommentaire(String commentaire) { this.commentaire = commentaire; return this; }

    public AppUser getUtilisePar() { return utilisePar; }
    public AvoirClientUtilisation setUtilisePar(AppUser utilisePar) { this.utilisePar = utilisePar; return this; }
}
