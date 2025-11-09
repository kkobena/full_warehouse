import 'package:json_annotation/json_annotation.dart';
import 'package:mobile/src/models/list_item.dart';

part 'dashboard.g.dart';

@JsonSerializable()
class Dashboard {
  List<ListItem>? sales;
  List<ListItem>? netAmounts;
  List<ListItem>? salesTypes;
  List<ListItem>? counts;
  List<ListItem>? paymentModes;
  List<ListItem>? commandes;

  Dashboard({
    this.sales,
    this.netAmounts,
    this.salesTypes,
    this.counts,
    this.paymentModes,
    this.commandes,
  });

  factory Dashboard.fromJson(Map<String, dynamic> json) => _$DashboardFromJson(json);

  // Method for serialization
  Map<String, dynamic> toJson() => _$DashboardToJson(this);
}
