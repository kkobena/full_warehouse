import 'package:flutter/material.dart';

class StockPage extends StatelessWidget {
  static const String routeName = '/stock';

  const StockPage({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      /* appBar: AppBar(
        title: Text('Stock Management'),
      ),*/
      body: Center(
        child: Text(
          'Page de gestion des stocks',
          style: TextStyle(fontSize: 24),
        ),
      ),
    );
  }
}
