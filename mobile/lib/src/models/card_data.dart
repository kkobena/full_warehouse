import 'package:json_annotation/json_annotation.dart';
import 'package:mobile/src/models/list_item.dart';

part 'card_data.g.dart';

@JsonSerializable()
class CardData {
  final String title;
  final int? sum;
  final List<ListItem> items;
  final List<ListItem>? summary;

  CardData({required this.title, this.sum, required this.items, this.summary});

  factory CardData.fromJson(Map<String, dynamic> json) =>
      _$CardDataFromJson(json);

  Map<String, dynamic> toJson() => _$CardDataToJson(this);
}
