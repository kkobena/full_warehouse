# Stratégie d'Association Ordonnance Médicale - Vente

## 📋 Analyse de l'Existant

### Entités Actuelles
- **`Sales`** : Entité parent de toutes les ventes
  - Champ `typePrescription` (PRESCRIPTION, CONSEIL, DEPOT)
  - Champ `customer` : Client/Patient associé à la vente
  - Champ `seller` : Pharmacien vendeur
  - Champ `commentaire` : Zone de texte libre

- **`ThirdPartySales`** : Vente avec tiers-payant (hérite de Sales)
  - Champ `numBon` : Numéro du bon de commande
  - Champ `ayantDroit` : Ayant droit (bénéficiaire)
  - Relations vers lignes de vente spécifiques tiers-payant

### Points Clés Identifiés
✅ **TypePrescription existe** mais ne gère que le type (PRESCRIPTION, CONSEIL, DEPOT)
❌ **Pas d'entité Ordonnance** pour stocker les prescriptions médicales
❌ **Pas de lien avec un médecin prescripteur**
❌ **Pas de traçabilité** des médicaments prescrits vs délivrés
❌ **Pas de gestion des renouvellements** et délivrances partielles
❌ **Pas de stockage documentaire** (scan ordonnance)

---

## 🎯 Objectifs Métier

### Exigences Fonctionnelles
1. **Traçabilité réglementaire**
   - Enregistrer l'ordonnance médicale originale
   - Identifier le médecin prescripteur (nom, RPPS, spécialité)
   - Date de prescription et validité
   - Posologie et durée de traitement prescrite
   - Tracer toutes les délivrances (partielle ou totale)

2. **Gestion documentaire**
   - Scanner/numériser l'ordonnance papier
   - Stocker les ordonnances électroniques (e-prescription)
   - Conservation légale obligatoire (France: 3 ans minimum)
   - Horodatage et signature pharmacien

3. **Contrôle pharmaceutique**
   - Validation pharmaceutique lors de la délivrance
   - Contrôle interactions médicamenteuses
   - Vérification contre-indications
   - Alertes stupéfiants et psychotropes
   - Limites de délivrance réglementaires

4. **Facturation et remboursement**
   - Lien ordonnance → tiers-payant
   - Télétransmission Sécurité Sociale
   - Code prescripteur pour facturation
   - Taux de remboursement selon prescription

---

## 🏗️ Architecture Proposée

### Option 1 : Modèle Simple (Recommandé pour MVP)

#### Nouvelle Entité : `MedicalPrescription` (Ordonnance Médicale)

```java
@Entity
@Table(name = "medical_prescription")
public class MedicalPrescription implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relations
    @ManyToOne(optional = false)
    @JoinColumn(name = "patient_id")
    private Customer patient;

    @ManyToOne(optional = false)
    @JoinColumn(name = "doctor_id")
    private Doctor doctor; // Le médecin prescripteur

    @ManyToOne(optional = false)
    @JoinColumn(name = "received_by_id") // Pharmacien qui reçoit l'ordonnance
    private AppUser receivedBy;

    // Métadonnées ordonnance
    @NotNull
    @Column(name = "prescription_number", unique = true, length = 50)
    private String prescriptionNumber; // Numéro unique interne

    @Column(name = "external_number", length = 50) // Numéro sur l'ordonnance si présent
    private String externalNumber;

    @NotNull
    @Column(name = "prescription_date") // Date de prescription par le médecin
    private LocalDate prescriptionDate;

    @NotNull
    @Column(name = "reception_date") // Date de réception à la pharmacie
    private LocalDateTime receptionDate;

    @Column(name = "date_expiration")
    private LocalDate dateExpiration; // 3 mois par défaut, 1 an pour ALD

    @Enumerated(EnumType.STRING)
    @Column(name = "prescription_type", length = 30)
    private PrescriptionType prescriptionType;
    // ORDONNANCE_SIMPLE, ORDONNANCE_RENOUVELABLE, ORDONNANCE_CHRONIQUE, ORDONNANCE_ALD

    @Column(name = "is_renewable")
    private Boolean isRenewable = false;

    @Column(name = "max_renewals") // Nombre de renouvellements autorisés
    private Integer maxRenewals = 0;

    @Column(name = "renewals_done") // Nombre de renouvellements effectués
    private Integer renewalsDone = 0;

    @Column(name = "commentaire_medecin", columnDefinition = "TEXT")
    private String commentaireMedecin; // Notes du médecin

    @Column(name = "commentaire_pharmacien", columnDefinition = "TEXT")
    private String commentairePharmacien; // Notes du pharmacien

    // Gestion documentaire
    @Column(name = "document_path", length = 500)
    private String documentPath; // Chemin vers scan ordonnance

    @Column(name = "document_format", length = 10) // PDF, JPG, PNG
    private String documentFormat;

    @Column(name = "is_electronic")
    private Boolean isElectronic = false; // E-prescription

    @Column(name = "electronic_signature")
    private String electronicSignature; // Signature électronique médecin

    // Statut et délivrance
    @Enumerated(EnumType.STRING)
    @Column(name = "statut", length = 20)
    private PrescriptionStatut statut;
    // ENREGISTREE, EN_COURS, DELIVREE_PARTIELLE, DELIVREE_TOTALE, EXPIREE, ANNULEE

    @Column(name = "is_urgent")
    private Boolean isUrgent = false;

    @Column(name = "has_stupefiant") // Contient stupéfiant
    private Boolean hasStupefiant = false;

    @NotNull
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @NotNull
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Liste des délivrances (ventes) liées à cette ordonnance
    @OneToMany(mappedBy = "prescription", fetch = FetchType.LAZY)
    private Set<Sales> deliveries = new HashSet<>();

    // Méthodes utilitaires
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.receptionDate = LocalDateTime.now();

        if (this.prescriptionNumber == null) {
            this.prescriptionNumber = generatePrescriptionNumber();
        }

        // Date expiration par défaut: 3 mois
        if (this.dateExpiration == null) {
            this.dateExpiration = LocalDate.now().plusMonths(3);
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    private String generatePrescriptionNumber() {
        // Format: ORD-{YEAR}{MONTH}{DAY}-{SEQUENCE}
        // Exemple: ORD-20260125-00001
        return String.format("ORD-%s-%05d",
            LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE),
            // Séquence générée par database ou service
        );
    }

    public boolean canBeRenewed() {
        return isRenewable && renewalsDone < maxRenewals
               && !isExpired() && statut != PrescriptionStatut.ANNULEE;
    }

    public boolean isExpired() {
        return LocalDate.now().isAfter(dateExpiration);
    }
}
```

