import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:mobile/src/data/auth/model/user.dart';
import 'package:mobile/src/data/auth/servie/auth_service.dart';
import 'package:mobile/src/ui/stock/widgets/product_search_page.dart';
import 'package:mobile/src/ui/stock/widgets/stock_page.dart';
import 'package:mobile/src/utils/constant.dart';
import 'package:mobile/src/utils/theme_provider.dart';

class AppDrawer extends StatelessWidget {
  const AppDrawer({super.key});

  @override
  Widget build(BuildContext context) {
    // Access theme data and auth service
    final ThemeData theme = Theme.of(context);
    final authService = context.watch<AuthService>();
    final themeProvider = context.read<ThemeProvider>();
    final User? currentUser = authService.currentUser;
    final Color drawerHeaderColor = theme.colorScheme.primary;
    final Color drawerHeaderTextColor = theme.colorScheme.onPrimary;
    final Color drawerIconColor = theme.colorScheme.onSurface.withValues(
      alpha: 0.7,
    );
    final Color drawerTextColor = theme.colorScheme.onSurface;

    final String username = currentUser?.abbrName ?? 'Menu';
    final IconData headerIcon =
        Icons.add_circle_outline_rounded; // Example dynamic icon
    final String themeSwitchLabel =
        themeProvider.currentThemeKey == AppThemes.bleu ? 'Bleu' : 'Vert';

    return Drawer(
      child: ListView(
        padding: EdgeInsets.zero,
        children: <Widget>[
          DrawerHeader(
            decoration: BoxDecoration(color: drawerHeaderColor),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Icon(headerIcon, size: 48, color: drawerHeaderTextColor),
                const SizedBox(height: 12),
                Text(
                  username, // Display username or app name
                  style: TextStyle(
                    color: drawerHeaderTextColor,
                    fontSize: 20,

                    fontWeight: FontWeight.bold,
                  ),
                  overflow: TextOverflow.ellipsis, // Handle long names
                ),
              ],
            ),
          ),
          ListTile(
            leading: Icon(Icons.store, color: drawerIconColor),
            title: Text(
              Constant.stock,
              style: TextStyle(color: drawerTextColor),
            ),
            onTap: () {
              Navigator.pop(context); // Close the drawer
              Navigator.pushNamed(context,ProductSearchPage.routeName); // Navigate to TvaPage
            },
          ),
          const Divider(),


          ListTile(
            leading: Icon(Icons.inventory, color: drawerIconColor),
            title: Text(
              Constant.inventaire,
              style: TextStyle(color: drawerTextColor),
            ),
            onTap: () {
              Navigator.pop(context); // Close the drawer
           //   Navigator.pushNamed(context,StockPage.routeName); // Navigate to TvaPage
            },
          ),
          const Divider(),
          SwitchListTile(
            title: Text(
              themeSwitchLabel, // Dynamic label
              style: TextStyle(color: drawerTextColor),
            ),
            value: themeProvider.currentThemeKey == AppThemes.bleu,
            // Switch is "on" if current theme is Bleu
            onChanged: (bool value) async {
              await themeProvider.setTheme(
                value ? AppThemes.bleu : AppThemes.vert,
              );
            },
            secondary: Icon(
              themeProvider.currentThemeKey == AppThemes.bleu
                  ? Icons.dark_mode_outlined
                  : Icons.light_mode_outlined,
              color: drawerIconColor,
            ),
            activeColor:
            theme.colorScheme.primary, // Color of the switch when it's ON
          ),
          const Divider(),
          ListTile(
            leading: Icon(Icons.logout, color: drawerIconColor),
            title: Text(
              'Déconnexion',
              style: TextStyle(color: drawerTextColor),
            ),
            onTap: () async {
              final navigator = Navigator.of(context);
              if (navigator.canPop()) {
                navigator.pop();
              }
              await authService.logout();
            },
          ),
        ],
      ),
    );
  }
}
