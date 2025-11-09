import 'package:json_annotation/json_annotation.dart';

part  'produit_fournisseur.g.dart';
@JsonSerializable()
class ProduitFournisseur {
  final String codeCip;
  final int prixUni;
  final int prixAchat;
  final String fournisseurLibelle;
  final bool principal;



  ProduitFournisseur({
    required this.codeCip,
    required this.prixUni,
    required this.prixAchat,
    required this.fournisseurLibelle,
    required this.principal ,
  });

  factory ProduitFournisseur.fromJson(Map<String, dynamic> json) => _$ProduitFournisseurFromJson(json);

  Map<String, dynamic> toJson() => _$ProduitFournisseurToJson(this);


}