#### Nouvelle Entité : `PrescriptionLine` (Lignes d'ordonnance)

```java
@Entity
@Table(name = "prescription_line")
public class PrescriptionLine implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "prescription_id")
    private MedicalPrescription prescription;

    @ManyToOne // Produit prescrit (peut être NULL si produit non référencé)
    @JoinColumn(name = "produit_prescrit_id")
    private Produit produitPrescrit;

    @Column(name = "designation_produit", length = 500) // Libellé sur ordonnance
    private String designationProduit;

    @Column(name = "quantite_prescrite", nullable = false)
    private Integer quantitePrescrite;

    @Column(name = "quantite_delivree")
    private Integer quantiteDelivree = 0;

    @Column(name = "quantite_restante")
    private Integer quantiteRestante; // Calculée automatiquement

    @Column(name = "posologie", length = 500)
    private String posologie; // Ex: "1 cp matin et soir"

    @Column(name = "duree_traitement_jours")
    private Integer dureeTraitementJours;

    @Column(name = "nombre_prises_par_jour")
    private Integer nombrePrisesParJour;

    @Column(name = "voie_administration", length = 50) // Orale, Injectable, etc.
    private String voieAdministration;

    @Column(name = "non_substituable")
    private Boolean nonSubstituable = false; // Si médecin interdit substitution

    @ManyToOne // Produit délivré (si différent du prescrit)
    @JoinColumn(name = "produit_delivre_id")
    private Produit produitDelivre;

    @Column(name = "motif_substitution", length = 255)
    private String motifSubstitution;

    @Column(name = "conseil_delivrance", columnDefinition = "TEXT")
    private String conseilDelivrance;

    @Column(name = "is_stupefiant")
    private Boolean isStupefiant = false;

    @Column(name = "code_cip13", length = 13) // Code produit sur ordonnance
    private String codeCip13;

    @Column(name = "ordre_ligne") // Ordre d'apparition sur ordonnance
    private Integer ordreLigne;

    @PrePersist
    @PreUpdate
    public void calculateRestante() {
        if (quantitePrescrite != null && quantiteDelivree != null) {
            this.quantiteRestante = quantitePrescrite - quantiteDelivree;
        }
    }
}
```

#### Nouvelle Entité : `Doctor` (Médecin Prescripteur)

```java
@Entity
@Table(name = "doctor")
public class Doctor implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "nom", length = 100, nullable = false)
    private String nom;

    @Column(name = "prenom", length = 100)
    private String prenom;

    @Column(name = "titre", length = 50) // Dr, Pr, etc.
    private String titre;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 30)
    private DoctorType type; // MEDECIN_GENERALISTE, MEDECIN_SPECIALISTE, etc.

    @Column(name = "specialite", length = 100)
    private String specialite; // Cardiologue, Pédiatre, etc.

    // Identifiants professionnels
    @Column(name = "numero_rpps", unique = true, length = 11) // Répertoire Partagé Professions Santé
    private String numeroRPPS;

    @Column(name = "numero_am", length = 9) // Numéro Assurance Maladie
    private String numeroAM;

    @Column(name = "numero_ordre", length = 20) // Numéro Ordre des Médecins
    private String numeroOrdre;

    // Coordonnées
    @Column(name = "adresse", columnDefinition = "TEXT")
    private String adresse;

    @Column(name = "code_postal", length = 10)
    private String codePostal;

    @Column(name = "ville", length = 100)
    private String ville;

    @Column(name = "telephone", length = 20)
    private String telephone;

    @Column(name = "fax", length = 20)
    private String fax;

    @Column(name = "email", length = 100)
    private String email;

    // Statut
    @Column(name = "actif")
    private Boolean actif = true;

    @Column(name = "conventione") // Secteur 1, 2, etc.
    private Boolean conventione = true;

    @Column(name = "commentaire", columnDefinition = "TEXT")
    private String commentaire;

    // Métadonnées
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public String getFullName() {
        StringBuilder sb = new StringBuilder();
        if (titre != null) {
            sb.append(titre).append(" ");
        }
        sb.append(nom);
        if (prenom != null) {
            sb.append(" ").append(prenom);
        }
        return sb.toString();
    }
}
```

#### Enums Associés

```java
public enum DoctorType {
    MEDECIN_GENERALISTE("Médecin généraliste"),
    MEDECIN_SPECIALISTE("Médecin spécialiste"),
    DENTISTE("Chirurgien-dentiste"),
    SAGE_FEMME("Sage-femme"),
    INFIRMIER_PRESCRIPTEUR("Infirmier en pratique avancée"),
    KINESITHERAPEUTE("Masseur-kinésithérapeute"),
    PODOLOGUE("Pédicure-podologue");

    private final String libelle;

    DoctorType(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}

public enum PrescriptionType {
    ORDONNANCE_SIMPLE("Ordonnance simple"),
    ORDONNANCE_RENOUVELABLE("Ordonnance renouvelable"),
    ORDONNANCE_CHRONIQUE("Traitement chronique"),
    ORDONNANCE_ALD("Affection Longue Durée"),
    ORDONNANCE_EXCEPTION("Médicament d'exception"),
    ORDONNANCE_SECURISEE("Ordonnance sécurisée (stupéfiants)"),
    ORDONNANCE_BIZONE("Ordonnance bizone");

    private final String libelle;

    PrescriptionType(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}

public enum PrescriptionStatut {
    ENREGISTREE("Enregistrée"),                    // Ordonnance enregistrée
    EN_COURS("En cours de délivrance"),            // Délivrance commencée
    DELIVREE_PARTIELLE("Délivrée partiellement"),  // Délivrance partielle
    DELIVREE_TOTALE("Délivrée totalement"),        // Tous produits délivrés
    EXPIREE("Expirée"),                            // Date dépassée
    ANNULEE("Annulée");                            // Annulée

    private final String libelle;

    PrescriptionStatut(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
```

