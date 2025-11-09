import 'package:json_annotation/json_annotation.dart';
 part 'user.g.dart';
@JsonSerializable()
class User {
  String? login;
  String? firstName;
  String? lastName;
  String? roleName;
  String? abbrName;
  String? password;
  int? id;

  User({
    this.login,
    this.firstName,
    this.lastName,
    this.roleName,
    this.abbrName,
    this.password,
    this.id,
  });
String? get fullName => abbrName;

  factory User.fromJson(Map<String, dynamic> json) =>
      _$UserFromJson(json);

  Map<String, dynamic> toJson() => _$UserToJson(this);

}
