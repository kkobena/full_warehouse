import 'dart:convert';


import 'package:flutter/material.dart';
import 'package:mobile/src/data/auth/model/user.dart';
import 'package:mobile/src/data/services/utils/api_client.dart';
import 'package:http/http.dart' as http;

class AuthService extends ChangeNotifier {
  bool _isLoading = false;
  String _errorMessage = '';
  User? _currentUser;
  bool _isAuthenticated = false;

  bool get isLoading => _isLoading;

  String get errorMessage => _errorMessage;

  User? get currentUser => _currentUser;

  bool get isAuthenticated => _isAuthenticated;

  String? get currentUserRole => _currentUser?.roleName;

  Future<void> login(String usernameOrEmail, String password, String? apiurl) async {
    _isLoading = true;
    _errorMessage = '';
    notifyListeners();

    try {
      if (apiurl != null) {
        await ApiClient().saveApiUrl(apiurl);
      }

      // Step 1: Authenticate and get JWT tokens
      final Uri loginUri = Uri.parse('${ApiClient().authUrl}');

      final loginResponse = await http.post(
        loginUri,
        headers: {'Content-Type': 'application/json; charset=UTF-8'},
        body: jsonEncode(<String, String>{
          'username': usernameOrEmail,
          'password': password,
        }),
      ).timeout(const Duration(seconds: 10));

      if (loginResponse.statusCode == 200) {
        final tokenData = json.decode(loginResponse.body);

        // Save JWT tokens
        final String accessToken = tokenData['accessToken'];
        final String refreshToken = tokenData['refreshToken'];
        final int expiresIn = tokenData['expiresIn'] ?? 28800; // Default 8 hours

        await ApiClient().saveTokens(accessToken, refreshToken, expiresIn);

        // Step 2: Fetch user details using the access token
        final Uri accountUri = Uri.parse('${ApiClient().apiUrl}/account');

        final accountResponse = await http.get(
          accountUri,
          headers: ApiClient().headers, // Now includes Bearer token
        ).timeout(const Duration(seconds: 10));

        if (accountResponse.statusCode == 200) {
          final userData = json.decode(accountResponse.body);

          // Map AdminUserDTO to User model
          _currentUser = User(
            id: userData['id'],
            login: userData['login'],
            firstName: userData['firstName'],
            lastName: userData['lastName'],
            roleName: _extractRoleName(userData),
            abbrName: _buildAbbrName(userData),
          );

          if (_currentUser != null) {
            if (checkUserRole()) {
              await ApiClient().saveUser(
                _currentUser!,
                usernameOrEmail,
                password,
                true,
              );
              _isAuthenticated = true;
              _errorMessage = '';
            } else {
              _isAuthenticated = false;
              _errorMessage = 'L\'utilisateur n\'a pas de rôle défini.';
            }
          } else {
            _isAuthenticated = false;
            _errorMessage = 'Échec de la connexion. Utilisateur introuvable.';
          }
        } else {
          _isAuthenticated = false;
          _errorMessage = 'Échec de la récupération des informations utilisateur';
        }
      } else if (loginResponse.statusCode == 401) {
        _isAuthenticated = false;
        _errorMessage = 'Nom d\'utilisateur ou mot de passe incorrect';
      } else {
        _isAuthenticated = false;
        try {
          final errorData = json.decode(loginResponse.body);
          _errorMessage = errorData['message'] ?? 'Échec de la connexion';
        } catch (e) {
          _errorMessage = 'Échec de la connexion';
        }
      }
    } catch (e) {
      _isAuthenticated = false;
      _errorMessage = 'Une erreur inattendue s\'est produite';
      print('Erreur lors de la connexion : $e');
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  /// Extract role name from AdminUserDTO authorities
  String? _extractRoleName(Map<String, dynamic> userData) {
    if (userData['authorities'] != null && userData['authorities'] is List) {
      final List authorities = userData['authorities'];
      if (authorities.isNotEmpty) {
        return authorities.first.toString();
      }
    }
    return null;
  }

  /// Build abbreviated name from user data
  String _buildAbbrName(Map<String, dynamic> userData) {
    final firstName = userData['firstName'] ?? '';
    final lastName = userData['lastName'] ?? '';
    return '$firstName $lastName'.trim();
  }


  Future<void> logout() async {
    _currentUser = null;
    _errorMessage = '';
    await ApiClient().clearCredentials();
    _isAuthenticated = false;
    notifyListeners();
  }

  bool checkUserRole() {
    final String? roleName = _currentUser?.roleName; // Get roleName safely
    return roleName != null && roleName.isNotEmpty;
  }

  Future<void> autoLogin() async {
    // Check if we have valid tokens
    if (ApiClient().accessToken != null && !ApiClient().isTokenExpired) {
      // Try to load user from stored token
      try {
        final Uri accountUri = Uri.parse('${ApiClient().apiUrl}/account');
        final accountResponse = await http.get(
          accountUri,
          headers: ApiClient().headers,
        ).timeout(const Duration(seconds: 10));

        if (accountResponse.statusCode == 200) {
          final userData = json.decode(accountResponse.body);
          _currentUser = User(
            id: userData['id'],
            login: userData['login'],
            firstName: userData['firstName'],
            lastName: userData['lastName'],
            roleName: _extractRoleName(userData),
            abbrName: _buildAbbrName(userData),
          );
          _isAuthenticated = true;
          notifyListeners();
          return;
        }
      } catch (e) {
        print('Auto-login with token failed: $e');
      }
    }

    // If token is invalid or expired, try to login with saved credentials
    final username = ApiClient().username;
    final password = ApiClient().password;
    if (username != null && password != null && username.isNotEmpty && password.isNotEmpty) {
      await login(username, password, null);
    }
  }
}
