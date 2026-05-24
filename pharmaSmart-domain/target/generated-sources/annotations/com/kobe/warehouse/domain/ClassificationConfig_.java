package com.kobe.warehouse.domain;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.LocalDateTime;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.ClassificationConfig}
 **/
@StaticMetamodel(ClassificationConfig.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class ClassificationConfig_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #seuilParetoAPlus
	 **/
	public static final String SEUIL_PARETO_APLUS = "seuilParetoAPlus";
	
	/**
	 * @see #seuilParetoA
	 **/
	public static final String SEUIL_PARETO_A = "seuilParetoA";
	
	/**
	 * @see #seuilParetoB
	 **/
	public static final String SEUIL_PARETO_B = "seuilParetoB";
	
	/**
	 * @see #seuilParetoC
	 **/
	public static final String SEUIL_PARETO_C = "seuilParetoC";
	
	/**
	 * @see #seuilFrequenceMinMois
	 **/
	public static final String SEUIL_FREQUENCE_MIN_MOIS = "seuilFrequenceMinMois";
	
	/**
	 * @see #cmmSeuilAPlus
	 **/
	public static final String CMM_SEUIL_APLUS = "cmmSeuilAPlus";
	
	/**
	 * @see #cmmSeuilA
	 **/
	public static final String CMM_SEUIL_A = "cmmSeuilA";
	
	/**
	 * @see #cmmSeuilB
	 **/
	public static final String CMM_SEUIL_B = "cmmSeuilB";
	
	/**
	 * @see #cmmSeuilC
	 **/
	public static final String CMM_SEUIL_C = "cmmSeuilC";
	
	/**
	 * @see #changementMinPourcentage
	 **/
	public static final String CHANGEMENT_MIN_POURCENTAGE = "changementMinPourcentage";
	
	/**
	 * @see #activerClassificationOrdo
	 **/
	public static final String ACTIVER_CLASSIFICATION_ORDO = "activerClassificationOrdo";
	
	/**
	 * @see #activerCorrectionSaisonniere
	 **/
	public static final String ACTIVER_CORRECTION_SAISONNIERE = "activerCorrectionSaisonniere";
	
	/**
	 * @see #indiceSaisonnaliteMin
	 **/
	public static final String INDICE_SAISONNALITE_MIN = "indiceSaisonnaliteMin";
	
	/**
	 * @see #nbMoisSaisonAnalyse
	 **/
	public static final String NB_MOIS_SAISON_ANALYSE = "nbMoisSaisonAnalyse";
	
	/**
	 * @see #nbMoisMinNouveauProduit
	 **/
	public static final String NB_MOIS_MIN_NOUVEAU_PRODUIT = "nbMoisMinNouveauProduit";
	
	/**
	 * @see #autoClassificationEnabled
	 **/
	public static final String AUTO_CLASSIFICATION_ENABLED = "autoClassificationEnabled";
	
	/**
	 * @see #updatedAt
	 **/
	public static final String UPDATED_AT = "updatedAt";
	
	/**
	 * @see #updatedBy
	 **/
	public static final String UPDATED_BY = "updatedBy";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.ClassificationConfig}
	 **/
	public static volatile EntityType<ClassificationConfig> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ClassificationConfig#id}
	 **/
	public static volatile SingularAttribute<ClassificationConfig, Integer> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ClassificationConfig#seuilParetoAPlus}
	 **/
	public static volatile SingularAttribute<ClassificationConfig, Integer> seuilParetoAPlus;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ClassificationConfig#seuilParetoA}
	 **/
	public static volatile SingularAttribute<ClassificationConfig, Integer> seuilParetoA;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ClassificationConfig#seuilParetoB}
	 **/
	public static volatile SingularAttribute<ClassificationConfig, Integer> seuilParetoB;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ClassificationConfig#seuilParetoC}
	 **/
	public static volatile SingularAttribute<ClassificationConfig, Integer> seuilParetoC;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ClassificationConfig#seuilFrequenceMinMois}
	 **/
	public static volatile SingularAttribute<ClassificationConfig, Integer> seuilFrequenceMinMois;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ClassificationConfig#cmmSeuilAPlus}
	 **/
	public static volatile SingularAttribute<ClassificationConfig, Integer> cmmSeuilAPlus;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ClassificationConfig#cmmSeuilA}
	 **/
	public static volatile SingularAttribute<ClassificationConfig, Integer> cmmSeuilA;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ClassificationConfig#cmmSeuilB}
	 **/
	public static volatile SingularAttribute<ClassificationConfig, Integer> cmmSeuilB;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ClassificationConfig#cmmSeuilC}
	 **/
	public static volatile SingularAttribute<ClassificationConfig, Integer> cmmSeuilC;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ClassificationConfig#changementMinPourcentage}
	 **/
	public static volatile SingularAttribute<ClassificationConfig, Integer> changementMinPourcentage;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ClassificationConfig#activerClassificationOrdo}
	 **/
	public static volatile SingularAttribute<ClassificationConfig, Boolean> activerClassificationOrdo;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ClassificationConfig#activerCorrectionSaisonniere}
	 **/
	public static volatile SingularAttribute<ClassificationConfig, Boolean> activerCorrectionSaisonniere;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ClassificationConfig#indiceSaisonnaliteMin}
	 **/
	public static volatile SingularAttribute<ClassificationConfig, Integer> indiceSaisonnaliteMin;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ClassificationConfig#nbMoisSaisonAnalyse}
	 **/
	public static volatile SingularAttribute<ClassificationConfig, Integer> nbMoisSaisonAnalyse;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ClassificationConfig#nbMoisMinNouveauProduit}
	 **/
	public static volatile SingularAttribute<ClassificationConfig, Integer> nbMoisMinNouveauProduit;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ClassificationConfig#autoClassificationEnabled}
	 **/
	public static volatile SingularAttribute<ClassificationConfig, Boolean> autoClassificationEnabled;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ClassificationConfig#updatedAt}
	 **/
	public static volatile SingularAttribute<ClassificationConfig, LocalDateTime> updatedAt;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ClassificationConfig#updatedBy}
	 **/
	public static volatile SingularAttribute<ClassificationConfig, AppUser> updatedBy;

}

