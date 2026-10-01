-- « pi-file-minus » n'existe pas dans PrimeIcons 8 : le lien Avoirs s'affichait sans icône.
-- Conditionnel pour ne pas écraser une icône choisie depuis l'écran de gestion des menus.
UPDATE nav_item SET icon = 'pi pi-wallet' WHERE code = 'facturation.avoirs' AND icon = 'pi pi-file-minus';
