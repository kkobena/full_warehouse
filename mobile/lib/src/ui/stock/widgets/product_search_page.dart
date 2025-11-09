import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:mobile/src/data/services/produit/produit_service.dart';
import 'package:mobile/src/models/list_item.dart';
import 'package:mobile/src/models/produit/etat_produit.dart';
import 'package:mobile/src/models/produit/produit.dart';
import 'package:mobile/src/utils/constant.dart';

class ProductSearchPage extends StatefulWidget {
  static const String routeName = '/produit';

  const ProductSearchPage({super.key});

  @override
  State<ProductSearchPage> createState() => _ProductSearchPageState();
}

class _ProductSearchPageState extends State<ProductSearchPage> {
  Produit? _selectedProduit; // To store the selected product for detail display
  final TextEditingController _textEditingController = TextEditingController();
  final FocusNode _focusNode = FocusNode();

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      // Initialize or fetch data here if needed
    });
  }

  @override
  void dispose() {
    _textEditingController.dispose();
    _focusNode.dispose();
    super.dispose();
  }

  Widget _buildProduitSuggestionTile(
    BuildContext context,
    Produit produit,
    VoidCallback onTap,
  ) {
    final Color? itemTextColor = Theme.of(context).textTheme.bodyMedium?.color;
    Color? quantityTextColor = itemTextColor; // Default text color

    if (produit.totalQuantity != null && produit.totalQuantity! < 0) {
      quantityTextColor = Colors.red[300] ?? Colors.red;
    } else if (produit.totalQuantity != null &&
        produit.qtySeuilMini != null &&
        produit.totalQuantity! < produit.qtySeuilMini!) {
      quantityTextColor = Colors.orange[300] ?? Colors.orange;
    }

    return InkWell(
      onTap: onTap,
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 5.0),
        child: Container(
          // color: rowBackgroundColor, // Apply background color here if you want the whole row colored
          padding: const EdgeInsets.all(8.0), // Increased padding a bit
          child: Row(
            children: [
              Expanded(
                flex: 3,
                child: Text(
                  produit.libelle,
                  style: TextStyle(fontSize: 13, color: itemTextColor),
                  overflow: TextOverflow.ellipsis,
                ),
              ),
              const SizedBox(width: 5),
              Expanded(
                flex: 1, // Adjusted flex for quantity, if you add it
                child: Text(
                  Constant.formatNumber(produit.totalQuantity),
                  // Display quantity
                  textAlign: TextAlign.end,
                  style: TextStyle(
                    fontSize: 13,
                    fontWeight: FontWeight.w600,
                    color:
                        quantityTextColor, // Use the dynamic color for quantity
                  ),
                  overflow: TextOverflow.ellipsis,
                ),
              ),
              const SizedBox(width: 8),
              Expanded(
                flex: 2,
                child: Text(
                  produit.codeCip ?? '',
                  textAlign: TextAlign.end,
                  style: TextStyle(
                    fontSize: 13,
                    fontWeight: FontWeight.w600,
                    color: itemTextColor,
                  ),
                  overflow: TextOverflow.ellipsis,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final produitService = Provider.of<ProduitService>(context, listen: false);

    return Scaffold(
      appBar: AppBar(title: Text(Constant.produitPageTitle)),
      body: Padding(
        padding: const EdgeInsets.all(4.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: <Widget>[
            Autocomplete<Produit>(
              displayStringForOption: (Produit option) =>
                  '${option.libelle} (${option.codeCip ?? ''})',

              optionsBuilder: (TextEditingValue textEditingValue) async {
                if (textEditingValue.text.trim().isEmpty) {
                  return const Iterable<Produit>.empty();
                }
                final String searchTerm = textEditingValue.text.trim();
                if (searchTerm.length < Constant.searchTermLenght) {
                  return const Iterable<Produit>.empty();
                }
                await produitService.fetchAll(searchTerm);
                if (produitService.errorMessage.isNotEmpty) {
                  return const Iterable<Produit>.empty();
                }
                return produitService.items ?? const Iterable<Produit>.empty();
              },
              onSelected: (Produit selection) {
                setState(() {
                  _selectedProduit = selection;
                  _textEditingController.text =
                      selection.libelle; // Keep or update as needed
                  _focusNode.unfocus();
                });
              },
              fieldViewBuilder:
                  (
                    BuildContext context,
                    TextEditingController fieldTextEditingController,
                    FocusNode fieldFocusNode,
                    VoidCallback onFieldSubmitted,
                  ) {
                    return TextField(
                      controller: fieldTextEditingController, // USE THIS
                      focusNode: fieldFocusNode, // USE THIS
                      decoration: InputDecoration(
                        hintText: Constant.produitSearchInputPlaceholder,
                        labelText: Constant.produitSearchInputLabel,
                        border: OutlineInputBorder(),
                        suffixIcon: fieldTextEditingController.text.isNotEmpty
                            ? IconButton(
                                icon: Icon(Icons.clear),
                                onPressed: () {
                                  fieldTextEditingController.clear();
                                  setState(() {
                                    _selectedProduit = null;
                                  });
                                },
                              )
                            : null,
                      ),
                      onSubmitted: (String value) {
                        onFieldSubmitted();
                      },
                    );
                  },
              optionsViewBuilder:
                  (
                    BuildContext context,
                    AutocompleteOnSelected<Produit> onSelected,
                    // This is the callback to use
                    Iterable<Produit> options,
                  ) {
                    return Align(
                      alignment: Alignment.topLeft,
                      child: Material(
                        elevation: 2.0, // Increased elevation slightly
                        child: ConstrainedBox(
                          constraints: BoxConstraints(
                            maxHeight: MediaQuery.of(context).size.height * 0.3,
                          ), // Responsive max height
                          child: ListView.builder(
                            padding: EdgeInsets.zero,
                            shrinkWrap: true,
                            // Add this if not already constrained by parent
                            itemCount: options.length,
                            itemBuilder: (BuildContext context, int index) {
                              final Produit option = options.elementAt(index);
                              return _buildProduitSuggestionTile(
                                context,
                                option,
                                () => onSelected(option),
                              );
                            },
                          ),
                        ),
                      ),
                    );
                  },
            ),
            const SizedBox(height: 6),
            if (_selectedProduit != null)
              Expanded(child: ProduitDetailWidget(produit: _selectedProduit!))
            else
              Expanded(
                // Use Expanded here too for consistent layout if needed
                child: Center(
                  child: Padding(
                    padding: const EdgeInsets.all(15.0),
                    child: Text(
                      Constant.produitDetailText,
                      style: Theme.of(context).textTheme.titleMedium,
                      textAlign: TextAlign.center,
                    ),
                  ),
                ),
              ),
          ],
        ),
      ),
    );
  }
}

class ProduitDetailWidget extends StatelessWidget {
  final Produit produit;

  const ProduitDetailWidget({Key? key, required this.produit});

  @override
  Widget build(BuildContext context) {
    final Color titleColor =
        Theme.of(context).textTheme.titleLarge?.color ??
        Theme.of(context).colorScheme.onSurface;

    Color? rowColor = Theme.of(
      context,
    ).textTheme.bodyMedium?.color; // Default color
    if (produit.totalQuantity != null && produit.totalQuantity! < 0) {
      rowColor = Colors.red[300] ?? Colors.red; // Light red for negative stock
    } else if (produit.totalQuantity != null &&
        produit.qtySeuilMini != null &&
        produit.totalQuantity! < produit.qtySeuilMini!) {
      rowColor =
          Colors.orange[300] ??
          Colors.orange; // Light orange for below minimum threshold
    }

    return SingleChildScrollView(
      physics: const ClampingScrollPhysics(
        parent: AlwaysScrollableScrollPhysics(),
      ),
      child: Padding(
        padding: const EdgeInsets.all(0.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: <Widget>[
            _buildProduitInfo(context, rowColor!, titleColor),
            _buildStocks(context, titleColor, rowColor),
            _buildProduitEtatMetterGroup(context: context),
            _buildGrossiste(context, titleColor, rowColor),
            _buildRayons(context, titleColor, rowColor),
          ],
        ),
      ),
    );
  }

  List<Widget> _buildStockRow(String label, int? stock, Color rowColor) {
    return [
      Padding(
        padding: const EdgeInsets.symmetric(vertical: 4.0),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            SizedBox(
              width: 100,
              child: Text(label, style: TextStyle(fontWeight: FontWeight.bold)),
            ),
            Expanded(
              child: Text(
                Constant.formatNumber(stock),
                textAlign: TextAlign.end,
                style: TextStyle(fontWeight: FontWeight.bold, color: rowColor),
              ),
            ),
          ],
        ),
      ),
      Divider(),
    ];
  }

  List<Widget> _buildDetailRow(String label, String? value) {
    if (value == null || value.isEmpty) {
      return [SizedBox.shrink()]; // Return empty widget if value is empty
    }
    return <Widget>[
      Padding(
        padding: const EdgeInsets.symmetric(vertical: 4.0),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Expanded(
              child: Text(label, style: TextStyle(fontWeight: FontWeight.bold)),
            ),
            Expanded(child: Text(value, textAlign: TextAlign.end)),
          ],
        ),
      ),
      Divider(),
    ];
  }

  Widget _buildProduitInfo(
    BuildContext context,
    Color rowColor,
    Color titleColor,
  ) {
    final Color circleAvatarForegroundColor =
        Constant.getCircleAvatarForegroundColor(context);

    final Color circleAvatarBackgroundColor =
        Constant.getCircleAvatarBackgroundColor(context);
    final String firstLetter = Constant.getFirstLetter(produit.libelle);
    return Card(
      elevation: 0.2,
      shape: Constant.roundedRectangleBorder,
      margin: const EdgeInsets.all(2.0),
      child: Padding(
        padding: const EdgeInsets.all(8.0),
        child: SingleChildScrollView(
          // Make details scrollable if they are long
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisSize: MainAxisSize.min,
            children: <Widget>[
              Row(
                crossAxisAlignment: CrossAxisAlignment.center,
                children: [
                  CircleAvatar(
                    radius: 20,
                    backgroundColor: circleAvatarBackgroundColor,
                    child: Text(
                      firstLetter,
                      style: TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.bold,
                        color: circleAvatarForegroundColor,
                      ),
                    ),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: Text(
                      produit.libelle,
                      style: TextStyle(
                        fontSize: 15,
                        fontWeight: FontWeight.bold,
                        color: titleColor,
                      ),
                      overflow: TextOverflow.ellipsis,
                      maxLines: 2,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 8),
              Divider(
                height: 1,
                color: Theme.of(context).dividerColor.withValues(alpha: 0.5),
              ),
              const SizedBox(height: 8),

              ..._buildDetailRow(Constant.cip, produit.codeCip),
              ..._buildStockRow(
                Constant.qteStockTotal,
                produit.totalQuantity,
                rowColor,
              ),

              ..._buildDetailRow(
                Constant.qteReserve,
                Constant.formatNumber(produit.qtyReserve),
              ),

              ..._buildDetailRow(
                Constant.prixAchat,
                Constant.formatNumber(produit.costAmount),
              ),

              ..._buildDetailRow(
                Constant.prixVente,
                Constant.formatNumber(produit.regularUnitPrice),
              ),
              ..._buildDetailRow(
                Constant.qteSeuil,
                Constant.formatNumber(produit.qtySeuilMini),
              ),
              ..._buildDetailRow(
                Constant.qteReap,
                Constant.formatNumber(produit.qtyAppro),
              ),
              ..._buildDetailRow(
                Constant.datePeremption,
                produit.perimeAtFormatted,
              ),
              ..._buildDetailRow(
                Constant.lastDateOfSale,
                produit.lastDateOfSaleFormatted,
              ),
              ..._buildDetailRow(
                Constant.lastOrderDate,
                produit.lastOrderDateFormatted,
              ),
              ..._buildDetailRow(
                Constant.lastInventoryDate,
                produit.lastInventoryDateFormatted,
              ),

              ..._buildDetailRow(
                Constant.tvaCode,
                Constant.formatNumber(produit.tvaTaux),
              ),

              ..._buildDetailRow("Rayon", produit.rayonLibelle),

              ..._buildDetailRow(
                Constant.laboratoire,
                produit.laboratoireLibelle,
              ),

              ..._buildDetailRow(Constant.gamme, produit.gammeLibelle),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildStocks(BuildContext context, Color titleColor, Color rowColor) {
    if (produit.stockProduits == null ||
        produit.stockProduits!.isEmpty ||
        produit.stockProduits!.length < 2) {
      return SizedBox.shrink();
    }
    final Color circleAvatarForegroundColor =
        Constant.getCircleAvatarForegroundColor(context);
    final Color circleAvatarBackgroundColor =
        Constant.getCircleAvatarBackgroundColor(context);
    final String firstLetter = Constant.getFirstLetter(Constant.produitStock);
    return Card(
      elevation: 0.2,
      shape: Constant.roundedRectangleBorder,
      child: Padding(
        padding: const EdgeInsets.all(12.0),
        child: Column(
          // This Column should also try to be as small as possible
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisSize: MainAxisSize.min, // **** ADD THIS ****
          children: <Widget>[
            Row(
              crossAxisAlignment: CrossAxisAlignment.center,
              children: [
                CircleAvatar(
                  radius: 20,
                  backgroundColor: circleAvatarBackgroundColor,
                  child: Text(
                    firstLetter,
                    style: TextStyle(
                      fontSize: 18,
                      fontWeight: FontWeight.bold,
                      color: circleAvatarForegroundColor,
                    ),
                  ),
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: Text(
                    Constant.produitStock,
                    style: TextStyle(
                      fontSize: 18,
                      fontWeight: FontWeight.bold,
                      color: titleColor,
                    ),
                    overflow: TextOverflow.ellipsis,
                    maxLines: 2,
                  ),
                ),
              ],
            ),

            const SizedBox(height: 8.0),
            Constant.getDivider(context),
            const SizedBox(height: 8),
            ListView.separated(
              shrinkWrap: true,
              physics: NeverScrollableScrollPhysics(),
              itemCount: produit.stockProduits!.length,
              itemBuilder: (BuildContext context, int index) {
                final stockDetail = produit.stockProduits![index];
                return Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  mainAxisSize: MainAxisSize.min,
                  children: <Widget>[
                    ..._buildDetailRow(
                      Constant.storageName,
                      stockDetail.storageName,
                    ),

                    ..._buildDetailRow(
                      Constant.storageType,
                      stockDetail.storageType,
                    ),

                    ..._buildStockRow(
                      // _buildStockRow also returns a Row
                      Constant.produitStock,
                      stockDetail.qtyStock,
                      rowColor, // This should be currentStockRowColor
                    ),
                  ],
                );
              },
              separatorBuilder: (BuildContext context, int index) =>
                  Constant.getDivider(context),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildGrossiste(
    BuildContext context,
    Color titleColor,
    Color rowColor,
  ) {
    if (produit.fournisseurProduits == null ||
        produit.fournisseurProduits!.isEmpty ||
        produit.fournisseurProduits!.length < 2) {
      return SizedBox.shrink();
    }
    final Color circleAvatarForegroundColor =
        Constant.getCircleAvatarForegroundColor(context);

    final Color circleAvatarBackgroundColor =
        Constant.getCircleAvatarBackgroundColor(context);
    final String firstLetter = Constant.getFirstLetter(Constant.fournisseurs);
    return Card(
      elevation: 0.2,
      shape: Constant.roundedRectangleBorder,
      // margin: const EdgeInsets.all(4.0), // Uncomment if you want margin
      child: Padding(
        padding: const EdgeInsets.all(12.0),
        // Increased padding for better spacing
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisSize: MainAxisSize.min,
          children: <Widget>[
            Row(
              crossAxisAlignment: CrossAxisAlignment.center,
              children: [
                CircleAvatar(
                  radius: 20,
                  backgroundColor: circleAvatarBackgroundColor,
                  child: Text(
                    firstLetter,
                    style: TextStyle(
                      fontSize: 18,
                      fontWeight: FontWeight.bold,
                      color: circleAvatarForegroundColor,
                    ),
                  ),
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: Text(
                    Constant.fournisseurs,
                    style: TextStyle(
                      fontSize: 18,
                      fontWeight: FontWeight.bold,
                      color: titleColor,
                    ),
                    overflow: TextOverflow.ellipsis,
                    maxLines: 2,
                  ),
                ),
              ],
            ),

            const SizedBox(height: 8.0),
            Constant.getDivider(context),
            const SizedBox(height: 8),
            ListView.separated(
              shrinkWrap: true,
              physics: NeverScrollableScrollPhysics(),
              itemCount: produit.fournisseurProduits!.length,
              itemBuilder: (BuildContext context, int index) {
                final fourDetails = produit.fournisseurProduits![index];
                return Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  mainAxisSize: MainAxisSize.min,
                  children: <Widget>[
                    ..._buildDetailRow(
                      Constant.fournisseur,
                      fourDetails.fournisseurLibelle,
                    ),
                    ..._buildDetailRow(Constant.cip, fourDetails.codeCip),
                    ..._buildDetailRow(
                      Constant.prixAchat,
                      Constant.formatNumber(fourDetails.prixAchat),
                    ),
                    ..._buildDetailRow(
                      Constant.prixVente,
                      Constant.formatNumber(fourDetails.prixUni),
                    ),
                  ],
                );
              },
              separatorBuilder: (BuildContext context, int index) =>
                  Constant.getDivider(context),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildRayons(BuildContext context, Color titleColor, Color rowColor) {
    if (produit.rayonProduits == null ||
        produit.rayonProduits!.isEmpty ||
        produit.rayonProduits!.length < 2) {
      return SizedBox.shrink();
    }
    final String firstLetter = Constant.getFirstLetter(Constant.rayons);
    final Color circleAvatarForegroundColor =
        Constant.getCircleAvatarForegroundColor(context);
    return Card(
      elevation: 0.2,
      shape: Constant.roundedRectangleBorder,
      // margin: const EdgeInsets.all(4.0), // Uncomment if you want margin
      child: Padding(
        padding: const EdgeInsets.all(12.0),
        // Increased padding for better spacing
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisSize: MainAxisSize.min,
          children: <Widget>[
            Row(
              crossAxisAlignment: CrossAxisAlignment.center,
              children: [
                CircleAvatar(
                  radius: 20,
                  backgroundColor: Constant.getCircleAvatarBackgroundColor(
                    context,
                  ),
                  child: Text(
                    firstLetter,
                    style: TextStyle(
                      fontSize: 18,
                      fontWeight: FontWeight.bold,
                      color: circleAvatarForegroundColor,
                    ),
                  ),
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: Text(
                    Constant.rayons,
                    style: TextStyle(
                      fontSize: 18,
                      fontWeight: FontWeight.bold,
                      color: titleColor,
                    ),
                    overflow: TextOverflow.ellipsis,
                    maxLines: 2,
                  ),
                ),
              ],
            ),

            const SizedBox(height: 8.0),
            Constant.getDivider(context),
            const SizedBox(height: 8),
            ListView.separated(
              shrinkWrap: true,
              physics: NeverScrollableScrollPhysics(),
              itemCount: produit.rayonProduits!.length,
              itemBuilder: (BuildContext context, int index) {
                final rayonDetails = produit.rayonProduits![index];
                return Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  mainAxisSize: MainAxisSize.min,
                  children: <Widget>[
                    ..._buildDetailRow(
                      Constant.rayon,
                      rayonDetails.libelleRayon,
                    ),
                    ..._buildDetailRow(
                      Constant.codeRayon,
                      rayonDetails.codeRayon,
                    ),
                    ..._buildDetailRow(
                      Constant.storageName,
                      rayonDetails.libelleStorage,
                    ),
                    ..._buildDetailRow(
                      Constant.storageType,
                      rayonDetails.storageType,
                    ),
                  ],
                );
              },
              separatorBuilder: (BuildContext context, int index) =>
                  Constant.getDivider(context),
            ),
          ],
        ),
      ),
    );
  }

  double _percentage(List<ListItem> data) {
    if (data.length == 3) {
      return 33.33;
    } else if (data.length == 2) {
      return 50.0;
    } else if (data.length == 1) {
      return 100.0;
    }
    return 0.0;
  }

  Widget _buildProduitEtatMetterGroup({required BuildContext context}) {
    final EtatProduit etatProduit = produit.etatProduit;
    if ((!etatProduit.enCommande &&
        !etatProduit.enSuggestion &&
        !etatProduit.entree)) {
      return SizedBox.shrink();
    }
    final List<ListItem> data = [
      if (etatProduit.enCommande)
        ListItem(libelle: Constant.inOrder, autre: '33', value: ''),
      if (etatProduit.enSuggestion)
        ListItem(libelle: Constant.inSuggestion, autre: '33', value: ''),
      if (etatProduit.entree)
        ListItem(libelle: Constant.inStock, autre: '33', value: ''),
    ];
    final double poucentageTotal = _percentage(data);
    String firstLetter = Constant.getFirstLetter(Constant.etatProduit);
    final Color circleAvatarForegroundColor =
        Constant.getCircleAvatarForegroundColor(context);
    final Color circleAvatarBackgroundColor =
        Constant.getCircleAvatarBackgroundColor(context);

    return Card(
      margin: const EdgeInsets.all(4.0),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8.0)),
      elevation: 0.2,
      clipBehavior: Clip.antiAlias,
      child: Padding(
        padding: const EdgeInsets.all(10.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              crossAxisAlignment: CrossAxisAlignment.center,
              children: [
                CircleAvatar(
                  radius: 20,
                  backgroundColor: circleAvatarBackgroundColor,
                  child: Text(
                    firstLetter,
                    style: TextStyle(
                      fontSize: 18,
                      fontWeight: FontWeight.bold,
                      color: circleAvatarForegroundColor,
                    ),
                  ),
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: Text(
                    Constant.etatProduit,
                    style: TextStyle(
                      fontSize: 18,
                      fontWeight: FontWeight.bold,
                      color:
                          Theme.of(context).textTheme.titleLarge?.color ??
                          Theme.of(context).colorScheme.onSurface,
                    ),
                    overflow: TextOverflow.ellipsis,
                    maxLines: 2,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 8),
            Divider(
              height: 1,
              color: Theme.of(context).dividerColor.withValues(alpha: 0.5),
            ),
            const SizedBox(height: 12),

            /// Meter bar
            Row(
              children: data.asMap().entries.map((entry) {
                int index = entry.key;
                Color itemColor = Constant
                    .pieChartColors[index % Constant.pieChartColors.length];

                return Expanded(
                  flex: (poucentageTotal * 100).toInt(),
                  child: Container(
                    height: 8,
                    color: itemColor, // Use the color from the list by index
                  ),
                );
              }).toList(),
            ),
            const SizedBox(height: 12),

            /// metter list
            ...data.asMap().entries.map((entry) {
              int index = entry.key;
              ListItem item = entry.value;
              Color itemColor = Constant
                  .pieChartColors[index % Constant.pieChartColors.length];
              return Padding(
                padding: const EdgeInsets.symmetric(vertical: 4.0),
                child: Row(
                  children: [
                    Container(
                      width: 10,
                      height: 10,
                      decoration: BoxDecoration(
                        color: itemColor,
                        shape: BoxShape.circle,
                      ),
                    ),
                    const SizedBox(width: 5),
                    Expanded(
                      child: RichText(
                        text: TextSpan(
                          text: item.libelle,
                          style: const TextStyle(
                            color: Colors.black,
                            fontSize: 14,
                          ),
                        ),
                      ),
                    ),
                  ],
                ),
              );
            }),
          ],
        ),
      ),
    );
  }
}
