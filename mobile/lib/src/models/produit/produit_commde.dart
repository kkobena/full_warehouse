import 'package:json_annotation/json_annotation.dart';
part 'produit_commde.g.dart';
@JsonSerializable()
class ProduitCommde {
  final  String date;
  final String quantity;
  final String? fournisseur;
  final String? numero;
  final String? unitPrice;
  final String? totalAmount;
  final String? grossAmount;
  final String? totaGrossAmount;
  final String? userName;

  ProduitCommde({
    required this.date,
    required this.quantity,
    this.fournisseur,
    this.numero,
    this.unitPrice,
    this.totalAmount,
    this.grossAmount,
    this.totaGrossAmount,
    this.userName,
  });

  factory ProduitCommde.fromJson(Map<String, dynamic> json) => _$ProduitCommdeFromJson(json);
  Map<String, dynamic> toJson() => _$ProduitCommdeToJson(this);

}