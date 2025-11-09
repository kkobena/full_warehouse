import 'package:flutter/material.dart';

class BonPage extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text('Bon de commandes Management'),
      ),
      body: Center(
        child: Text(
          'Page de gestion des bons de commande',
          style: TextStyle(fontSize: 24),
        ),
      ),
    );
  }
}
