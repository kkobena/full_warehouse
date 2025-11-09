import 'package:hive/hive.dart';
import 'package:json_annotation/json_annotation.dart';

part 'inventaire.g.dart';

@HiveType(typeId: 3)
@JsonSerializable()
class Inventaire extends HiveObject {
  @HiveField(0)
  final int? id;
  @HiveField(1)
  final String description;
  @HiveField(2)
  final int? inventoryValueCostBegin;
  @HiveField(3)
  final int? inventoryAmountBegin;
  @HiveField(4)
  final int? inventoryValueCostAfter;
  @HiveField(5)
  final int? inventoryAmountAfter;
  @HiveField(6)
  final int? gapCost;
  @HiveField(7)
  final int? gapAmount;
  @HiveField(8)
  final String? statut;
  @HiveField(9)
  final String? inventoryType;
  @HiveField(10)
  final String? inventoryCategory;

  Inventaire({
    this.id,
    required this.description,
    this.inventoryValueCostBegin,
    this.inventoryAmountBegin,
    this.inventoryValueCostAfter,
    this.inventoryAmountAfter,
    this.gapCost,
    this.gapAmount,
    this.statut,
    this.inventoryType,
    this.inventoryCategory,
  });

  factory Inventaire.fromJson(Map<String, dynamic> json) =>
      _$InventaireFromJson(json);

  Map<String, dynamic> toJson() => _$InventaireToJson(this);
}