#### Modification de l'entité `Sales`

```java
// Ajout dans la classe Sales
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "prescription_id")
private MedicalPrescription prescription; // Lien vers ordonnance

// Getter/Setter
public MedicalPrescription getPrescription() {
    return prescription;
}

public Sales setPrescription(MedicalPrescription prescription) {
    this.prescription = prescription;
    return this;
}
```

---

## 📐 Schéma de Relations

```
┌──────────────────────┐
│       Doctor         │ 1
│  (Médecin prescr.)   │───┐
└──────────────────────┘   │
                           │ N:1
                           │
┌──────────────────────┐   │    ┌──────────────────────┐
│      Customer        │ 1 │    │      AppUser         │
│   (Patient/Client)   │───┼────┤   (Pharmacien)       │
└──────────────────────┘   │    └──────────────────────┘
            │              │              │
            │ 1            │ 1            │ 1
            │              │              │
            │              │              │
            └──────────────┼──────────────┘
                           │
                           │ N
                           │
                    ┌──────▼──────────────┐
                    │ MedicalPrescription │
                    │   (Ordonnance)      │
                    └──────┬──────────────┘
                           │
                           │ 1:N
                           │
                    ┌──────▼──────────────┐
                    │  PrescriptionLine   │
                    │  (Lignes ordonnance)│
                    └──────┬──────────────┘
                           │
                           │ N:1
                           │
                    ┌──────▼──────────────┐
                    │      Produit        │
                    │   (Médicament)      │
                    └─────────────────────┘

┌──────────────────────────┐
│         Sales            │ N
│       (Vente)            │───┐
└──────────────────────────┘   │
            │                  │ N:1 (plusieurs délivrances possibles)
            │ 1:N              │
            │                  │
┌───────────▼──────────────┐   │
│      SalesLine           │   │
│   (Lignes vente)         │   │
└──────────────────────────┘   │
                               │
                        ┌──────▼──────────────┐
                        │ MedicalPrescription │
                        │   (Ordonnance)      │
                        └─────────────────────┘

Légende :
- Une ordonnance appartient à UN patient et UN médecin
- Une ordonnance est reçue par UN pharmacien
- Une ordonnance peut avoir PLUSIEURS délivrances (ventes)
- Chaque vente est liée à UNE ordonnance (optionnel pour ventes sans ordonnance)
```

---

## 🔄 Flux Métier Proposé

### Scénario 1 : Ordonnance Simple - Délivrance Totale

```
1. RÉCEPTION PATIENT
   - Patient présente ordonnance papier du médecin
   - Vérification identité patient
   ↓
2. SCAN ORDONNANCE (optionnel mais recommandé)
   - Scanner/photographier l'ordonnance
   - Upload : /ordonnances/2026/01/ORD-20260125-00001.pdf
   ↓
3. ENREGISTREMENT MÉDECIN
   - Rechercher médecin dans base (par RPPS ou nom)
   - Si nouveau : Créer fiche Doctor
     * Nom, prénom, RPPS, spécialité
     * Adresse cabinet
   ↓
4. CRÉATION ORDONNANCE (MedicalPrescription)
   - patient: sélection/création client
   - doctor: médecin trouvé/créé
   - receivedBy: pharmacien connecté
   - prescriptionDate: date sur ordonnance
   - prescriptionType: ORDONNANCE_SIMPLE
   - dateExpiration: +3 mois
   - documentPath: chemin scan
   - statut: ENREGISTREE
   ↓
5. SAISIE LIGNES ORDONNANCE (PrescriptionLine)
   Pour chaque médicament sur l'ordonnance:
   - designationProduit: "DOLIPRANE 1000mg"
   - produitPrescrit: recherche dans base produits
   - quantitePrescrite: 2 boîtes
   - posologie: "1 cp toutes les 6h si douleur"
   - dureeTraitementJours: 7 jours
   ↓
6. VALIDATION PHARMACEUTIQUE
   - Contrôle interactions médicamenteuses (API Vidal)
   - Vérification contre-indications patient
   - Vérification posologie
   - Alertes si stupéfiant/psychotrope
   - statut → EN_COURS
   ↓
7. PRÉPARATION DÉLIVRANCE
   - Vérification stock pour chaque ligne
   - Substitution générique si autorisée
   - Si substitution:
     * produitDelivre ≠ produitPrescrit
     * motifSubstitution: "Générique disponible"
     * Information patient obligatoire
   ↓
8. CRÉATION VENTE (Sales/ThirdPartySales)
   - prescription: lien vers MedicalPrescription
   - typePrescription: PRESCRIPTION
   - customer: patient
   - seller: pharmacien

   Pour chaque PrescriptionLine → créer SalesLine:
   - produit: produitDelivre (ou produitPrescrit)
   - quantité: quantitePrescrite
   ↓
9. MISE À JOUR ORDONNANCE
   - Pour chaque PrescriptionLine:
     * quantiteDelivree += quantité vendue
     * quantiteRestante = quantitePrescrite - quantiteDelivree
   - Si toutes lignes délivrées totalement:
     * prescription.statut = DELIVREE_TOTALE
   ↓
10. FINALISATION
    - Annotation ordonnance papier:
      * Date délivrance
      * Tampon pharmacie
      * Signature pharmacien
    - Remise médicaments + conseils
    - Impression ticket de caisse
    - Archivage ordonnance
```

