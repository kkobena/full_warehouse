import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

class Constant {
  static const String appName = 'Pharma Smart';
  static const String appVersion = 'Version 1.0.0';
  static const String profilAdmin = 'mobile_admin';
  static const String profilUser = 'mobile_user';
  static const String tableau = 'Accueil';
  static const String balance = 'Balance';
  static const String stock = 'Stock';
  static const String tva = 'Rapport Tva';
  static const String recapitulatifCaisse = 'Caisse';
  static const String inventaire = 'Inventaire';
  static const String valider = 'VALIDER';
  static const String select = 'SELECTIONNER';
  static const String selectPeriodeText = 'Sélectionnez la période';
  static const String selectPeriodeOptions = 'Sélectionnez les paramètres';
  static const String annuler = 'ANNULER';
  static const String du = 'Du:';
  static const String au = 'Au:';
  static const String userResume = 'Total mobile';
  static const String resume = 'Total général';
  static const String onlyVenteText = 'Ventes uniquement';
  static const String produitPageTitle = 'Produits';
  static const String produitSearchInputPlaceholder =
      'Taper pour rechercher un produit';
  static const String produitSearchInputLabel = 'Rechercher un produit';
  static const String produitDetailText =
      'Sélectionnez un produit pour voir les détails';
  static const String designation = 'Désignation';
  static const String cip = 'Cip';
  static const String rayons = 'Rayons';
  static const String rayon = 'Libellé';
  static const String codeRayon = 'Code rayon';
  static const String prixAchat = 'Prix d\'achat';
  static const String prixVente = 'Prix de vente';
  static const String qteStockTotal = 'Stock total';
  static const String qteReserve = 'Stock Réserve';
  static const String qteReap = 'Qté réappro';
  static const String qteSeuil = 'Qté seuil';
  static const String tvaCode = 'Tva';
  static const String datePeremption = 'Date de péremption';
  static const String fournisseur = 'Libellé';
  static const String fournisseurs = 'Grossistes';
  static const String etatProduit = 'Statut du produit';
  static const String lastDateOfSale = 'Dernière vente';
  static const String lastOrderDate = 'Dernière commande';
  static const String lastInventoryDate = 'Dernier inventaire';
  static const String laboratoire = 'Laboratoire';
  static const String forme = 'Forme';
  static const String gamme = 'Gamme';
  static const String dci = 'DCI';
  static const String storageName = 'Emplacement';
  static const String storageType = 'Type d\'emplacement';
  static const String produitStock = 'Stocks';
  static const String inOrder = 'Commande en cours';
  static const String inSuggestion = 'En suggestion';
  static const String inStock = 'En entrée en stock';
  static const int searchTermLenght = 3;
  static const String hiveInventaireBox = 'inventaires';
  static const String hiveRayonBox = 'rayons';
  static const String hiverayonInventaireItemsBox = 'rayonInventaireItems';
  static DateTime fromDate = DateTime.now();
  static DateTime toDate = DateTime.now();
  static DateTime firstDate = DateTime(2024);
  static DateTime lastDate = DateTime.now();
  static DateFormat datePattern = DateFormat('yyyy-MM-dd');
  static DateFormat datePatternFr = DateFormat('dd/MM/yyyy');
  static RoundedRectangleBorder roundedRectangleBorder =
      const RoundedRectangleBorder(
        borderRadius: BorderRadius.all(Radius.circular(8)),
      );

  static Divider getDivider(BuildContext context) {
    return Divider(
      height: 1,
      color: Theme.of(context).dividerColor.withValues(alpha: 0.5),
    );
  }

  static Color getCircleAvatarForegroundColor(BuildContext context) {

 return   Theme.of(
      context,
    ).colorScheme.primary;
  }
  static Color getCircleAvatarBackgroundColor(BuildContext context) {
    final Color baseColor = Theme.of(context).colorScheme.primaryContainer;
    return baseColor.withValues(alpha: 0.3);
  }

  static const List<Color> metterGroupColors = [
    Color(0xFFFACC13),
    Color(0xFF6466F1),
    Color(0xFF39BDF8),
    Color(0xFFA855F7),
    Color(0xFF17B8A6),
    Color(0xFF22C45F),
    Color(0xFFF97316),
    Color(0xFFEAB30C),
    Color(0xFFEC4899),
    Color(0xFF10B981), // Emerald
    Color(0xFF8B5CF6), // Violet
    Color(0xFF0EA5E9), // Sky Blue
    Color(0xFFEF4444), // Red
    Color(0xFF14B8A6), // Cyan
    Color(0xFF7C3AED),
    Color(0xFFA78BF9),
    // Teal
  ];
  static const pieChartColors = [
    Color(0xFF39BDF8),
    Color(0xFF6466F1),
    Color(0xFF17B8A6),
    Color(0xFFFACC13),
    Color(0xFFA855F7),
    Color(0xFFEC4899),
  ];

  static String formatNumber(num? value) {
    if (value == null) {
      return '';
    }
    return NumberFormat(
      '#,##0',
      'fr_FR',
    ).format(value).replaceAll(',', '\u00A0');
  }

  static String getFirstLetter(String text) {
    if (text.isEmpty) {
      return text;
    }
    return text[0].toUpperCase();
  }

}
