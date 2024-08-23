@color 2
@echo EXPORTING LABOREX REFERENTIAL DATA
@echo **********************************************

if "%1"=="" (
    set /p username=Entrer le nom d'utilisateur de la base de donnees:
   ) else (
    set username=%1
   )
if "%2"=="" (
    set /p password=Entrer le mot de passe de la base de donnees:
   ) else (
    set password=%2
    )
if "%3"=="" (
    set /p databaseName=Entrer le nom de la base de donnees:
   ) else (
    set databaseName=%3
    )


set currentDate=%date%
set currentTime=%time%
set currentTime=%currentTime::=_%
set currentDate=%currentDate:/=_%
set currentDate=%currentDate: =%
set currentTime=%currentTime: =%
set currentTime=%currentTime:,=_%
set currentDateTime=%currentDate%_%currentTime%
echo %currentDateTime%
set currentDir=%CD%
set laborex_clients=%currentDir%\scripts\clients\laborex_clients_%currentDateTime%.txt
set laborex_clients=%laborex_clients:\=/%
set laborex_tiers_payants=%currentDir%\scripts\tiers_payants\laborex_tiers_payants_%currentDateTime%.txt
set laborex_tiers_payants=%laborex_tiers_payants:\=/%
set laborex_clent_tiers_payants=%currentDir%\scripts\compte_clients_tiers_payants\laborex_clients_tiers_payants_%currentDateTime%.txt
set laborex_clent_tiers_payants=%laborex_clent_tiers_payants:\=/%
set laborex_ayant_droits=%currentDir%\scripts\ayant_droits\laborex_ayant_droits_%currentDateTime%.txt
set laborex_ayant_droits=%laborex_ayant_droits:\=/%
set laborex_produits=%currentDir%\scripts\produits\laborex_produits_%currentDateTime%.txt
set laborex_produits_details=%currentDir%\scripts\produits\details\laborex_produits_details_%currentDateTime%.txt
set laborex_produits_details=%laborex_produits_details:\=/%
set laborex_produits=%laborex_produits:\=/%
set laborex_rayons=%currentDir%\scripts\rayons\laborex_rayons_%currentDateTime%.txt
set laborex_rayons=%laborex_rayons:\=/%
set emplacements=%currentDir%\scripts\emplacements\laborex_emplacements_%currentDateTime%.txt
set emplacements=%emplacements:\=/%
set officine=%currentDir%\scripts\officine\laborex_officine_%currentDateTime%.txt
set officine=%officine:\=/%
set laborex_gamme_produit=%currentDir%\scripts\gammes\laborex_gamme_produit_%currentDateTime%.txt
set laborex_gamme_produit=%laborex_gamme_produit:\=/%
set laborex_laboratoire=%currentDir%\scripts\laboratoires\laborex_laboratoire_%currentDateTime%.txt
set laborex_laboratoire=%laborex_laboratoire:\=/%

SET client_query=select c.lg_CLIENT_ID as id, c.str_FIRST_NAME as firstName,c.str_LAST_NAME as lastName,c.str_SEXE  as sexe,c.dt_NAISSANCE  asdatNaiss,c.lg_TYPE_CLIENT_ID as typeClientId,c.str_ADRESSE as phone,c.str_NUMERO_SECURITE_SOCIAL as numSecu ^
INTO OUTFILE '%laborex_clients%' FIELDS TERMINATED BY ';' LINES TERMINATED BY '\n' ^
from t_client c where str_STATUT='enable' ;

set tiers_payants_query=SELECT p.lg_TIERS_PAYANT_ID id, p.str_NAME  as name,p.str_FULLNAME as fullName,p.str_CODE_ORGANISME as codeOrganisme ^
,p.int_NBREBONS as nbreBordereaux,p.is_cmus as isCmus,p.str_TELEPHONE as telephoneFixe, p.str_MOBILE as telephone,p.str_ADRESSE as adresse,p.dbl_REMISE_FORFETAIRE as remiseForfaitaire,gp.str_LIBELLE  asgroupeTiersPayantName, ^
 tp.str_LIBELLE_TYPE_TIERS_PAYANT as typeTiersPayantName,p.str_CODE_REGROUPEMENT as codeRegroupement,p.to_be_exclude as toBeExclude,^
 p.dbl_PLAFOND_CREDIT as plafondCredit,mf.str_VALUE as modelFactureName,p.int_MONTANTFAC as montantMaxParFcture,^
 p.is_depot as isdepot,p.grouping_by_taux as groupingByTaux,p.db_CONSOMMATION_MENSUELLE as consommationMensuelle, p.bool_IsACCOUNT as isAccount INTO OUTFILE '%laborex_tiers_payants%' FIELDS TERMINATED BY ';' LINES TERMINATED BY '\n' ^
 FROM  t_tiers_payant p  left join t_groupe_tierspayant gp ON  p.lg_GROUPE_ID=gp.lg_GROUPE_ID left join t_type_tiers_payant tp on p.lg_TYPE_TIERS_PAYANT_ID = tp.lg_TYPE_TIERS_PAYANT_ID ^
 left join t_model_facture mf on p.lg_MODEL_FACTURE_ID = mf.lg_MODEL_FACTURE_ID where p.str_STATUT='enable' ;

