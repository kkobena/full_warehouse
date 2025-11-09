import 'package:flutter/material.dart';

class AppTheme {
  static ThemeData get themeDataVert => ThemeData(
    // Palette "Moderne et Bien-Être"
    colorScheme: ColorScheme.fromSeed(
      seedColor: const Color(0xFF4DB6AC),
      // Turquoise doux
      primary: const Color(0xFF4DB6AC),
      secondary: const Color(0xFFB0BEC5),
      // Bleu gris clair
      surface: const Color(0xFFF5F5F5),
      // Light Grayish White for Cards
      onSurface: const Color(0xFF333333),
      // Dark Gray for text on surface

      // Blanc crème légèrement verdâtre
      surfaceContainerHighest: const Color(0xFF333333),
      error: const Color(0xFFE57373),
      // Un rouge doux pour les erreurs
      onError: Colors.white,
    ),
    scaffoldBackgroundColor: const Color(0xFFEAFAF0),
    // Blanc crème
    appBarTheme: const AppBarTheme(
      backgroundColor: Color(0xFF4DB6AC),
      foregroundColor: Colors.white,
      elevation: 2.0,
    ),
    textTheme: const TextTheme(
      bodyLarge: TextStyle(color: Color(0xFF333333), fontSize: 16),
      bodyMedium: TextStyle(color: Color(0xFF333333), fontSize: 14),
      headlineSmall: TextStyle(
        color: Color(0xFF333333),
        fontWeight: FontWeight.bold,
      ),
      titleLarge: TextStyle(
        color: Color(0xFF333333),
        fontWeight: FontWeight.bold,
      ),
      labelLarge: TextStyle(color: Colors.white, fontWeight: FontWeight.w500),
    ),
    elevatedButtonTheme: ElevatedButtonThemeData(
      style: ElevatedButton.styleFrom(
        backgroundColor: const Color(0xFF4DB6AC),
        foregroundColor: Colors.white,
        padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 12),
        textStyle: const TextStyle(fontSize: 16, fontWeight: FontWeight.w500),
      ),
    ),
    textButtonTheme: TextButtonThemeData(
      style: TextButton.styleFrom(foregroundColor: const Color(0xFF4DB6AC)),
    ),
    cardTheme: CardThemeData(
      elevation: 1.0,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12.0)),
      // color: Color(0xFFF5F5F5), // Explicitly set or rely on colorScheme.surface
    ),
    inputDecorationTheme: InputDecorationTheme(
      border: OutlineInputBorder(
        borderRadius: BorderRadius.circular(8.0),
        borderSide: const BorderSide(
          color: Color(0xFFCFD8DC),
        ), // Gris très clair
      ),
      focusedBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(8.0),
        borderSide: const BorderSide(
          color: Color(0xFF4DB6AC), // Couleur primaire en focus
          width: 2.0,
        ),
      ),
      filled: true,
      fillColor: Colors.white.withValues(alpha: 0.5),
      // Un remplissage très subtil
      labelStyle: const TextStyle(
        color: Color(0xFF555555), // Dark Gray
      ),
    ),
    floatingActionButtonTheme: const FloatingActionButtonThemeData(
      backgroundColor: Color(0xFFB0BEC5), // Couleur secondaire
      foregroundColor: Color(0xFF37474F), // Texte/icône contrastant
    ),
    useMaterial3: true,
  );

  static ThemeData get themeBleu => ThemeData(
    useMaterial3: true,
    // Palette "Confiance et Sérénité"
    colorScheme: ColorScheme.fromSeed(
      seedColor: const Color(0xFF2A7AAE),
      primary: const Color(0xFF2A7AAE),
      // Bleu confiant
      secondary: const Color(0xFF8DBA8E),
      // Vert doux/sauge
      surface: const Color(0xFFFFFFFF),
      // Blanc pur pour les surfaces de cartes
      onSurface: const Color(0xFF343A40),

      // Fond général
      surfaceContainerHighest: const Color(0xFF343A40),
      error: const Color(0xFFD32F2F),
      // Un rouge d'erreur standard
      onError: Colors.white,
    ),
    scaffoldBackgroundColor: const Color(0xFFF5F6F7),
    appBarTheme: const AppBarTheme(
      backgroundColor: Color(0xFF2F7CAF),
      foregroundColor: Colors.white,
      elevation: 2.0,
    ),
    textTheme: const TextTheme(
      bodyLarge: TextStyle(color: Color(0xFF343A40), fontSize: 16),
      bodyMedium: TextStyle(color: Color(0xFF343A40), fontSize: 14),
      headlineSmall: TextStyle(
        color: Color(0xFF212529),
        fontWeight: FontWeight.bold,
      ),
      titleLarge: TextStyle(
        color: Color(0xFF2F7CAF),
        fontWeight: FontWeight.bold,
      ),
      labelLarge: TextStyle(color: Colors.white, fontWeight: FontWeight.w500),
    ),
    elevatedButtonTheme: ElevatedButtonThemeData(
      style: ElevatedButton.styleFrom(
        backgroundColor: const Color(0xFF2F7CAF),
        foregroundColor: Colors.white,
        padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 12),
        textStyle: const TextStyle(fontSize: 16, fontWeight: FontWeight.w500),
      ),
    ),
    textButtonTheme: TextButtonThemeData(
      style: TextButton.styleFrom(foregroundColor: const Color(0xFF2F7CAF)),
    ),
    cardTheme: CardThemeData(
      elevation: 1.0,
      color: const Color(0xFFFFFFFF),
      // Surface des cartes explicitement en blanc
      surfaceTintColor: Colors.transparent,
      // Pour éviter teintes non désirées M3
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(8.0),
        side: const BorderSide(
          color: Color(0xFFE9ECEF),
          width: 0.5,
        ), // Bordure subtile
      ),
      margin: const EdgeInsets.symmetric(horizontal: 8.0, vertical: 6.0),
    ),
    inputDecorationTheme: InputDecorationTheme(
      border: OutlineInputBorder(
        borderRadius: BorderRadius.circular(8.0),
        borderSide: const BorderSide(color: Color(0xFFCED4DA)),
      ),
      focusedBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(8.0),
        borderSide: const BorderSide(color: Color(0xFF2A7AAE), width: 2.0),
      ),
      filled: true,
      fillColor: const Color(0xFFFFFFFF),
      labelStyle: const TextStyle(color: Color(0xFF495057)),
    ),
    dividerTheme: const DividerThemeData(
      color: Color(0xFFE9ECEF),
      thickness: 1,
    ),
    floatingActionButtonTheme: const FloatingActionButtonThemeData(
      backgroundColor: Color(0xFF8DBA8E), // Vert doux
      foregroundColor: Color(0xFF212529), // Noir doux
    ),
  );
}