### Scénario 2 : Délivrance Partielle

```
1. Patient présente ordonnance mais stock insuffisant
   ↓
2. Enregistrement ordonnance (étapes 1-6 du Scénario 1)
   ↓
3. DÉLIVRANCE PARTIELLE
   - Ligne 1: Doliprane → stock OK → délivre 2 boîtes
   - Ligne 2: Antibiotique → rupture stock → délivre 0
   ↓
4. CRÉATION VENTE PARTIELLE
   - Créer Sales avec uniquement produits disponibles
   - prescription: lien ordonnance
   ↓
5. MISE À JOUR ORDONNANCE
   - Ligne 1: quantiteDelivree = 2, quantiteRestante = 0
   - Ligne 2: quantiteDelivree = 0, quantiteRestante = 2
   - prescription.statut = DELIVREE_PARTIELLE
   ↓
6. ANNOTATION ORDONNANCE
   - Noter "Délivrance partielle - reste antibiotique"
   - Date et signature
   - Remettre ordonnance au patient
   ↓
7. Patient revient quand produit disponible
   ↓
8. NOUVELLE DÉLIVRANCE sur même ordonnance
   - Rechercher ordonnance par numéro
   - Vérifier quantiteRestante > 0
   - Créer nouvelle Sales liée à même prescription
   - Mettre à jour quantiteDelivree
   - Si tout délivré → statut = DELIVREE_TOTALE
```

### Scénario 3 : Ordonnance Renouvelable

```
1. Patient présente ordonnance avec mention "Renouvelable X fois"
   ↓
2. ENREGISTREMENT ORDONNANCE
   - prescriptionType: ORDONNANCE_RENOUVELABLE
   - isRenewable: true
   - maxRenewals: X (selon ordonnance)
   - renewalsDone: 0
   - dateExpiration: selon type (1 an pour ALD)
   ↓
3. PREMIÈRE DÉLIVRANCE (mois 1)
   - Créer Sales (flux normal)
   - renewalsDone = 0
   - statut = DELIVREE_TOTALE
   ↓
4. PATIENT REVIENT POUR RENOUVELLEMENT (mois 2)
   - Rechercher ordonnance du patient
   - Vérifier canBeRenewed():
     * renewalsDone < maxRenewals ✓
     * !isExpired() ✓
     * statut != ANNULEE ✓
   ↓
5. DÉLIVRANCE RENOUVELLEMENT
   - Créer nouvelle Sales
   - Lien vers même MedicalPrescription
   - renewalsDone++
   ↓
6. Si renewalsDone == maxRenewals
   - Informer patient: "Dernière délivrance, revoir médecin"
   - Flaguer ordonnance pour suivi
```

### Scénario 4 : Ordonnance avec Tiers-Payant

```
1. Patient présente ordonnance + Carte Vitale
   ↓
2. Enregistrement ordonnance (flux normal)
   ↓
3. LECTURE CARTE VITALE
   - Récupération droits Sécurité Sociale
   - Vérification taux remboursement
   - Identification tiers-payant (ClientTiersPayant)
   ↓
4. CRÉATION ThirdPartySales
   - prescription: lien ordonnance
   - typePrescription: PRESCRIPTION
   - numBon: généré automatiquement
   - partAssure: montant patient
   - partTiersPayant: montant Sécu
   ↓
5. TÉLÉTRANSMISSION
   - Envoi feuille de soins électronique
   - Référence ordonnance
   - Code médecin (RPPS)
   - Code produits délivrés
   ↓
6. Patient paie uniquement partAssure
```

### Scénario 5 : Stupéfiants (Ordonnance Sécurisée)

```
1. Patient présente ordonnance sécurisée (filigrane)
   ↓
2. VÉRIFICATIONS RENFORCÉES
   - Vérifier authenticité ordonnance (filigrane, numérotation)
   - Vérifier identité patient (carte ID)
   - Vérifier RPPS médecin
   ↓
3. ENREGISTREMENT
   - prescriptionType: ORDONNANCE_SECURISEE
   - hasStupefiant: true
   - Scan OBLIGATOIRE
   ↓
4. SAISIE LIGNES
   - Pour chaque stupéfiant:
     * isStupefiant: true
     * Vérification durée maximale prescription
     * Vérification quantité maximale
   ↓
5. DÉLIVRANCE
   - Annotation ordonnance
   - Signature patient obligatoire
   ↓
6. REGISTRE STUPÉFIANTS
   - Enregistrement dans registre séparé
   - Traçabilité complète
   - Conservation 10 ans minimum
```

---

## 💾 Migration Base de Données

### Flyway Migration V1.1.0__medical_prescription.sql