set clent_tiers_payants_query=SELECT cp.lg_COMPTE_CLIENT_TIERS_PAYANT_ID as id, cl.lg_CLIENT_ID as clientId,tp.lg_TIERS_PAYANT_ID as tiersPayantId,cp.str_NUMERO_SECURITE_SOCIAL as numSecu ^
,cp.b_IS_RO as ro,cp.int_PRIORITY as priority,cp.int_POURCENTAGE as taux,cp.db_PLAFOND_ENCOURS as plafondEncours,cp.dbl_PLAFOND as plafond,cp.dbl_QUOTA_CONSO_VENTE as planfondVente,cp.b_IsAbsolute as plafondIsAbsolute ^
INTO OUTFILE '%laborex_clent_tiers_payants%' FIELDS TERMINATED BY ';' LINES TERMINATED BY '\n' FROM t_compte_client_tiers_payant cp join t_compte_client c on cp.lg_COMPTE_CLIENT_ID = c.lg_COMPTE_CLIENT_ID JOIN  t_tiers_payant tp on cp.lg_TIERS_PAYANT_ID = tp.lg_TIERS_PAYANT_ID ^
JOIN t_client cl on c.lg_CLIENT_ID = cl.lg_CLIENT_ID WHERE cp.str_STATUT='enable' ;

set ayant_droits_query=SELECT a.lg_AYANTS_DROITS_ID as id,a.lg_CLIENT_ID as idClient,a.str_FIRST_NAME as firstName,a.str_LAST_NAME as lastName,a.str_SEXE as sexe,a.dt_NAISSANCE as datNaiss ^
INTO OUTFILE '%laborex_ayant_droits%' FIELDS TERMINATED BY ';' LINES TERMINATED BY '\n' FROM t_ayant_droit a WHERE a.str_STATUT='enable'  AND a.lg_AYANTS_DROITS_ID != a.lg_CLIENT_ID;

set officine_scripts=SELECT o.str_NOM_ABREGE as name, o.str_NOM_COMPLET as fullName, o.str_ADRESSSE_POSTALE as address, o.str_PHONE as phone,^
o.str_COMPTE_BANCAIRE as compteBancaire, o.str_REGISTRE_IMPOSITION as registreImposition,o.str_REGISTRE_COMMERCE as registre, o.str_COMMENTAIRE1 as note,^
o.str_ENTETE  as welcomeMessage INTO OUTFILE '%officine%' FIELDS TERMINATED BY ';' LINES TERMINATED BY '\n' FROM t_officine o where o.lg_OFFICINE_ID='1';

set emplacement_scripts=SELECT e.str_NAME as name,e.str_PHONE as phone INTO OUTFILE '%emplacements%' FIELDS TERMINATED BY ';' LINES TERMINATED BY '\n' FROM t_emplacement e where e.lg_EMPLACEMENT_ID !='1';

set produits_scripts=SELECT em.str_NAME as emplacement, f.lg_FAMILLE_ID AS id,f.str_NAME as libelle,st.int_NUMBER_AVAILABLE as quantity,st.int_UG as qtyUG,^
f.int_CIP as cip, f.int_EAN13 as codeEan,f.int_PRICE as regularUnitPrice,f.int_PAF as prixachat,f.int_SEUIL_MIN as seuilMin,f.int_QTE_REAPPROVISIONNEMENT as qteReapprovisionnement,f.int_SEUIL_MAX as seuilMax,^
DATE(f.dt_PEREMPTION) as datePeremption,f.bool_ACCOUNT as chiffre,f.cmu_price as prixCmu,f.is_scheduled as isScheduled,f.int_T as tableau,f.bool_DECONDITIONNE as deconditionne,^
f.int_NUMBERDETAIL as itemQty,f.lg_FAMILLE_PARENT_ID as familleParentId,f.bool_CHECKEXPIRATIONDATE as checkExpiryDate,rayon.str_CODE as rayonCode,rayon.str_LIBELLEE as rayonLibelle,^
fa.str_LIBELLE  as familleArticle, fm.str_LIBELLE as formeArticle,gp.libelle as gamme,l.libelle as laboratoire,^
tva.int_VALUE as tvaValue,te.str_NAME as typeEtiquette,f.bool_DECONDITIONNE_EXIST as deconditionneExist INTO OUTFILE '%laborex_produits%' FIELDS TERMINATED BY ';' LINES TERMINATED BY '\n' ^
FROM  t_famille f  join t_famille_stock st on f.lg_FAMILLE_ID = st.lg_FAMILLE_ID LEFT JOIN  t_zone_geographique rayon ON f.lg_ZONE_GEO_ID = rayon.lg_ZONE_GEO_ID ^
JOIN t_famillearticle fa on f.lg_FAMILLEARTICLE_ID = fa.lg_FAMILLEARTICLE_ID LEFT JOIN t_forme_article fm  on f.lg_FORME_ID = fm.lg_FORME_ARTICLE_ID ^
LEFT JOIN  gamme_produit gp on f.gamme_id = gp.id LEFT JOIN laboratoire l on f.laboratoire_id = l.id ^
JOIN t_code_tva as tva on f.lg_CODE_TVA_ID = tva.lg_CODE_TVA_ID JOIN t_typeetiquette te on f.lg_TYPEETIQUETTE_ID = te.lg_TYPEETIQUETTE_ID ^
JOIN t_emplacement em on st.lg_EMPLACEMENT_ID = em.lg_EMPLACEMENT_ID WHERE  f.str_STATUT='enable' AND st.str_STATUT='enable' AND f.bool_DECONDITIONNE =0 ORDER BY f.str_NAME ;

