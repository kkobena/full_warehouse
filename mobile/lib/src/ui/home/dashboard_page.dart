
import 'package:provider/provider.dart';
import 'package:mobile/src/data/services/dahboard/dashboard_sale_service.dart';
import 'package:mobile/src/models/dashboard.dart';
import 'package:mobile/src/models/list_item.dart';
import 'package:flutter/material.dart';
import 'package:mobile/src/utils/card_builder.dart';
import 'package:mobile/src/utils/constant.dart';
import 'package:mobile/src/utils/custom_app_bar.dart';
import 'package:mobile/src/utils/date_range_state.dart';
import 'package:mobile/src/utils/filter_params_utils.dart';
import 'package:mobile/src/utils/metter_group_builder.dart';

class DashboardPage extends StatefulWidget {
  const DashboardPage({
    Key? key,
  }) ;

  @override
  State<DashboardPage> createState() => _DashboardPageState();
}

class _DashboardPageState extends State<DashboardPage> {
 // DateTime _selectedDate = DateTime.now();
  late DateTime _fromDate;

  late DateTime _toDate;
  Future<void> _fetchData() async {
    if (!mounted) return;
    final dashboardSaleService = context.read<DashboardSaleService>();
    final String fromDateString = Constant.datePattern.format(_fromDate);
    final String toDateString = Constant.datePattern.format(_toDate);
    await dashboardSaleService.fetchDashboard(fromDateString, toDateString);
  }


  void _showDateRangePicker() {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      shape: Constant.roundedRectangleBorder,
      builder: (BuildContext bottomSheetContext) {
        return FilterParamsUtils(
          initialFromDate: _fromDate,
          initialToDate: _toDate,
          firstDate: Constant.firstDate,
          lastDate: Constant.lastDate,
          sheetTitleText: Constant.selectPeriodeText,
          onDateRangeSelected: (newFromDate, newToDate) {
            if (!mounted) return;
            setState(() {
              _fromDate = newFromDate;
              _toDate = newToDate;
            });

          },
          onApplyAll: () {
            _fetchData();
          },
        );
      },
    );
  }
  @override
  void initState() {
    super.initState();
    final dateRangeState = Provider.of<DateRangeState>(context, listen: false);
    _fromDate = dateRangeState.selectedFromDate;
    _toDate = dateRangeState.selectedToDate;
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _fetchData();
    });
  }

  @override
  Widget build(BuildContext context) {

    return Scaffold(
      appBar:  CustomAppBar( // Using the shared AppBar
          fromDate: _fromDate,
          toDate: _toDate,
          onDateRangeTap: _showDateRangePicker,
          onRefreshTap: _fetchData
      ),
      body: Consumer<DashboardSaleService>(
        builder: (context, service, child) {
          if (service.isLoading) {
            return const Center(child: CircularProgressIndicator());
          }
          if (service.errorMessage.isNotEmpty) {
            return Center(
              child: Text(
                service.errorMessage,
                style: const TextStyle(color: Colors.red),
              ),
            );
          }

          return SingleChildScrollView(
            physics: const ClampingScrollPhysics(
              parent: AlwaysScrollableScrollPhysics(),
            ),
            child: Padding(
              padding: const EdgeInsets.all(8.0),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: _buildChildren(context, service.dashboard),
              ),
            ),
          );
        },
      ) /*,
      floatingActionButton: FloatingActionButton(
        onPressed: () => Provider.of<DashboardSaleService>(
          context,
          listen: false,
        ).fetchDashboard(),
        child: const Icon(Icons.refresh),
      ),*/,
    );
  }

  List<Widget> _buildChildren(BuildContext context, Dashboard? dashboard) {
    List<Widget> children = [];
    if (dashboard == null) {
      return children; // Return empty list if dashboard is null
    }

    void addMetterGroup(String title, List<ListItem>? dataList, VoidCallback? onTap) {
      if (dataList != null && dataList.isNotEmpty) {
        children.add(
          buildMetterGroup(
            context: context,
            title: title,
            data: dataList,
            onTap: onTap,
          ),
        );
      }
    }



    void addCard(String title, List<ListItem>? dataList, VoidCallback? onTap) {
      if (dataList != null && dataList.isNotEmpty) {
        children.add(
          buildDataCard(
            context: context,
            title: title,
            data: dataList,
            onTap: onTap,
          ),
        );
      }
    }

    addCard(
      'Ventes',
      dashboard.sales,
      () => {},
      //  () => Navigator.pushNamed(context, '/sales'),
    );
    addCard(
      'Montants Nets',
      dashboard.netAmounts,
      () => Navigator.pushNamed(context, '/sales'),
    );
    addCard('Types de Vente', dashboard.salesTypes, () => {});
    addMetterGroup('Modes de Paiement', dashboard.paymentModes, () => {});
    addCard('Commandes', dashboard.commandes, () => {});

    return children;
  }
}