```sql
-- Table médecins prescripteurs
CREATE TABLE doctor (
    id BIGSERIAL PRIMARY KEY,
    nom VARCHAR(100) NOT NULL,
    prenom VARCHAR(100),
    titre VARCHAR(50),
    type VARCHAR(30),
    specialite VARCHAR(100),
    numero_rpps VARCHAR(11) UNIQUE,
    numero_am VARCHAR(9),
    numero_ordre VARCHAR(20),
    adresse TEXT,
    code_postal VARCHAR(10),
    ville VARCHAR(100),
    telephone VARCHAR(20),
    fax VARCHAR(20),
    email VARCHAR(100),
    actif BOOLEAN DEFAULT TRUE,
    conventione BOOLEAN DEFAULT TRUE,
    commentaire TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Index pour recherche rapide médecins
CREATE INDEX idx_doctor_rpps ON doctor(numero_rpps);
CREATE INDEX idx_doctor_nom ON doctor(nom);
CREATE INDEX idx_doctor_type ON doctor(type);
CREATE INDEX idx_doctor_actif ON doctor(actif);
CREATE INDEX idx_doctor_nom_prenom ON doctor(nom, prenom);

-- Table principale ordonnances médicales
CREATE TABLE medical_prescription (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    doctor_id BIGINT NOT NULL,
    received_by_id BIGINT NOT NULL, -- Pharmacien qui enregistre
    prescription_number VARCHAR(50) UNIQUE NOT NULL,
    external_number VARCHAR(50), -- Numéro sur ordonnance si présent
    prescription_date DATE NOT NULL, -- Date prescription médecin
    reception_date TIMESTAMP NOT NULL DEFAULT NOW(), -- Date réception pharmacie
    date_expiration DATE NOT NULL,
    prescription_type VARCHAR(30) NOT NULL,
    is_renewable BOOLEAN DEFAULT FALSE,
    max_renewals INTEGER DEFAULT 0,
    renewals_done INTEGER DEFAULT 0,
    commentaire_medecin TEXT,
    commentaire_pharmacien TEXT,
    document_path VARCHAR(500), -- Chemin scan ordonnance
    document_format VARCHAR(10), -- PDF, JPG, PNG
    is_electronic BOOLEAN DEFAULT FALSE,
    electronic_signature VARCHAR(255),
    statut VARCHAR(20) NOT NULL DEFAULT 'ENREGISTREE',
    is_urgent BOOLEAN DEFAULT FALSE,
    has_stupefiant BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    -- Contraintes FK
    CONSTRAINT fk_prescription_patient
        FOREIGN KEY (patient_id)
        REFERENCES customer(id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_prescription_doctor
        FOREIGN KEY (doctor_id)
        REFERENCES doctor(id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_prescription_received_by
        FOREIGN KEY (received_by_id)
        REFERENCES app_user(id)
        ON DELETE RESTRICT
);

-- Index pour performances
CREATE INDEX idx_prescription_patient ON medical_prescription(patient_id);
CREATE INDEX idx_prescription_doctor ON medical_prescription(doctor_id);
CREATE INDEX idx_prescription_received_by ON medical_prescription(received_by_id);
CREATE INDEX idx_prescription_date ON medical_prescription(prescription_date);
CREATE INDEX idx_prescription_number ON medical_prescription(prescription_number);
CREATE INDEX idx_prescription_statut ON medical_prescription(statut);
CREATE INDEX idx_prescription_type ON medical_prescription(prescription_type);
CREATE INDEX idx_prescription_expiration ON medical_prescription(date_expiration);
CREATE INDEX idx_prescription_reception_date ON medical_prescription(reception_date);

-- Table lignes d'ordonnance
CREATE TABLE prescription_line (
    id BIGSERIAL PRIMARY KEY,
    prescription_id BIGINT NOT NULL,
    produit_prescrit_id BIGINT, -- Peut être NULL si produit non référencé
    designation_produit VARCHAR(500), -- Libellé sur ordonnance
    quantite_prescrite INTEGER NOT NULL,
    quantite_delivree INTEGER DEFAULT 0,
    quantite_restante INTEGER, -- Calculée automatiquement
    posologie VARCHAR(500),
    duree_traitement_jours INTEGER,
    nombre_prises_par_jour INTEGER,
    voie_administration VARCHAR(50),
    non_substituable BOOLEAN DEFAULT FALSE, -- Médecin interdit substitution
    produit_delivre_id BIGINT, -- Si différent du prescrit
    motif_substitution VARCHAR(255),
    conseil_delivrance TEXT,
    is_stupefiant BOOLEAN DEFAULT FALSE,
    code_cip13 VARCHAR(13), -- Code produit sur ordonnance
    ordre_ligne INTEGER, -- Ordre d'apparition

    CONSTRAINT fk_prescription_line_prescription
        FOREIGN KEY (prescription_id)
        REFERENCES medical_prescription(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_prescription_line_produit_prescrit
        FOREIGN KEY (produit_prescrit_id)
        REFERENCES produit(id),
    CONSTRAINT fk_prescription_line_produit_delivre
        FOREIGN KEY (produit_delivre_id)
        REFERENCES produit(id)
);

CREATE INDEX idx_prescription_line_prescription ON prescription_line(prescription_id);
CREATE INDEX idx_prescription_line_produit_prescrit ON prescription_line(produit_prescrit_id);
CREATE INDEX idx_prescription_line_produit_delivre ON prescription_line(produit_delivre_id);
CREATE INDEX idx_prescription_line_statut ON prescription_line(quantite_restante);

-- Ajout FK dans table Sales pour lien ordonnance
ALTER TABLE sales
ADD COLUMN prescription_id BIGINT,
ADD CONSTRAINT fk_sales_prescription
    FOREIGN KEY (prescription_id)
    REFERENCES medical_prescription(id)
    ON DELETE SET NULL;

CREATE INDEX idx_sales_prescription ON sales(prescription_id);

-- Séquence pour numéro ordonnance
CREATE SEQUENCE prescription_number_seq START 1;

-- Fonction génération numéro ordonnance
CREATE OR REPLACE FUNCTION generate_prescription_number()
RETURNS VARCHAR(50) AS $$
DECLARE
    next_seq INTEGER;
    date_part VARCHAR(8);
    result VARCHAR(50);
BEGIN
    next_seq := nextval('prescription_number_seq');
    date_part := TO_CHAR(CURRENT_DATE, 'YYYYMMDD');
    result := 'ORD-' || date_part || '-' || LPAD(next_seq::TEXT, 5, '0');
    RETURN result;
END;
$$ LANGUAGE plpgsql;

-- Trigger auto-génération numéro si null
CREATE OR REPLACE FUNCTION set_prescription_number()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.prescription_number IS NULL THEN
        NEW.prescription_number := generate_prescription_number();
    END IF;
    -- Date expiration par défaut si non fournie
    IF NEW.date_expiration IS NULL THEN
        NEW.date_expiration := NEW.prescription_date + INTERVAL '3 months';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_prescription_number
BEFORE INSERT ON medical_prescription
FOR EACH ROW
EXECUTE FUNCTION set_prescription_number();

-- Trigger calcul quantité restante
CREATE OR REPLACE FUNCTION calculate_quantite_restante()
RETURNS TRIGGER AS $$
BEGIN
    NEW.quantite_restante := COALESCE(NEW.quantite_prescrite, 0) - COALESCE(NEW.quantite_delivree, 0);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_quantite_restante
BEFORE INSERT OR UPDATE ON prescription_line
FOR EACH ROW
EXECUTE FUNCTION calculate_quantite_restante();

-- Trigger mise à jour statut ordonnance après délivrance
CREATE OR REPLACE FUNCTION update_prescription_statut()
RETURNS TRIGGER AS $$
DECLARE
    total_lignes INTEGER;
    lignes_completes INTEGER;
    lignes_partielles INTEGER;
BEGIN
    -- Compter lignes
    SELECT COUNT(*) INTO total_lignes
    FROM prescription_line
    WHERE prescription_id = NEW.prescription_id;

    -- Compter lignes totalement délivrées
    SELECT COUNT(*) INTO lignes_completes
    FROM prescription_line
    WHERE prescription_id = NEW.prescription_id
    AND quantite_restante = 0;

    -- Compter lignes partiellement délivrées
    SELECT COUNT(*) INTO lignes_partielles
    FROM prescription_line
    WHERE prescription_id = NEW.prescription_id
    AND quantite_delivree > 0
    AND quantite_restante > 0;

    -- Mettre à jour statut ordonnance
    IF lignes_completes = total_lignes THEN
        UPDATE medical_prescription
        SET statut = 'DELIVREE_TOTALE',
            updated_at = NOW()
        WHERE id = NEW.prescription_id;
    ELSIF lignes_partielles > 0 OR lignes_completes > 0 THEN
        UPDATE medical_prescription
        SET statut = 'DELIVREE_PARTIELLE',
            updated_at = NOW()
        WHERE id = NEW.prescription_id;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_prescription_statut
AFTER INSERT OR UPDATE ON prescription_line
FOR EACH ROW
EXECUTE FUNCTION update_prescription_statut();

-- Vue pour ordonnances expirées
CREATE OR REPLACE VIEW expired_prescriptions AS
SELECT p.*, d.nom AS doctor_nom, c.firstName AS patient_nom
FROM medical_prescription p
JOIN doctor d ON p.doctor_id = d.id
JOIN customer c ON p.patient_id = c.id
WHERE p.date_expiration < CURRENT_DATE
AND p.statut NOT IN ('DELIVREE_TOTALE', 'ANNULEE', 'EXPIREE');

-- Procédure expiration automatique ordonnances
CREATE OR REPLACE FUNCTION mark_expired_prescriptions()
RETURNS INTEGER AS $$
DECLARE
    affected_rows INTEGER;
BEGIN
    UPDATE medical_prescription
    SET statut = 'EXPIREE',
        updated_at = NOW()
    WHERE date_expiration < CURRENT_DATE
    AND statut NOT IN ('DELIVREE_TOTALE', 'ANNULEE', 'EXPIREE');

    GET DIAGNOSTICS affected_rows = ROW_COUNT;
    RETURN affected_rows;
END;
$$ LANGUAGE plpgsql;

-- Job cron (à configurer dans PostgreSQL)
-- SELECT cron.schedule('expire-prescriptions', '0 2 * * *', 'SELECT mark_expired_prescriptions()');
```