set produits_details_scripts=SELECT em.str_NAME as emplacement, f.lg_FAMILLE_ID AS id,f.str_NAME as libelle,st.int_NUMBER_AVAILABLE as quantity,st.int_UG as qtyUG,^
f.int_CIP as cip, f.int_EAN13 as codeEan,f.int_PRICE as regularUnitPrice,f.int_PAF as prixachat,f.int_SEUIL_MIN as seuilMin,f.int_QTE_REAPPROVISIONNEMENT as qteReapprovisionnement,f.int_SEUIL_MAX as seuilMax,^
DATE(f.dt_PEREMPTION) as datePeremption,f.bool_ACCOUNT as chiffre,f.cmu_price as prixCmu,f.is_scheduled as isScheduled,f.int_T as tableau,f.bool_DECONDITIONNE as deconditionne,^
f.int_NUMBERDETAIL as itemQty,f.lg_FAMILLE_PARENT_ID as familleParentId,f.bool_CHECKEXPIRATIONDATE as checkExpiryDate,rayon.str_CODE as rayonCode,rayon.str_LIBELLEE as rayonLibelle,^
fa.str_LIBELLE  as familleArticle, fm.str_LIBELLE as formeArticle,gp.libelle as gamme,l.libelle as laboratoire,^
tva.int_VALUE as tvaValue,te.str_NAME as typeEtiquette,f.bool_DECONDITIONNE_EXIST as deconditionneExist INTO OUTFILE '%laborex_produits_details%' FIELDS TERMINATED BY ';' LINES TERMINATED BY '\n' ^
FROM  t_famille f  join t_famille_stock st on f.lg_FAMILLE_ID = st.lg_FAMILLE_ID LEFT JOIN  t_zone_geographique rayon ON f.lg_ZONE_GEO_ID = rayon.lg_ZONE_GEO_ID ^
JOIN t_famillearticle fa on f.lg_FAMILLEARTICLE_ID = fa.lg_FAMILLEARTICLE_ID LEFT JOIN t_forme_article fm  on f.lg_FORME_ID = fm.lg_FORME_ARTICLE_ID ^
LEFT JOIN  gamme_produit gp on f.gamme_id = gp.id LEFT JOIN laboratoire l on f.laboratoire_id = l.id ^
JOIN t_code_tva as tva on f.lg_CODE_TVA_ID = tva.lg_CODE_TVA_ID JOIN t_typeetiquette te on f.lg_TYPEETIQUETTE_ID = te.lg_TYPEETIQUETTE_ID ^
JOIN t_emplacement em on st.lg_EMPLACEMENT_ID = em.lg_EMPLACEMENT_ID WHERE  f.str_STATUT='enable' AND st.str_STATUT='enable' AND f.bool_DECONDITIONNE =1 ORDER BY f.str_NAME ;

set rayons_scripts=SELECT r.str_CODE as code, r.str_LIBELLEE as libelle, r.bool_ACCOUNT as takeAccount INTO OUTFILE '%laborex_rayons%' FIELDS TERMINATED BY ';' LINES TERMINATED BY '\n' FROM t_zone_geographique r where str_STATUT='enable' ;

set gamme_scripts=SELECT  g.libelle INTO OUTFILE '%laborex_gamme_produit%' FIELDS TERMINATED BY ';' LINES TERMINATED BY '\n' FROM gamme_produit g where g.status='ENABLE';
set laboratoire_scripts=SELECT l.libelle INTO OUTFILE '%laborex_laboratoire%' FIELDS TERMINATED BY ';' LINES TERMINATED BY '\n' FROM laboratoire l where l.status='ENABLE';

set all_scripts=%client_query% %tiers_payants_query% %clent_tiers_payants_query% %ayant_droits_query% %officine_scripts% %emplacement_scripts% %produits_scripts% %produits_details_scripts% %rayons_scripts% %gamme_scripts% %laboratoire_scripts%


mysql -h localhost -u %username% -p%password%  %databaseName%  -e "%all_scripts%"


@pause