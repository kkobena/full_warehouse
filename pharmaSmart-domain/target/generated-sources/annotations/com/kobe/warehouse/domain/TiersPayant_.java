package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.Periodicite;
import com.kobe.warehouse.domain.enumeration.TiersPayantCategorie;
import com.kobe.warehouse.domain.enumeration.TiersPayantStatut;
import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.TiersPayant}
 **/
@StaticMetamodel(TiersPayant.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class TiersPayant_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #name
	 **/
	public static final String NAME = "name";
	
	/**
	 * @see #fullName
	 **/
	public static final String FULL_NAME = "fullName";
	
	/**
	 * @see #nbreBons
	 **/
	public static final String NBRE_BONS = "nbreBons";
	
	/**
	 * @see #montantMaxParFcture
	 **/
	public static final String MONTANT_MAX_PAR_FCTURE = "montantMaxParFcture";
	
	/**
	 * @see #codeOrganisme
	 **/
	public static final String CODE_ORGANISME = "codeOrganisme";
	
	/**
	 * @see #consoMensuelle
	 **/
	public static final String CONSO_MENSUELLE = "consoMensuelle";
	
	/**
	 * @see #plafondAbsolu
	 **/
	public static final String PLAFOND_ABSOLU = "plafondAbsolu";
	
	/**
	 * @see #adresse
	 **/
	public static final String ADRESSE = "adresse";
	
	/**
	 * @see #telephone
	 **/
	public static final String TELEPHONE = "telephone";
	
	/**
	 * @see #telephoneFixe
	 **/
	public static final String TELEPHONE_FIXE = "telephoneFixe";
	
	/**
	 * @see #email
	 **/
	public static final String EMAIL = "email";
	
	/**
	 * @see #beExclude
	 **/
	public static final String BE_EXCLUDE = "beExclude";
	
	/**
	 * @see #plafondConso
	 **/
	public static final String PLAFOND_CONSO = "plafondConso";
	
	/**
	 * @see #statut
	 **/
	public static final String STATUT = "statut";
	
	/**
	 * @see #categorie
	 **/
	public static final String CATEGORIE = "categorie";
	
	/**
	 * @see #remiseForfaitaire
	 **/
	public static final String REMISE_FORFAITAIRE = "remiseForfaitaire";
	
	/**
	 * @see #nbreBordereaux
	 **/
	public static final String NBRE_BORDEREAUX = "nbreBordereaux";
	
	/**
	 * @see #groupeTiersPayant
	 **/
	public static final String GROUPE_TIERS_PAYANT = "groupeTiersPayant";
	
	/**
	 * @see #created
	 **/
	public static final String CREATED = "created";
	
	/**
	 * @see #updated
	 **/
	public static final String UPDATED = "updated";
	
	/**
	 * @see #user
	 **/
	public static final String USER = "user";
	
	/**
	 * @see #consommations
	 **/
	public static final String CONSOMMATIONS = "consommations";
	
	/**
	 * @see #modelFacture
	 **/
	public static final String MODEL_FACTURE = "modelFacture";
	
	/**
	 * @see #plafondConsoClient
	 **/
	public static final String PLAFOND_CONSO_CLIENT = "plafondConsoClient";
	
	/**
	 * @see #plafondJournalierClient
	 **/
	public static final String PLAFOND_JOURNALIER_CLIENT = "plafondJournalierClient";
	
	/**
	 * @see #plafondAbsoluClient
	 **/
	public static final String PLAFOND_ABSOLU_CLIENT = "plafondAbsoluClient";
	
	/**
	 * @see #ncc
	 **/
	public static final String NCC = "ncc";
	
	/**
	 * @see #delaiReglement
	 **/
	public static final String DELAI_REGLEMENT = "delaiReglement";
	
	/**
	 * @see #periodiciteFactureDefinitive
	 **/
	public static final String PERIODICITE_FACTURE_DEFINITIVE = "periodiciteFactureDefinitive";
	
	/**
	 * @see #periodiciteFactureProvisoire
	 **/
	public static final String PERIODICITE_FACTURE_PROVISOIRE = "periodiciteFactureProvisoire";
	
	/**
	 * @see #inclureFacturationAutoDefinitive
	 **/
	public static final String INCLURE_FACTURATION_AUTO_DEFINITIVE = "inclureFacturationAutoDefinitive";
	
	/**
	 * @see #inclureFacturationAutoProvisoire
	 **/
	public static final String INCLURE_FACTURATION_AUTO_PROVISOIRE = "inclureFacturationAutoProvisoire";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.TiersPayant}
	 **/
	public static volatile EntityType<TiersPayant> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.TiersPayant#id}
	 **/
	public static volatile SingularAttribute<TiersPayant, Integer> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.TiersPayant#name}
	 **/
	public static volatile SingularAttribute<TiersPayant, String> name;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.TiersPayant#fullName}
	 **/
	public static volatile SingularAttribute<TiersPayant, String> fullName;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.TiersPayant#nbreBons}
	 **/
	public static volatile SingularAttribute<TiersPayant, Integer> nbreBons;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.TiersPayant#montantMaxParFcture}
	 **/
	public static volatile SingularAttribute<TiersPayant, Long> montantMaxParFcture;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.TiersPayant#codeOrganisme}
	 **/
	public static volatile SingularAttribute<TiersPayant, String> codeOrganisme;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.TiersPayant#consoMensuelle}
	 **/
	public static volatile SingularAttribute<TiersPayant, Long> consoMensuelle;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.TiersPayant#plafondAbsolu}
	 **/
	public static volatile SingularAttribute<TiersPayant, Boolean> plafondAbsolu;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.TiersPayant#adresse}
	 **/
	public static volatile SingularAttribute<TiersPayant, String> adresse;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.TiersPayant#telephone}
	 **/
	public static volatile SingularAttribute<TiersPayant, String> telephone;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.TiersPayant#telephoneFixe}
	 **/
	public static volatile SingularAttribute<TiersPayant, String> telephoneFixe;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.TiersPayant#email}
	 **/
	public static volatile SingularAttribute<TiersPayant, String> email;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.TiersPayant#beExclude}
	 **/
	public static volatile SingularAttribute<TiersPayant, Boolean> beExclude;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.TiersPayant#plafondConso}
	 **/
	public static volatile SingularAttribute<TiersPayant, Long> plafondConso;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.TiersPayant#statut}
	 **/
	public static volatile SingularAttribute<TiersPayant, TiersPayantStatut> statut;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.TiersPayant#categorie}
	 **/
	public static volatile SingularAttribute<TiersPayant, TiersPayantCategorie> categorie;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.TiersPayant#remiseForfaitaire}
	 **/
	public static volatile SingularAttribute<TiersPayant, Integer> remiseForfaitaire;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.TiersPayant#nbreBordereaux}
	 **/
	public static volatile SingularAttribute<TiersPayant, Integer> nbreBordereaux;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.TiersPayant#groupeTiersPayant}
	 **/
	public static volatile SingularAttribute<TiersPayant, GroupeTiersPayant> groupeTiersPayant;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.TiersPayant#created}
	 **/
	public static volatile SingularAttribute<TiersPayant, LocalDateTime> created;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.TiersPayant#updated}
	 **/
	public static volatile SingularAttribute<TiersPayant, LocalDateTime> updated;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.TiersPayant#user}
	 **/
	public static volatile SingularAttribute<TiersPayant, AppUser> user;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.TiersPayant#consommations}
	 **/
	public static volatile SingularAttribute<TiersPayant, Set> consommations;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.TiersPayant#modelFacture}
	 **/
	public static volatile SingularAttribute<TiersPayant, String> modelFacture;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.TiersPayant#plafondConsoClient}
	 **/
	public static volatile SingularAttribute<TiersPayant, Integer> plafondConsoClient;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.TiersPayant#plafondJournalierClient}
	 **/
	public static volatile SingularAttribute<TiersPayant, Integer> plafondJournalierClient;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.TiersPayant#plafondAbsoluClient}
	 **/
	public static volatile SingularAttribute<TiersPayant, Boolean> plafondAbsoluClient;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.TiersPayant#ncc}
	 **/
	public static volatile SingularAttribute<TiersPayant, String> ncc;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.TiersPayant#delaiReglement}
	 **/
	public static volatile SingularAttribute<TiersPayant, Integer> delaiReglement;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.TiersPayant#periodiciteFactureDefinitive}
	 **/
	public static volatile SingularAttribute<TiersPayant, Periodicite> periodiciteFactureDefinitive;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.TiersPayant#periodiciteFactureProvisoire}
	 **/
	public static volatile SingularAttribute<TiersPayant, Periodicite> periodiciteFactureProvisoire;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.TiersPayant#inclureFacturationAutoDefinitive}
	 **/
	public static volatile SingularAttribute<TiersPayant, Boolean> inclureFacturationAutoDefinitive;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.TiersPayant#inclureFacturationAutoProvisoire}
	 **/
	public static volatile SingularAttribute<TiersPayant, Boolean> inclureFacturationAutoProvisoire;

}

