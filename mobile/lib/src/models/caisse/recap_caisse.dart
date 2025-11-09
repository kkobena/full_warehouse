

import 'package:json_annotation/json_annotation.dart';
import 'package:mobile/src/models/list_item.dart';
import 'package:mobile/src/models/caisse/user_caisse_recap.dart';

part 'recap_caisse.g.dart';
@JsonSerializable()
class RecapCaisse {
  final List<UserCaisseRecap> items;
  final List<ListItem>? resume;

  RecapCaisse({required this.items, this.resume});

  factory RecapCaisse.fromJson(Map<String, dynamic> json) =>
      _$RecapCaisseFromJson(json);

  Map<String, dynamic> toJson() => _$RecapCaisseToJson(this);
}
