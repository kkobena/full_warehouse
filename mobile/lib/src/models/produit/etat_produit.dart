import 'package:json_annotation/json_annotation.dart';

part 'etat_produit.g.dart';

@JsonSerializable()
class EtatProduit {
  final bool stockPositif;
  final bool sockZero;
  final bool stockNegatif;
  final bool enSuggestion;
  final bool enCommande;
  final bool entree;
  final bool otherSuggestion;
  final bool otherCommande;

  EtatProduit({
    required this.stockPositif,
    required this.sockZero,
    required this.stockNegatif,
    required this.enSuggestion,
    required this.enCommande,
    required this.entree,
    required this.otherSuggestion,
    required this.otherCommande,
  });

  factory EtatProduit.fromJson(Map<String, dynamic> json) =>
      _$EtatProduitFromJson(json);

  Map<String, dynamic> toJson() => _$EtatProduitToJson(this);
}
