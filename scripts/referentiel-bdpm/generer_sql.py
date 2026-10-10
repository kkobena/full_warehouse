#!/usr/bin/env python3
"""Génère les scripts SQL de chargement du référentiel BDPM (tables ref_*, migration V2.1.19).

    python generer_sql.py                       # lit sortie-gpc/referentiel_bdpm.csv, écrit scripts/sql/
    python generer_sql.py --csv autre.csv --sortie sql

Les fichiers produits sont rejouables et s'exécutent depuis scripts/sql :
    psql -U pharma_smart -d <base> -v ON_ERROR_STOP=1 -f run_all.sql
Python 3.10+, bibliothèque standard seule.
"""
import argparse
import csv
import re
from pathlib import Path

ICI = Path(__file__).parent
LOT = 500

TYPES = {
    'princeps': 'PRINCEPS',
    'générique': 'GENERIQUE',
    'générique substituable': 'GENERIQUE_SUBSTITUABLE',
    'générique par complémentarité posologique': 'GENERIQUE_COMPLEMENTARITE_POSOLOGIQUE',
}
# Nombre + une unité simple : le reste (homéopathie, « qsp »…) reste en texte.
DOSAGE = re.compile(
    r'^(\d+(?:[   ]\d{3})*(?:[.,]\d+)?)\s*(mg|g|microgrammes?|µg|UI|U\.I\.|ml|mL|l|mmol|MBq|%|kg|cm|mm)$')
UNITES = {'U.I.': 'UI', 'µg': 'microgrammes', 'microgramme': 'microgrammes', 'ml': 'mL'}

ENTETE = """\\encoding UTF8
-- Généré par generer_sql.py — ne pas modifier à la main.
\\if :{?schema}
\\else
  \\set schema pharma_smart
\\endif
SET search_path TO :"schema";

"""

COLONNES_COMPOSITION = ['cis', 'substance_code', 'dci_libelle', 'nature', 'dosage_texte',
                        'dosage_valeur', 'dosage_unite', 'reference_dosage']
# La DCI est résolue par libellé, pour ne pas dépendre des identifiants de ref_dci.
INSERT_COMPOSITION = (
    'INSERT INTO ref_specialite_composition '
    '(cis, substance_code, dci_id, nature, dosage_texte, dosage_valeur, dosage_unite, reference_dosage)\n'
    'SELECT v.cis, v.substance_code, d.id, v.nature, v.dosage_texte, v.dosage_valeur::numeric, '
    'v.dosage_unite, v.reference_dosage\n'
    'FROM (VALUES\n'
)
SUITE_COMPOSITION = (
    '\n) AS v(' + ', '.join(COLONNES_COMPOSITION) + ')\n'
    'LEFT JOIN ref_dci d ON d.libelle = v.dci_libelle;\n\n'
)


def litteral_sql(v):
    """Littéral SQL : NULL si vide, apostrophes doublées (standard_conforming_strings)."""
    v = (v or '').strip()
    return 'NULL' if not v else "'" + v.replace("'", "''") + "'"


def extraire_dosage(texte):
    m = DOSAGE.match((texte or '').strip())
    if not m:
        return 'NULL', 'NULL'
    valeur = re.sub(r'[   ]', '', m.group(1)).replace(',', '.')
    return valeur, litteral_sql(UNITES.get(m.group(2), m.group(2)))


def construire_conflit_maj(cle, colonnes):
    return f'ON CONFLICT ({cle}) DO UPDATE SET ' + ', '.join(f'{c} = EXCLUDED.{c}' for c in colonnes)


def ecrire(chemin, table, lignes, *, colonnes=None, conflit='', avant='', entete=None, suite=None):
    """Écrit des INSERT multi-lignes par lots de LOT. `entete`/`suite` encadrent le VALUES si besoin."""
    lignes = list(lignes)
    entete = entete or f'INSERT INTO {table} ({", ".join(colonnes)}) VALUES\n'
    suite = suite or f'\n{conflit};\n\n'
    with open(chemin, 'w', encoding='utf-8', newline='\n') as f:
        f.write(ENTETE)
        f.write(f'-- {len(lignes)} lignes -> {table}\n{avant}')
        for i in range(0, len(lignes), LOT):
            f.write(entete)
            f.write(',\n'.join('    (' + ', '.join(l) + ')' for l in lignes[i:i + LOT]))
            f.write(suite)
    print(f'{chemin.name}: {len(lignes)} lignes')


