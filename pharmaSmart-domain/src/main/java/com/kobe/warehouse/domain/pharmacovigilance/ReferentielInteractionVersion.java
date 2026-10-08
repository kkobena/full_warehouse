package com.kobe.warehouse.domain.pharmacovigilance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/** Une version importée d'une source d'interactions ; seule une version publiée (relue) est utilisée. */
@Entity
@Table(name = "referentiel_interaction_version")
public class ReferentielInteractionVersion implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "source", nullable = false)
    private String source;

    @Column(name = "version", nullable = false)
    private String version;

    @Column(name = "importe_le", nullable = false)
    private LocalDateTime importeLe = LocalDateTime.now();

    @Column(name = "importe_par")
    private String importePar;

    @Column(name = "publie", nullable = false)
    private boolean publie;

    @Column(name = "publie_le")
    private LocalDateTime publieLe;

    @Column(name = "publie_par")
    private String publiePar;

    @Column(name = "commentaire")
    private String commentaire;

    protected ReferentielInteractionVersion() {}

    public ReferentielInteractionVersion(String source, String version, String importePar) {
        this.source = source;
        this.version = version;
        this.importePar = importePar;
    }

    /** Publier = attester que la relecture humaine est faite. */
    public void publier(String par) {
        this.publie = true;
        this.publieLe = LocalDateTime.now();
        this.publiePar = par;
    }

    public Integer getId() {
        return id;
    }

    public String getSource() {
        return source;
    }

    public String getVersion() {
        return version;
    }

    public LocalDateTime getImporteLe() {
        return importeLe;
    }

    public String getImportePar() {
        return importePar;
    }

    public boolean isPublie() {
        return publie;
    }

    public LocalDateTime getPublieLe() {
        return publieLe;
    }

    public String getPubliePar() {
        return publiePar;
    }

    public String getCommentaire() {
        return commentaire;
    }

    public void setCommentaire(String commentaire) {
        this.commentaire = commentaire;
    }
}
