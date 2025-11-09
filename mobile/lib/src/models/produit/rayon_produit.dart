import 'package:json_annotation/json_annotation.dart';
part 'rayon_produit.g.dart';
@JsonSerializable()
class RayonProduit {
  final String codeRayon;
  final String libelleRayon;
  final String libelleStorage;
  final String storageType;


  RayonProduit({
    required this.codeRayon,
    required this.libelleRayon,
    required this.libelleStorage,
    required this.storageType,
  });

  factory RayonProduit.fromJson(Map<String, dynamic> json) =>
      _$RayonProduitFromJson(json);

  Map<String, dynamic> toJson() => _$RayonProduitToJson(this);
}