---

## 🔧 Services Backend

### MedicalPrescriptionService

```java
public interface MedicalPrescriptionService {

    // CRUD basique
    MedicalPrescription create(MedicalPrescriptionDTO dto);
    MedicalPrescription update(Long id, MedicalPrescriptionDTO dto);
    MedicalPrescription findById(Long id);
    void delete(Long id);

    // Recherche
    Page<MedicalPrescription> findByPatient(Long patientId, Pageable pageable);
    Page<MedicalPrescription> findByDoctor(Long doctorId, Pageable pageable);
    Page<MedicalPrescription> findByPharmacien(Long pharmacienId, Pageable pageable);
    List<MedicalPrescription> findByDateRange(LocalDate start, LocalDate end);
    Page<MedicalPrescription> findByStatut(PrescriptionStatut statut, Pageable pageable);
    List<MedicalPrescription> findExpired();
    List<MedicalPrescription> findRenewable();

    // Workflow métier
    MedicalPrescription deliver(Long prescriptionId, SaleId saleId);
    MedicalPrescription deliverPartially(Long prescriptionId, SaleId saleId, Map<Long, Integer> lineQuantities);
    MedicalPrescription cancel(Long id, String motif);
    void markAsExpired(Long id);

    // Renouvellement
    boolean canRenew(Long prescriptionId);
    MedicalPrescription renew(Long prescriptionId);
    int getRemainingRenewals(Long prescriptionId);

    // Vérifications pharmaceutiques
    List<String> checkInteractions(Long prescriptionId);
    List<String> checkContraindications(Long prescriptionId, Long patientId);
    boolean requiresSecuredPrescription(Long prescriptionId);
    boolean isExpired(Long prescriptionId);

    // Documents
    String uploadScanDocument(Long prescriptionId, MultipartFile file);
    byte[] downloadScanDocument(Long prescriptionId);
    byte[] generatePrescriptionReceipt(Long prescriptionId); // Reçu de délivrance
    String getElectronicSignature(Long prescriptionId);

    // Statistiques
    Map<String, Long> getPrescriptionStatsByDoctor(Long doctorId, LocalDate start, LocalDate end);
    Map<String, Long> getPrescriptionStatsByPatient(Long patientId);
    List<Produit> getMostPrescribedProducts(LocalDate start, LocalDate end, int limit);
}
```

### DoctorService

