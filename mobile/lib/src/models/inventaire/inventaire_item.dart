import 'package:hive/hive.dart';
import 'package:json_annotation/json_annotation.dart';

part 'inventaire_item.g.dart';

@HiveType(typeId: 1)
@JsonSerializable()
class InventaireItem extends HiveObject {
  @HiveField(0)
  final int id;
  @HiveField(1)
  final int? quantityOnHand;
  @HiveField(2)
  final int? quantityInit;
  @HiveField(3)
  final int? inventoryValueCost;
  @HiveField(4)
  final int storeInventoryId;
  @HiveField(5)
  final int produitId;
  @HiveField(6)
  final int produitLibelle;
  @HiveField(7)
  final int? inventoryValueTotalCost;
  @HiveField(8)
  final int? inventoryValueAmount;
  @HiveField(9)
  final bool updated;
  @HiveField(10)
  final int? gap;
  @HiveField(11)
  final int prixAchat;
  @HiveField(12)
  final int prixUni;
  @HiveField(13)
  final int produitCip;
  @HiveField(14)
  final int rayonId ; // id rayon


  InventaireItem({
    required this.id,
    this.quantityOnHand,
    this.quantityInit,
    this.inventoryValueCost,
    required this.storeInventoryId,
    required this.produitId,
    required this.produitLibelle,
    this.inventoryValueTotalCost,
    this.inventoryValueAmount,
    required this.updated,
    this.gap,
    required this.prixAchat,
    required this.prixUni,
    required this.produitCip,
    required this.rayonId,
  });

  factory InventaireItem.fromJson(Map<String, dynamic> json) =>
      _$InventaireItemFromJson(json);

  Map<String, dynamic> toJson() => _$InventaireItemToJson(this);
} 
