/*import 'dart:ui';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

const List<Color> kWalletColors = [
  Color(0xFFF4B03E), // BTC
  Color(0xFF6E6E78), // ETH
  Color(0xFF65CD80), // GBP
  Color(0xFFCEDC2B), // EUR
  // Add more as needed
];
class WalletItem {
  final String name;
  final double percent;
  final String value;
  final String symbol;
  final int order;

  WalletItem({
    required this.name,
    required this.percent,
    required this.value,
    required this.symbol,
    required this.order,
  });

  factory WalletItem.fromJson(Map<String, dynamic> json) {
    return WalletItem(
      name: json['name'],
      percent: (json['percent'] as num).toDouble(),
      value: json['value'],
      symbol: json['symbol'],
      order: json['order'],
    );
  }

  /// 🟡 Assign color from predefined list by order (with fallback)
  Color get color => kWalletColors.length > order
      ? kWalletColors[order]
      : Colors.blueGrey;
}

void main() {
  runApp(MyApp());
}

class MyApp extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return ChangeNotifierProvider(
      create: (_) => WalletProvider()..loadWallet(),
      child: MaterialApp(
        debugShowCheckedModeBanner: false,
        home: Scaffold(
          backgroundColor: Colors.grey[100],
          appBar: AppBar(title: Text("Wallet Overview")),
          body: Center(child: WalletCard()),
        ),
      ),
    );
  }
}

class WalletCard extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    final provider = Provider.of<WalletProvider>(context);

    if (provider.isLoading) {
      return const Center(child: CircularProgressIndicator());
    }

    if (provider.error != null) {
      return Center(child: Text('Error: ${provider.error}'));
    }

    final walletItems = provider.items;

    return Card(
      margin: EdgeInsets.all(16),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      elevation: 4,
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            /// Title row
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: const [
                Text('My Wallet', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
                Icon(Icons.more_horiz),
              ],
            ),
            const SizedBox(height: 16),

            /// Meter bar
            Row(
              children: walletItems
                  .map((item) => Expanded(
                flex: (item.percent * 100).toInt(),
                child: Container(
                  height: 8,
                  color: item.color,
                ),
              ))
                  .toList(),
            ),
            const SizedBox(height: 20),

            /// Wallet list
            ...walletItems.map((item) => Padding(
              padding: const EdgeInsets.symmetric(vertical: 6),
              child: Row(
                children: [
                  Container(
                    width: 10,
                    height: 10,
                    decoration: BoxDecoration(
                      color: item.color,
                      shape: BoxShape.circle,
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: RichText(
                      text: TextSpan(
                        text: item.name,
                        style: const TextStyle(color: Colors.black, fontSize: 14),
                        children: [
                          TextSpan(
                            text: " (${(item.percent * 100).toInt()}%)",
                            style: const TextStyle(color: Colors.grey),
                          ),
                        ],
                      ),
                    ),
                  ),
                  Text(
                    '${item.symbol} ${item.value}',
                    style: const TextStyle(fontWeight: FontWeight.w500),
                  )
                ],
              ),
            )),
          ],
        ),
      ),
    );
  }
}

const List<Color> kWalletColors = [
  Color(0xFF4DB6AC), // Teal
  Color(0xFF42AFA9),
  Color(0xFF37A7A5),
  Color(0xFF2D9FA0),
  Color(0xFF24989B),
  Color(0xFF2A7AAE), // Blue
];
import 'package:flutter/material.dart';

/// Generate a list of lighter and darker variations of a base color
List<Color> generateColorPalette(Color baseColor, {int count = 6}) {
  final List<Color> palette = [];

  final hslBase = HSLColor.fromColor(baseColor);

  for (int i = 0; i < count; i++) {
    final factor = (i - (count / 2)).toDouble() / count;
    final hslAdjusted = hslBase.withLightness(
      (hslBase.lightness + factor).clamp(0.1, 0.9),
    );
    palette.add(hslAdjusted.toColor());
  }

  return palette;
}
Color pastelize(Color color) {
  return Color.fromARGB(
    255,
    (color.red + 255) ~/ 2,
    (color.green + 255) ~/ 2,
    (color.blue + 255) ~/ 2,
  );
}
final _random = Random();

Color getRandomColor() {
  return Color.fromARGB(
    255,
    _random.nextInt(256),
    _random.nextInt(256),
    _random.nextInt(256),
  );
}
const List<Color> kWalletColors = [
  Color(0xFFF97316), // Orange
  Color(0xFF6466F1), // Indigo
  Color(0xFF3C81F6), // Blue
  Color(0xFF22C45F), // Green
  Color(0xFFA855F7), // Purple
  Color(0xFFEAB30C), // Yellow
  Color(0xFF17B8A6), // Teal
];
*/