```java
public interface DoctorService {

    // CRUD basique
    Doctor create(DoctorDTO dto);
    Doctor update(Long id, DoctorDTO dto);
    Doctor findById(Long id);
    void delete(Long id);

    // Recherche
    Page<Doctor> findAll(Pageable pageable);
    Page<Doctor> findByNom(String nom, Pageable pageable);
    Doctor findByRPPS(String numeroRPPS);
    List<Doctor> findBySpecialite(String specialite);
    List<Doctor> searchDoctors(String query); // Recherche globale

    // Gestion
    Doctor activate(Long id);
    Doctor deactivate(Long id);

    // Statistiques
    Long countPrescriptionsByDoctor(Long doctorId);
    List<Map<String, Object>> getTopPrescribingDoctors(int limit);
}
```

### PrescriptionLineService

```java
public interface PrescriptionLineService {

    // CRUD
    PrescriptionLine create(Long prescriptionId, PrescriptionLineDTO dto);
    PrescriptionLine update(Long id, PrescriptionLineDTO dto);
    void delete(Long id);

    // Gestion
    PrescriptionLine updateQuantityDelivered(Long id, Integer quantityDelivered);
    boolean isFullyDelivered(Long id);
    Integer getRemainingQuantity(Long id);

    // Substitution
    PrescriptionLine substitute(Long id, Long newProduitId, String motif);
    boolean canSubstitute(Long id);
}
```

---

## 🖥️ Frontend Angular

### Composant PrescriptionFormComponent (Enregistrement Ordonnance)

**Features :**
- Formulaire réactif avec validation
- Recherche médecin (autocomplete avec RPPS)
- Ajout rapide nouveau médecin
- Saisie lignes ordonnance avec autocomplete produits
- Upload scan ordonnance (drag & drop)
- Visualiseur PDF intégré
- Contrôle interactions médicamenteuses en temps réel
- Alertes stupéfiants/psychotropes
- Gestion substitution générique

**Structure fichiers :**
```
src/main/webapp/app/entities/prescription/
├── model/
│   ├── medical-prescription.model.ts
│   ├── prescription-line.model.ts
│   └── doctor.model.ts
├── service/
│   ├── medical-prescription.service.ts
│   ├── doctor.service.ts
│   └── prescription-validation.service.ts
├── prescription-list/
│   ├── prescription-list.component.ts
│   ├── prescription-list.component.html
│   └── prescription-list.component.scss
├── prescription-form/
│   ├── prescription-form.component.ts
│   ├── prescription-form.component.html
│   ├── prescription-form.component.scss
│   └── doctor-quick-add-dialog.component.ts
├── prescription-detail/
│   ├── prescription-detail.component.ts
│   ├── prescription-detail.component.html
│   └── prescription-history.component.ts
├── prescription-scan/
│   ├── prescription-scan-upload.component.ts
│   ├── prescription-scan-viewer.component.ts
│   └── prescription-scan.component.scss
└── doctor/
    ├── doctor-list.component.ts
    ├── doctor-form.component.ts
    └── doctor-search.component.ts
```

### Intégration dans le flux de vente

**Scénario 1 : Vente avec ordonnance existante**

```typescript
// Dans sales.component.ts
createSaleFromPrescription(prescription: MedicalPrescription): void {
  // 1. Vérifier ordonnance pas expirée
  if (prescription.statut === 'EXPIREE') {
    this.showError('Ordonnance expirée, délivrance impossible');
    return;
  }

  // 2. Créer vente liée
  const sale: Sales = {
    typePrescription: TypePrescription.PRESCRIPTION,
    customer: prescription.patient,
    prescriptionId: prescription.id, // LIEN IMPORTANT
    // ... autres champs
  };

  // 3. Créer lignes vente depuis lignes ordonnance
  prescription.lines.forEach(line => {
    // Vérifier quantité restante
    if (line.quantiteRestante > 0) {
      const saleLine: SalesLine = {
        produit: line.produitDelivre || line.produitPrescrit,
        quantity: line.quantiteRestante, // ou demander quantité à délivrer
        // ... calcul prix
      };
      sale.salesLines.push(saleLine);
    }
  });

  // 4. Sauvegarder vente
  this.salesService.create(sale).subscribe(createdSale => {
    // 5. Mettre à jour quantités délivrées
    this.prescriptionService.deliver(prescription.id, createdSale.id).subscribe(() => {
      this.showSuccess('Délivrance enregistrée avec succès');
    });
  });
}
```

**Scénario 2 : Enregistrement ordonnance pendant vente**

```typescript
// Dialog pendant le flux de vente
openPrescriptionRegistration(customer: Customer): void {
  const dialogRef = this.modalService.open(PrescriptionFormComponent, {
    size: 'xl',
    backdrop: 'static',
  });

  dialogRef.componentInstance.patient = customer;
  dialogRef.componentInstance.quickMode = true; // Mode rapide

  dialogRef.result.then((prescription: MedicalPrescription) => {
    if (prescription) {
      // Prescription enregistrée, créer vente
      this.createSaleFromPrescription(prescription);
    }
  });
}
```

**Composant recherche ordonnances patient :**

```typescript
// prescription-search.component.ts
searchPrescriptionsByPatient(patientId: number): void {
  this.prescriptionService.findByPatient(patientId).subscribe(prescriptions => {
    this.prescriptions = prescriptions.filter(p =>
      p.statut !== 'DELIVREE_TOTALE' && p.statut !== 'EXPIREE'
    );
    this.showPrescriptionSelector(this.prescriptions);
  });
}
```

---

## 📊 Tableau de Bord & Reporting

### KPIs à implémenter
- Nombre de prescriptions par pharmacien
- Taux de substitution générique
- Temps moyen de traitement prescription
- Alertes interactions non résolues
- Prescriptions expirées non délivrées
- Top 10 médicaments prescrits

