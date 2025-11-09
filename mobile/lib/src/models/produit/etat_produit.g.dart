// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'etat_produit.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

EtatProduit _$EtatProduitFromJson(Map<String, dynamic> json) => EtatProduit(
      stockPositif: json['stockPositif'] as bool,
      sockZero: json['sockZero'] as bool,
      stockNegatif: json['stockNegatif'] as bool,
      enSuggestion: json['enSuggestion'] as bool,
      enCommande: json['enCommande'] as bool,
      entree: json['entree'] as bool,
      otherSuggestion: json['otherSuggestion'] as bool,
      otherCommande: json['otherCommande'] as bool,
    );

Map<String, dynamic> _$EtatProduitToJson(EtatProduit instance) =>
    <String, dynamic>{
      'stockPositif': instance.stockPositif,
      'sockZero': instance.sockZero,
      'stockNegatif': instance.stockNegatif,
      'enSuggestion': instance.enSuggestion,
      'enCommande': instance.enCommande,
      'entree': instance.entree,
      'otherSuggestion': instance.otherSuggestion,
      'otherCommande': instance.otherCommande,
    };
