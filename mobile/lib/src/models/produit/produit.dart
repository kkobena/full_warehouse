
import 'package:json_annotation/json_annotation.dart';
import 'package:mobile/src/models/produit/etat_produit.dart';
import 'package:mobile/src/models/produit/produit_fournisseur.dart';
import 'package:mobile/src/models/produit/produit_stock.dart';
import 'package:mobile/src/models/produit/rayon_produit.dart';
import 'package:mobile/src/utils/constant.dart';

part 'produit.g.dart';

@JsonSerializable()
class Produit {
  final int id;
  final String libelle;
  final String? codeCip;
  final String? rayonLibelle;
  final EtatProduit etatProduit;
  final int? totalQuantity;
  final int? qtyReserve;
  final int? tvaTaux;
  final int? regularUnitPrice;
  final int? costAmount;
  final int? qtyAppro;
  final int? qtySeuilMini;
  final List<ProduitFournisseur>? fournisseurProduits;
  final List<ProduitStock>? stockProduits;
  final List<RayonProduit>? rayonProduits;
  final DateTime? lastDateOfSale;
  final DateTime? lastOrderDate;
  final DateTime? lastInventoryDate;
  final String? laboratoireLibelle;
  final String? formeLibelle;
  final String? gammeLibelle;
  final String? dciLibelle;
  final String? perimeAt;

  Produit({
    required this.id,
    required this.libelle,
    required this.etatProduit,
    this.codeCip,
    this.rayonLibelle,
    this.totalQuantity,
    this.qtyReserve,
    this.tvaTaux,
    this.regularUnitPrice,
    this.costAmount,
    this.qtyAppro,
    this.qtySeuilMini,
    this.fournisseurProduits,
    this.stockProduits,
    this.lastDateOfSale,
    this.lastOrderDate,
    this.lastInventoryDate,
    this.laboratoireLibelle,
    this.formeLibelle,
    this.gammeLibelle,
    this.dciLibelle,
    this.perimeAt,
    this.rayonProduits,
  });

  factory Produit.fromJson(Map<String, dynamic> json) =>
      _$ProduitFromJson(json);

  Map<String, dynamic> toJson() => _$ProduitToJson(this);

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is Produit &&
          runtimeType == other.runtimeType &&
          id == other.id &&
          libelle == other.libelle;

  @override
  int get hashCode => id.hashCode ^ libelle.hashCode;

  String get lastDateOfSaleFormatted {
    if (lastDateOfSale == null) return '';
    return Constant.datePatternFr.format(lastDateOfSale!);
  }

  String get lastOrderDateFormatted {
    if (lastOrderDate == null) return '';
    return Constant.datePatternFr.format(lastOrderDate!);
  }

  String get lastInventoryDateFormatted {
    if (lastInventoryDate == null) return '';
    return Constant.datePatternFr.format(lastInventoryDate!);
  }

  String get perimeAtFormatted {
    if (perimeAt == null) return '';
    return Constant.datePatternFr.format(DateTime.parse(perimeAt!));
  }


}