### Écrans de reporting
1. **Historique patient** : Toutes prescriptions par patient
2. **Journal pharmacien** : Prescriptions créées par pharmacien
3. **Conformité réglementaire** : Audit trail complet
4. **Analyse produits** : Produits prescrits vs délivrés

---

## 🔐 Sécurité & Conformité

### RGPD
- Consentement patient pour stockage données santé
- Droit à l'oubli (anonymisation après délai légal)
- Chiffrement données sensibles (posologie, motifs)
- Logs d'accès prescription

### Authentification
- Signature électronique pharmacien (certificat)
- Double authentification pour prescriptions sensibles
- Traçabilité complète des modifications

### Conservation
- France : 3 ans minimum (Code Santé Publique)
- Archivage automatique documents scannés
- Sauvegarde quotidienne base données

---

## 🚀 Plan de Déploiement

### Phase 1 : MVP (2-3 semaines)
- [ ] Créer entités `PharmacistPrescription` et `PrescriptionLine`
- [ ] Migration base de données
- [ ] CRUD backend basique
- [ ] Interface création prescription simple
- [ ] Lien prescription → vente

### Phase 2 : Fonctionnalités Métier (3-4 semaines)
- [ ] Validation pharmaceutique
- [ ] Contrôle interactions médicamenteuses (API externe)
- [ ] Gestion prescripteurs externes
- [ ] Upload/scan ordonnances
- [ ] Signature électronique

### Phase 3 : Conformité & Reporting (2-3 semaines)
- [ ] Génération PDF ordonnance
- [ ] Archivage automatique
- [ ] Tableau de bord prescriptions
- [ ] Exports réglementaires
- [ ] Audit trail complet

### Phase 4 : Optimisations (2 semaines)
- [ ] Modèles prescription fréquentes
- [ ] Renouvellement automatique protocoles
- [ ] Alertes expiration
- [ ] Synchronisation dossier pharmaceutique (si France)

---

## 📝 Points d'Attention

### Aspects Réglementaires (France)

#### Conservation Ordonnances
- **Durée légale** : 3 ans minimum (Art. L.5132-9 CSP)
- **Stupéfiants** : 10 ans minimum (Art. R.5132-9 CSP)
- **Support** : Papier OU numérique (scan haute qualité)
- **Archivage** : Classement chronologique ou par patient

#### Validité Ordonnances
- **Ordonnance simple** : 3 mois
- **ALD (Affection Longue Durée)** : 1 an
- **Stupéfiants** :
  - Sécurisée obligatoire (filigrane)
  - 3 jours pour délivrance (sauf exceptions)
  - 7 jours max pour amphétamines
  - 28 jours max pour autres stupéfiants
- **Ordonnance bizone** : 1 an pour partie supérieure

#### Médecin Prescripteur
- **Identification obligatoire** : Nom, RPPS, adresse
- **Vérification RPPS** : Annuaire Santé (https://annuaire.sante.fr)
- **Prescripteurs autorisés** :
  - Médecins (généralistes, spécialistes)
  - Dentistes (produits liés aux soins dentaires)
  - Sages-femmes (femmes enceintes, accouchées, nouveau-nés)
  - Infirmiers IPA (dans cadre protocole)

#### Délivrance
- **Droit de substitution** : Générique sauf mention "non substituable"
- **Fractionnement** : Possible si accord patient
- **Délivrance partielle** : Autorisée, annotation obligatoire
- **Délivrance d'urgence** : Possible sans ordonnance (Décret 2022-706)

#### Dossier Pharmaceutique (DP)
- **Connexion CNOP** : Consultation DP avant délivrance (recommandé)
- **Enregistrement** : Médicaments délivrés sur 4 mois
- **Carte Vitale** : Lecture obligatoire pour DP

#### Télétransmission Sécurité Sociale
- **Ordonnance électronique** : Signature électronique médecin
- **Feuille de soins** : Mention code médecin (RPPS/AM)
- **Facturation** : Code CIP13 produits
- **Tiers-payant** : Lien ordonnance obligatoire

### Technique
- **Performance** : Index sur dates et statuts
- **Volumétrie** : Partitionnement table par année si > 1M prescriptions
- **Concurrent access** : Gestion optimistic locking
- **Backup** : Sauvegarde différentielle quotidienne

### Formation Équipe Officine
- **Guide utilisateur** : Enregistrement ordonnances médicales
- **Vidéo tutorielle** : Scan et saisie ordonnances
- **Procédure** : Vérification RPPS médecins
- **FAQ juridique** : Validité, conservation, délivrance partielle
- **Cas pratiques** : Stupéfiants, ordonnances renouvelables, ALD
- **Formation réglementaire** : Droit de substitution, DP, télétransmission
- **Procédure d'urgence** : Délivrance sans ordonnance, panne système

---

## 📚 Ressources Complémentaires

### API Externes à Intégrer
- **Vidal API** : Interactions médicamenteuses
- **Thériaque** : Base de données médicaments
- **Assurance Maladie API** : Télétransmission
- **CNOP DP** : Dossier Pharmaceutique (si France)

### Librairies Recommandées
- **Backend** :
  - iText 7 (génération PDF ordonnances)
  - BouncyCastle (signature électronique)
  - Apache Tika (analyse documents scannés)

- **Frontend** :
  - ng2-pdf-viewer (visualisation PDF)
  - ngx-scanner (scan ordonnances via webcam)
  - signature_pad (signature électronique patient)

---

## ✅ Checklist de Validation

Avant mise en production :
- [ ] Tests unitaires services (> 80% couverture)
- [ ] Tests intégration flux complet
- [ ] Validation juridique (avocat spécialisé santé)
- [ ] Audit sécurité données santé
- [ ] Formation équipe officine
- [ ] Documentation complète
- [ ] Plan de reprise activité (PRA)
- [ ] Validation par pharmacien responsable

---

**Date** : 2026-01-25
**Version** : 1.0
**Statut** : Proposition stratégique - Nécessite validation métier
