import 'package:json_annotation/json_annotation.dart';
import 'package:mobile/src/models/list_item.dart';

part 'user_caisse_recap.g.dart';
@JsonSerializable()
class UserCaisseRecap {
  final String title ;
  final List<ListItem> items;
  final List<ListItem>? resume;

  UserCaisseRecap({required this.title, required this.items, this.resume});

  factory UserCaisseRecap.fromJson(Map<String, dynamic> json) =>
      _$UserCaisseRecapFromJson(json);

  Map<String, dynamic> toJson() => _$UserCaisseRecapToJson(this);
}
