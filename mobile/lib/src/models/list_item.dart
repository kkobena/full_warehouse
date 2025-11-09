import 'package:json_annotation/json_annotation.dart';

part 'list_item.g.dart';

@JsonSerializable()
class ListItem {
  final String libelle;
  final String value;
  final String? autre;

  ListItem({required this.libelle, required this.value, this.autre});

  factory ListItem.fromJson(Map<String, dynamic> json) => _$ListItemFromJson(json);

  // Method for serialization
  Map<String, dynamic> toJson() => _$ListItemToJson(this);

  double get percentageValue {
    return (double.tryParse(autre ?? '0') ?? 0.0) / 100.0; // Assuming 'autre' is "25" for 25%
  }
}
