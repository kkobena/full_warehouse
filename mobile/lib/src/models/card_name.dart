enum CardName {
  resume('RESUME','Résumé'),
  typeVente('TYPE_VENTE','Type de vente'),
  modePaiement('MODE_PAIEMENT','Mode de paiement'),
  typeMvt('TYPE_MVT','Autres mouvements');

  final String value;
  final String label;

const CardName(this.value, this.label);

  @override
  String toString() => label;

  factory CardName.fromString(String value) {
    return CardName.values.firstWhere(
      (e) => e.value == value,
      orElse: () => throw ArgumentError('Invalid CardName value: $value'),
    );
  }
}
