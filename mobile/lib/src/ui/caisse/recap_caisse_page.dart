import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:mobile/src/data/services/recap_caisse/recap_caisse_service.dart';
import 'package:mobile/src/models/caisse/recap_caisse.dart';
import 'package:mobile/src/models/caisse/user_caisse_recap.dart';
import 'package:mobile/src/utils/card_builder.dart';
import 'package:mobile/src/utils/constant.dart';
import 'package:mobile/src/utils/custom_app_bar.dart';
import 'package:mobile/src/utils/date_range_state.dart';
import 'package:mobile/src/utils/filter_params_utils.dart';
import 'package:mobile/src/utils/service_state_wrapper.dart';

class RecapCaissePage extends StatefulWidget {
  const RecapCaissePage({Key? key});

  @override
  State<RecapCaissePage> createState() => _RecapCaisseState();
}

class _RecapCaisseState extends State<RecapCaissePage> {

  bool _onlyVente = false;

  @override
  void initState() {
    super.initState();

    WidgetsBinding.instance.addPostFrameCallback((_) {
      final dateRangeState = Provider.of<DateRangeState>(context, listen: false);
      _fetchData(dateRangeState.selectedFromDate, dateRangeState.selectedToDate);
      dateRangeState.addListener(_onDateRangeChanged);
    });
  }
 @override
 void dispose() {
   if (mounted) { // Or check if a Provider reference is held
     Provider.of<DateRangeState>(context, listen: false).removeListener(_onDateRangeChanged);
   }
   super.dispose();
 }

  Future<void> _fetchData(DateTime from,DateTime to) async {
    if (!mounted) return;
    final recapCaisseService = context.read<RecapCaisseService>();
    final String fromDateString = Constant.datePattern.format(from);
    final String toDateString = Constant.datePattern.format(to);
    await recapCaisseService.fetch(fromDateString, toDateString, _onlyVente);
  }

 void _onDateRangeChanged() {
   if (mounted) {
     final dateRangeState = Provider.of<DateRangeState>(context, listen: false);
     _fetchData(dateRangeState.selectedFromDate, dateRangeState.selectedToDate);
   }
 }
  void _showEnhancedFilterOptions() {
    final dateRangeState = Provider.of<DateRangeState>(context, listen: false);

    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      shape: Constant.roundedRectangleBorder,
      builder: (BuildContext bottomSheetContext) {
        return FilterParamsUtils(
          initialFromDate: dateRangeState.selectedFromDate,
          initialToDate: dateRangeState.selectedToDate,
          firstDate: Constant.firstDate,
          lastDate: Constant.lastDate,
          sheetTitleText: Constant.selectPeriodeOptions,
          onDateRangeSelected: (newFromDate, newToDate) {
          },
          onApplyAll: () {
            if (!mounted) return;
            final latestDates = Provider.of<DateRangeState>(context, listen: false);
            _fetchData(latestDates.selectedFromDate, latestDates.selectedToDate);
          },

          leadingWidgets: [

            StatefulBuilder( // To update checkbox within the sheet
              builder: (BuildContext sbfContext, StateSetter sbfSetState) {
                return CheckboxListTile(
                  title: const Text(Constant.onlyVenteText),
                  value: _onlyVente,

                  onChanged: (bool? newValue) {
                    sbfSetState(() { // Update UI within the bottom sheet
                      _onlyVente = newValue ?? false;
                    });
                    // Also update the page's state so it persists
                    setState(() {
                      _onlyVente = newValue ?? false;
                    });
                  },
                  controlAffinity: ListTileControlAffinity.leading,
                  dense: false,
                  contentPadding: EdgeInsets.zero,
                );
              },
            ),
          ],

        );
      },
    );
  }


  @override
  Widget build(BuildContext context) {
    final dateRangeState = context.watch<DateRangeState>();
    return Scaffold(
      appBar: CustomAppBar(
        fromDate: dateRangeState.selectedFromDate,
        toDate: dateRangeState.selectedToDate,
        onDateRangeTap: _showEnhancedFilterOptions,
        onRefreshTap: () => _fetchData(dateRangeState.selectedFromDate, dateRangeState.selectedToDate),
      ),

      body: Column(
        children: [
          Expanded(
            child: Consumer<RecapCaisseService>(
              builder: (context, recapCaisseService, child) {
                return ServiceStateWrapper<RecapCaisseService, RecapCaisse>(
                  service: recapCaisseService,
                  data: recapCaisseService.recapCaisse,
                  // Pass the actual data object
                  successBuilder:
                      (BuildContext context, RecapCaisse recapCaisse) {
                        return RefreshIndicator(
                          onRefresh: () => _fetchData(dateRangeState.selectedFromDate, dateRangeState.selectedToDate),
                          // Call the method that uses current dates
                          child: ListView.builder(
                            padding: const EdgeInsets.all(8.0),
                            itemCount: recapCaisse.items.length,
                            itemBuilder: (BuildContext context, int index) {
                              final UserCaisseRecap userRecap =
                                  recapCaisse.items[index];
                              return buildDataCard(
                                context: context,
                                title: userRecap.title,
                                data: userRecap.items,
                                onTap: () {},
                                resume: userRecap.resume,
                              );
                            },
                          ),
                        );
                      },
                );
              },
            ),
          ),
        ],
      ),
    );
  }
}
