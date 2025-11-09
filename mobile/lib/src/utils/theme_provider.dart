import 'package:flutter/material.dart';
import 'package:mobile/src/data/services/utils/api_client.dart';
import 'package:mobile/src/utils/app_theme.dart';

enum AppThemes {
  vert, // Corresponds to AppTheme.themeDataVert
  bleu, // Corresponds to AppTheme.themeBleu
}

class ThemeProvider with ChangeNotifier {
late  ThemeData _currentTheme ; // Default theme
 late AppThemes _currentThemeKey ;

  ThemeData get currentTheme => _currentTheme;

  AppThemes get currentThemeKey => _currentThemeKey;
  Future<void> _loadCurrentTheme() async {

    _currentThemeKey = ApiClient().theme; // This uses your ApiClient's logic
    _currentTheme = _getThemeData(_currentThemeKey);

    print("ThemeProvider: Loaded theme from ApiClient: $_currentThemeKey");
    notifyListeners(); // Notify listeners after the theme is loaded
  }

  static ThemeData _getThemeData(AppThemes themeKey) {
    switch (themeKey) {
      case AppThemes.vert:
        return AppTheme.themeDataVert;
      case AppThemes.bleu:
        return AppTheme.themeBleu;
    }
  }
  ThemeProvider(){
    _currentThemeKey = ApiClient().theme; // Default theme
    _currentTheme = _getThemeData(AppThemes.bleu);
    _loadCurrentTheme(); // Load the current theme from ApiClient

  }

  Future<void> setTheme(AppThemes themeKey) async {
    if (themeKey == _currentThemeKey) {
      return;
    }
    _currentThemeKey = themeKey;
    _currentTheme = _getThemeData(themeKey);
    await ApiClient().updateTheme(themeKey);
    notifyListeners(); // Notify listeners that the theme has changed
  }
}
