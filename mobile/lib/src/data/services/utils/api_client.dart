
import 'package:hive/hive.dart';

import 'package:mobile/src/data/auth/model/user.dart';
import 'package:mobile/src/utils/theme_provider.dart';

class ApiClient {
  static final ApiClient _instance = ApiClient._internal();

  ApiClient._internal();

  factory ApiClient() {
    return _instance;
  }

  final Box _box = Hive.box('settings');

  Future<void> saveApiUrl(String apiUrl) async {
    await _box.put('apiUrl', 'http://$apiUrl/api');
    await _box.put('auth', 'http://$apiUrl/api/auth/login');
    await _box.put('javaClient', 'http://$apiUrl/java-client');
    await _box.put('appIp', apiUrl);
  }

  Future<void> saveCredentials(
    String username,
    String password,
    bool rememberMe,
  ) async {
    await _box.put('username', username);
    await _box.put('password', password);
    await _box.put('rememberMe', rememberMe);
  }

  Future<void> saveTokens(
    String accessToken,
    String refreshToken,
    int expiresIn,
  ) async {
    await _box.put('accessToken', accessToken);
    await _box.put('refreshToken', refreshToken);
    // Save expiration timestamp (current time + expiresIn seconds)
    final expirationTime = DateTime.now().millisecondsSinceEpoch + (expiresIn * 1000);
    await _box.put('tokenExpiration', expirationTime);
  }

  Future<void> saveUser(User user, String username, String password, bool rememberMe) async {
    await _box.put('username', username);
    await _box.put('password', password);
    await _box.put('rememberMe', rememberMe);
    await _box.put('user', user.toJson());
  }

  String? get apiUrl => _box.get('apiUrl');

  String? get authUrl => _box.get('auth');

  String? get javaClientUrl => _box.get('javaClient');

  String? get appIp => _box.get('appIp');

  String? get _username => _box.get('username');

  String? get _password => _box.get('password');

  String? get username => _username;

  String? get password => _password;

  String? get accessToken => _box.get('accessToken');

  String? get refreshToken => _box.get('refreshToken');

  bool get rememberMe => _box.get('rememberMe', defaultValue: false);

  AppThemes get theme => fromString(_box.get('theme', defaultValue: 'bleu'));

  /// Check if access token is expired
  bool get isTokenExpired {
    final expiration = _box.get('tokenExpiration');
    if (expiration == null) return true;
    return DateTime.now().millisecondsSinceEpoch >= expiration;
  }

  User? get user {
    final userJson = _box.get('user');
    if (userJson != null) {
      return User.fromJson(Map<String, dynamic>.from(userJson));
    }
    return null;
  }

  /// Get headers with JWT Bearer token
  Map<String, String> get headers {
    final token = accessToken;
    return {
      'Content-Type': 'application/json; charset=UTF-8',
      if (token != null) 'Authorization': 'Bearer $token'
    };
  }


  Future<void> clearCredentials() async {
    await _box.delete('username');
    await _box.delete('password');
    await _box.delete('rememberMe');
    await _box.delete('user');
    await _box.delete('accessToken');
    await _box.delete('refreshToken');
    await _box.delete('tokenExpiration');
  }

  Future<void> updateTheme(AppThemes th) async {

    await _box.put('theme', th.name);
  }

  AppThemes fromString(String value) {

    return AppThemes.values.firstWhere(
      (e) => e.name.toLowerCase() == value.toLowerCase(),
      orElse: () => AppThemes.bleu, // or return null if nullable
    );
  }


}
