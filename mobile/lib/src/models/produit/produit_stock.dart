import 'package:json_annotation/json_annotation.dart';

part 'produit_stock.g.dart';

@JsonSerializable()
class ProduitStock {
  final int qtyStock;
  final int qtyVirtual;
  final int qtyUG;
  final String storageName;
  final String storageType;


  ProduitStock({

    required this.qtyStock,
    required this.qtyVirtual,
    required this.qtyUG,
    required this.storageName,
    required this.storageType,

  });
  factory ProduitStock.fromJson(Map<String, dynamic> json) => _$ProduitStockFromJson(json);
  Map<String, dynamic> toJson() => _$ProduitStockToJson(this);

}
