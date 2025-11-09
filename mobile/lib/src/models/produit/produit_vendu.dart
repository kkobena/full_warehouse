import 'package:json_annotation/json_annotation.dart';

part 'produit_vendu.g.dart';

@JsonSerializable()
class ProduitVendu {
  final String date;
  final String quantity;
  final String? numero;
  final String? unitPrice;
  final String? totalAmount;
  final String? grossAmount;
  final String? totaGrossAmount;
  final String? userName;

  ProduitVendu({
    required this.date,
    required this.quantity,
    this.numero,
    this.unitPrice,
    this.totalAmount,
    this.grossAmount,
    this.totaGrossAmount,
    this.userName,
  });

  factory ProduitVendu.fromJson(Map<String, dynamic> json) =>
      _$ProduitVenduFromJson(json);

  Map<String, dynamic> toJson() => _$ProduitVenduToJson(this);
}