def main():
    p = argparse.ArgumentParser()
    p.add_argument('--csv', default=str(ICI / 'sortie-gpc' / 'referentiel_bdpm.csv'))
    p.add_argument('--sortie', default=str(ICI.parent / 'sql'))
    a = p.parse_args()
    sortie = Path(a.sortie)
    sortie.mkdir(parents=True, exist_ok=True)

    with open(a.csv, encoding='utf-8-sig', newline='') as f:
        lignes = list(csv.DictReader(f, delimiter=';'))

    groupes, substances, dcis, specialites, rcp, compositions = {}, {}, set(), {}, {}, {}
    for r in lignes:
        cis = r['cis']
        if r['groupe_generique_id']:
            groupes[int(r['groupe_generique_id'])] = r['groupe_generique']
        specialites.setdefault(cis, r)
        if r['indications'] or r['posologie'] or r['contre_indications']:
            rcp.setdefault(cis, r)
        if not r['code_substance']:  # spécialité sans composition connue
            continue
        substances.setdefault(r['code_substance'], r['substance'])
        if r['dci']:
            dcis.add(r['dci'])
        compositions.setdefault(
            (cis, r['code_substance'], r['nature_sa_ft'], r['dosage'], r['reference_dosage']), r)

    ecrire(sortie / '01_ref_groupe_generique.sql', 'ref_groupe_generique',
           ([str(k), litteral_sql(v)] for k, v in sorted(groupes.items())),
           colonnes=['id', 'libelle'], conflit=construire_conflit_maj('id', ['libelle']))

    ecrire(sortie / '02_ref_substance.sql', 'ref_substance',
           ([litteral_sql(k), litteral_sql(v)] for k, v in sorted(substances.items())),
           colonnes=['code', 'libelle'], conflit=construire_conflit_maj('code', ['libelle']))

    ecrire(sortie / '03_ref_dci.sql', 'ref_dci', ([litteral_sql(d)] for d in sorted(dcis)),
           colonnes=['libelle'], conflit='ON CONFLICT (libelle) DO NOTHING')

    cols_spec = ['cis', 'libelle', 'forme', 'voies', 'titulaire', 'commercialisee', 'composition',
                 'groupe_generique_id', 'type_generique', 'princeps_du_groupe']

    def ligne_specialite(r):
        return [litteral_sql(r['cis']), litteral_sql(r['libelle_commercial']), litteral_sql(r['forme']), litteral_sql(r['voies']), litteral_sql(r['titulaire']),
                'TRUE' if r['commercialisation'] == 'Commercialisée' else 'FALSE',
                litteral_sql(r['composition']), r['groupe_generique_id'] or 'NULL',
                litteral_sql(TYPES.get(r['princeps_ou_generique'])), litteral_sql(r['princeps_du_groupe'])]

    ecrire(sortie / '04_ref_specialite.sql', 'ref_specialite',
           (ligne_specialite(r) for _, r in sorted(specialites.items())),
           colonnes=cols_spec, conflit=construire_conflit_maj('cis', cols_spec[1:]))

    def ligne_composition(r):
        valeur, unite = extraire_dosage(r['dosage'])
        return [litteral_sql(r['cis']), litteral_sql(r['code_substance']), litteral_sql(r['dci']), litteral_sql(r['nature_sa_ft']),
                litteral_sql(r['dosage']), valeur, unite, litteral_sql(r['reference_dosage'])]

    comps = sorted((r for r in compositions.values() if r['nature_sa_ft'] in ('SA', 'FT')),
                   key=lambda r: (r['cis'], r['nature_sa_ft'] != 'FT', r['code_substance']))
    # Pas de clé naturelle unique : un rechargement vide la table avant de la remplir.
    ecrire(sortie / '05_ref_specialite_composition.sql', 'ref_specialite_composition',
           (ligne_composition(r) for r in comps),
           avant='DELETE FROM ref_specialite_composition;\n\n',
           entete=INSERT_COMPOSITION, suite=SUITE_COMPOSITION)

    cols_rcp = ['cis', 'indications', 'posologie', 'contre_indications']
    ecrire(sortie / '06_ref_specialite_rcp.sql', 'ref_specialite_rcp',
           ([litteral_sql(r['cis']), litteral_sql(r['indications']), litteral_sql(r['posologie']), litteral_sql(r['contre_indications'])]
            for _, r in sorted(rcp.items())),
           colonnes=cols_rcp, conflit=construire_conflit_maj('cis', cols_rcp[1:]))


if __name__ == '__main__':
    main()
