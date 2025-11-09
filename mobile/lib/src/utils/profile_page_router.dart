import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:mobile/main.dart';
import 'package:mobile/src/data/auth/servie/auth_service.dart';
import 'package:mobile/src/data/services/utils/api_client.dart';
import 'package:mobile/src/ui/auth/authenticate.dart';

class ProfilePageRouter extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    final authService = context.watch<AuthService>();
    final apiClient = ApiClient();
    if ((apiClient.rememberMe && apiClient.user != null) ||
        authService.isAuthenticated) {
      return MyHomePage();

    } else {

      apiClient.clearCredentials();
      return Authenticate();
    }

  }
}
