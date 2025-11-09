import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

class CustomAppBar extends StatelessWidget implements PreferredSizeWidget {
  final DateTime fromDate;
  final DateTime toDate;
  final VoidCallback onDateRangeTap;
  final VoidCallback? onRefreshTap;
  final List<Widget>? additionalActions;

  const CustomAppBar({
    Key? key,
    required this.fromDate,
    required this.toDate,
    required this.onDateRangeTap,
    this.onRefreshTap,
    this.additionalActions,
  });

  @override
  Widget build(BuildContext context) {
    final Color primaryColor = Theme
        .of(context)
        .colorScheme
        .primary;
    final DateFormat displayDateFormat = DateFormat('dd MMM', 'fr_FR');
    final HSLColor hslPrimary = HSLColor.fromColor(primaryColor);
    double lightnessIncrease = 0.25;
    final HSLColor hslLighterPrimary = hslPrimary.withLightness(
        (hslPrimary.lightness + lightnessIncrease).clamp(0.0, 1.0)
    );
    final Color lighterPrimaryColor = hslLighterPrimary.toColor();
    final Color appBarForegroundColor = Theme
        .of(context)
        .colorScheme
        .onPrimary;

    final Color gradientStart = primaryColor;
    final Color gradientEnd = lighterPrimaryColor;
    Color gradientForegroundColor = Theme
        .of(context)
        .colorScheme
        .onPrimary;
    final List<Color> effectiveGradientColors = [gradientStart, gradientEnd];




    return AppBar(
      title: InkWell(
        onTap: onDateRangeTap,
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 0),
          // Title usually has its own padding
          child: Row(
            mainAxisSize: MainAxisSize.min,
            children: <Widget>[
              Flexible(
                child: Text(
                  '${displayDateFormat.format(fromDate)} - ${displayDateFormat
                      .format(toDate)}',
                  style: TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.w500,
                    color: appBarForegroundColor,
                  ),
                  overflow: TextOverflow.ellipsis,
                ),
              ),
              const SizedBox(width: 4),
              Icon(Icons.arrow_drop_down, size: 24,
                  color: appBarForegroundColor),
            ],
          ),
        ),
      ),
      backgroundColor: Colors.transparent,
      elevation: 0,
      foregroundColor: appBarForegroundColor,
      // For back button, etc., if AppBar creates them
      centerTitle: true,
      // Default is usually false, but good to be explicit for custom titles
      titleSpacing: NavigationToolbar.kMiddleSpacing,
      // Default title spacing

      flexibleSpace: Container(
        decoration: BoxDecoration(
          gradient: LinearGradient(
            colors: effectiveGradientColors,
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
          ),
        ),
      ),
      actions: <Widget>[
        if (onRefreshTap != null)
          IconButton(
            icon: const Icon(Icons.refresh),
            tooltip: 'Actualiser',
            style: TextButton.styleFrom(
              foregroundColor: gradientForegroundColor,
              padding: EdgeInsets.symmetric(horizontal: 8.0, vertical: 4.0),
            ),
           // color: effectiveForegroundColor,

            onPressed: onRefreshTap,
          ),
        if (additionalActions != null) ...?additionalActions,
      ],
    );
  }

  @override
  Size get preferredSize =>
      const Size.fromHeight(kToolbarHeight); // Standard AppBar height
}