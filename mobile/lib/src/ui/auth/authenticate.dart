import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:mobile/main.dart';
import 'package:mobile/src/data/auth/servie/auth_service.dart';
import 'package:mobile/src/utils/constant.dart';

import '../../data/services/utils/api_client.dart';

class Authenticate extends StatefulWidget {
  const Authenticate({super.key});

  static const String routeName = '/auth';

  @override
  State<Authenticate> createState() => _AuthenticateState();
}

class _AuthenticateState extends State<Authenticate> {
  final apiClient = ApiClient();
  final _formKey = GlobalKey<FormState>();
  final _apiUrlController = TextEditingController();
  final _usernameController = TextEditingController();
  final _passwordController = TextEditingController();
  bool _obscurePassword = true;
  bool _isApiUrlEditable = true;

  @override
  void initState() {
    super.initState();
    // Initialize _apiUrlController with the current API URL from ApiClient
    _apiUrlController.text = apiClient.appIp ?? '';
    _isApiUrlEditable =
        apiClient.appIp == null || apiClient.appIp!.trim().isEmpty;
  }

  @override
  void dispose() {
    _apiUrlController.dispose();
    _usernameController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  Future<void> _performLogin() async {
    if (_formKey.currentState?.validate() ?? false) {
      final authService = context.read<AuthService>();
      await authService.login(
        _usernameController.text.trim(),
        _passwordController.text.trim(),
        _apiUrlController.text.trim(),
      );

      if (authService.isAuthenticated && mounted) {

        Navigator.of(context).pushReplacementNamed(
          MyHomePage.routeName,
        );
      } else if (mounted) {
        // Show error message from authService.errorMessage
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(
              authService.errorMessage.isNotEmpty
                  ? authService.errorMessage
                  : 'Échec de la connexion inattendu.',
            ),
            backgroundColor: Colors.redAccent,
          ),
        );
      }
    }
  }

  void _showServerConfigDialog() {
    final serverUrlController = TextEditingController(text: apiClient.appIp ?? '');
    final dialogFormKey = GlobalKey<FormState>();

    showDialog(
      context: context,
      builder: (BuildContext dialogContext) {
        return AlertDialog(
          title: Row(
            children: [
              Icon(Icons.settings, color: Theme.of(context).colorScheme.primary),
              const SizedBox(width: 8),
              const Text('Configuration du serveur'),
            ],
          ),
          content: Form(
            key: dialogFormKey,
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  'Entrez l\'adresse IP et le port du serveur',
                  style: Theme.of(context).textTheme.bodyMedium,
                ),
                const SizedBox(height: 16),
                TextFormField(
                  controller: serverUrlController,
                  decoration: InputDecoration(
                    labelText: 'Ip et port du service',
                    hintText: '192.168.1.1:8080',
                    prefixIcon: const Icon(Icons.link),
                    border: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(12),
                    ),
                  ),
                  keyboardType: TextInputType.text,
                  validator: (value) {
                    if (value == null || value.trim().isEmpty) {
                      return 'Veuillez entrer l\'URL du service.';
                    }
                    final trimmedValue = value.trim();
                    final isIpWithPort = RegExp(
                      r'^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?):\d+$',
                    ).hasMatch(trimmedValue);
                    if (!isIpWithPort) {
                      return 'Format invalide (ex: 192.168.1.1:8080)';
                    }
                    return null;
                  },
                ),
                const SizedBox(height: 16),
                if (apiClient.appIp != null && apiClient.appIp!.isNotEmpty)
                  Container(
                    padding: const EdgeInsets.all(12),
                    decoration: BoxDecoration(
                      color: Theme.of(context).colorScheme.surfaceContainerHighest,
                      borderRadius: BorderRadius.circular(8),
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          'Configuration actuelle:',
                          style: Theme.of(context).textTheme.bodySmall?.copyWith(
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                        const SizedBox(height: 4),
                        Text(
                          apiClient.appIp!,
                          style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                            fontFamily: 'monospace',
                          ),
                        ),
                      ],
                    ),
                  ),
              ],
            ),
          ),
          actions: [
            if (apiClient.appIp != null && apiClient.appIp!.isNotEmpty)
              TextButton.icon(
                onPressed: () async {
                  final confirm = await showDialog<bool>(
                    context: dialogContext,
                    builder: (ctx) => AlertDialog(
                      title: const Text('Confirmer la réinitialisation'),
                      content: const Text(
                        'Êtes-vous sûr de vouloir réinitialiser la configuration du serveur?',
                      ),
                      actions: [
                        TextButton(
                          onPressed: () => Navigator.pop(ctx, false),
                          child: const Text('Annuler'),
                        ),
                        TextButton(
                          onPressed: () => Navigator.pop(ctx, true),
                          style: TextButton.styleFrom(
                            foregroundColor: Colors.red,
                          ),
                          child: const Text('Réinitialiser'),
                        ),
                      ],
                    ),
                  );

                  if (confirm == true && mounted) {
                    await apiClient.clearCredentials();
                    setState(() {
                      _apiUrlController.clear();
                      _isApiUrlEditable = true;
                    });
                    Navigator.pop(dialogContext);
                    if (mounted) {
                      ScaffoldMessenger.of(context).showSnackBar(
                        const SnackBar(
                          content: Text('Configuration du serveur réinitialisée'),
                          backgroundColor: Colors.orange,
                        ),
                      );
                    }
                  }
                },
                icon: const Icon(Icons.refresh),
                label: const Text('Réinitialiser'),
                style: TextButton.styleFrom(
                  foregroundColor: Colors.orange,
                ),
              ),
            TextButton(
              onPressed: () => Navigator.pop(dialogContext),
              child: const Text('Annuler'),
            ),
            ElevatedButton(
              onPressed: () {
                if (dialogFormKey.currentState?.validate() ?? false) {
                  setState(() {
                    _apiUrlController.text = serverUrlController.text.trim();
                    _isApiUrlEditable = false;
                  });
                  Navigator.pop(dialogContext);
                  ScaffoldMessenger.of(context).showSnackBar(
                    SnackBar(
                      content: Text(
                        'Serveur configuré: ${serverUrlController.text.trim()}',
                      ),
                      backgroundColor: Colors.green,
                    ),
                  );
                }
              },
              child: const Text('Enregistrer'),
            ),
          ],
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    final authService = context.read<AuthService>();

    return Scaffold(
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        elevation: 0,
        actions: [
          IconButton(
            icon: const Icon(Icons.settings),
            tooltip: 'Configuration du serveur',
            onPressed: _showServerConfigDialog,
          ),
        ],
      ),
      body: Center(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24.0),
          child: Form(
            key: _formKey,
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: <Widget>[
              /*  Image.asset(
                  'assets/images/appstore.png',
                  height: 80,
                  width: 80,
                  fit: BoxFit.contain,
                ),*/
                Icon(
                  Icons.add_circle_outline_rounded, // Or your app logo
                  size: 80,
                  color: Theme.of(context).colorScheme.primary,
                ),
                const SizedBox(height: 16),
                Text(
                  Constant.appName,
                  textAlign: TextAlign.center,
                  style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                    color: Theme.of(context).colorScheme.primary,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                const SizedBox(height: 32),
                Row(
                  children: [
                    Expanded(
                      child: TextFormField(
                        controller: _apiUrlController,
                        enabled: _isApiUrlEditable,
                        decoration: InputDecoration(
                          labelText: 'Ip et port du service',
                          prefixIcon: Icon(Icons.link),
                          border: OutlineInputBorder(
                            borderRadius: BorderRadius.circular(12),
                          ),
                        ),
                        keyboardType: TextInputType.text,
                        validator: (value) {
                          if (value == null || value.trim().isEmpty) {
                            return 'Veuillez entrer l\'URL du service.';
                          }
                          final trimmedValue = value.trim();

                          // Regex for IP address with a required port
                          final isIpWithPort = RegExp(
                            r'^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?):\d+$',
                          ).hasMatch(trimmedValue);
                          if (!isIpWithPort) {
                            return '(ex: 192.168.1.1:8080)';
                          }
                          return null;
                        },
                      ),
                    ),
                    if (!_isApiUrlEditable)
                      IconButton(
                        icon: Icon(Icons.edit),
                        onPressed: () {
                          setState(() {
                            _isApiUrlEditable = true;
                          });
                        },
                        tooltip: 'Modifier l\'URL du service',
                      ),
                  ],
                ),
                const SizedBox(height: 16),
                // Username/Email Field
                TextFormField(
                  controller: _usernameController,
                  decoration: InputDecoration(
                    labelText: 'Nom d\'utilisateur',
                    prefixIcon: Icon(Icons.person_outline),
                    border: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(12),
                    ),
                  ),
                  keyboardType: TextInputType.text,
                  validator: (value) {
                    if (value == null || value.trim().isEmpty) {
                      return 'Veuillez entrer votre nom d\'utilisateur.';
                    }
                    // Optional: Add more specific email validation if needed
                    return null;
                  },
                ),
                const SizedBox(height: 16),

                // Password Field
                TextFormField(
                  controller: _passwordController,
                  decoration: InputDecoration(
                    labelText: 'Mot de passe',
                    prefixIcon: Icon(Icons.lock_outline),
                    border: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(12),
                    ),
                    suffixIcon: IconButton(
                      icon: Icon(
                        _obscurePassword
                            ? Icons.visibility_off
                            : Icons.visibility,
                      ),
                      onPressed: () {
                        setState(() {
                          _obscurePassword = !_obscurePassword;
                        });
                      },
                    ),
                  ),
                  obscureText: _obscurePassword,
                  validator: (value) {
                    if (value == null || value.isEmpty) {
                      return 'Veuillez entrer votre mot de passe.';
                    }
                    // Optional: Add password strength validation
                    return null;
                  },
                ),
                const SizedBox(height: 24),

                // Login Button
                authService.isLoading
                    ? Center(child: CircularProgressIndicator())
                    : ElevatedButton(
                        onPressed: _performLogin,
                        style: ElevatedButton.styleFrom(
                          padding: const EdgeInsets.symmetric(vertical: 16),
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(12),
                          ),
                          backgroundColor: Theme.of(
                            context,
                          ).colorScheme.primary,
                          foregroundColor: Theme.of(
                            context,
                          ).colorScheme.onPrimary,
                        ),
                        child: const Text(
                          'SE CONNECTER',
                          style: TextStyle(
                            fontSize: 16,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      ),
                const SizedBox(height: 16),

                // Server Configuration Link
                TextButton.icon(
                  onPressed: _showServerConfigDialog,
                  icon: const Icon(Icons.settings_outlined, size: 18),
                  label: const Text('Configuration du serveur'),
                  style: TextButton.styleFrom(
                    foregroundColor: Theme.of(context).colorScheme.secondary,
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
