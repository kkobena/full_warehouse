import 'package:fl_chart/fl_chart.dart';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:mobile/src/data/services/tva/tva_service.dart';
import 'package:mobile/src/models/tva.dart';
import 'package:mobile/src/models/tva_item.dart';
import 'package:mobile/src/utils/constant.dart';
import 'package:mobile/src/utils/custom_app_bar.dart';
import 'package:mobile/src/utils/date_range_state.dart';
import 'package:mobile/src/utils/filter_params_utils.dart';
import 'package:mobile/src/utils/service_state_wrapper.dart';

class TvaPage extends StatefulWidget {
  const TvaPage({Key? key});

  static const String routeName = '/tva';

  @override
  State<TvaPage> createState() => _TvaPageState();
}

class _TvaPageState extends State<TvaPage> {
  final List<Color> _colorPalette = Constant.pieChartColors;
  int? _touchedIndex; // For pie chart interactivity
  Color _getColorForPieSection(int index) {
    return _colorPalette[index % _colorPalette.length];
  }

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      final dateRangeState = Provider.of<DateRangeState>(
        context,
        listen: false,
      );
      _fetchData(
        dateRangeState.selectedFromDate,
        dateRangeState.selectedToDate,
      );
      dateRangeState.addListener(_onDateRangeChanged);
    });
  }

  List<PieChartSectionData> _generatePieSections(Tva tvaData) {
    if (tvaData.items.isEmpty) {
      return [];
    }

    double totalTtcAmount = 0;
    for (var item in tvaData.items) {
      final String cleanTvaString = item.ttc.replaceAll(' ', '');
      totalTtcAmount += double.tryParse(cleanTvaString) ?? 0.0;
    }

    if (totalTtcAmount == 0) return [];

    return tvaData.items.asMap().entries.map((entry) {
      final int index = entry.key;
      final TvaItem item = entry.value;
      final bool isTouched = index == _touchedIndex;
      final double fontSize = isTouched ? 18.0 : 14.0;
      final double radius = isTouched ? 110.0 : 100.0;
      final Color sectionColor = _getColorForPieSection(index);

      final String cleanItemTvaString = item.ttc.replaceAll(' ', '');
      final double itemTvaValue = double.tryParse(cleanItemTvaString) ?? 0.0;
      final double percentage = (itemTvaValue / totalTtcAmount) * 100;

      return PieChartSectionData(
        color: sectionColor,
        value: itemTvaValue,
        // The actual value for the section
        title: '${item.code}% (${percentage.toStringAsFixed(1)}%)',
        // Display TVA code and its percentage
        radius: radius,
        titleStyle: TextStyle(
          fontSize: fontSize,
          fontWeight: FontWeight.bold,
          color:
              ThemeData.estimateBrightnessForColor(sectionColor) ==
                  Brightness.dark
              ? Colors.white
              : Colors.black54, // Adjust text color for contrast
          shadows: const [Shadow(color: Colors.black26, blurRadius: 2)],
        ),
      );
    }).toList();
  }

  Future<void> _fetchData(DateTime from, DateTime to) async {
    if (!mounted) return;
    final tvaService = context.read<TvaService>();
    final String fromDateString = Constant.datePattern.format(from);
    final String toDateString = Constant.datePattern.format(to);
    await tvaService.fetch(fromDateString, toDateString);
  }

  @override
  void dispose() {
    if (mounted) {
      Provider.of<DateRangeState>(
        context,
        listen: false,
      ).removeListener(_onDateRangeChanged);
    }
    super.dispose();
  }

  void _onDateRangeChanged() {
    if (mounted) {
      // Ensure widget is still in the tree
      final dateRangeState = Provider.of<DateRangeState>(
        context,
        listen: false,
      );
      _fetchData(
        dateRangeState.selectedFromDate,
        dateRangeState.selectedToDate,
      );
    }
  }

  void _showDateRangePicker() {
    final dateRangeState = Provider.of<DateRangeState>(context, listen: false);
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      // Important for accommodating content and keyboard
      shape: Constant.roundedRectangleBorder,
      builder: (BuildContext bottomSheetContext) {
        return FilterParamsUtils(
          initialFromDate: dateRangeState.selectedFromDate,
          initialToDate: dateRangeState.selectedToDate,
          firstDate: Constant.firstDate,
          lastDate: Constant.lastDate,
          sheetTitleText: Constant.selectPeriodeText,
          onDateRangeSelected: (newFromDate, newToDate) {},
          onApplyAll: () {
            if (!mounted) return;
            final latestDates = Provider.of<DateRangeState>(
              context,
              listen: false,
            );
            _fetchData(
              latestDates.selectedFromDate,
              latestDates.selectedToDate,
            );
          },
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    final TextTheme textTheme = Theme.of(context).textTheme;
    final dateRangeState = context.watch<DateRangeState>();
    return Scaffold(
      appBar: CustomAppBar(
        // Using the shared AppBar
        fromDate: dateRangeState.selectedFromDate,
        toDate: dateRangeState.selectedToDate,
        onDateRangeTap: _showDateRangePicker,
        onRefreshTap: () => _fetchData(
          dateRangeState.selectedFromDate,
          dateRangeState.selectedToDate,
        ),
      ),
      body: Column(
        children: [
          Expanded(
            child: Consumer<TvaService>(
              builder: (context, tvaService, child) {
                return ServiceStateWrapper<TvaService, Tva>(
                  service: tvaService,
                  data: tvaService.tva,
                  customEmptyDataMessage:
                      'Aucune donnée pour la période sélectionnée.',
                  successBuilder: (BuildContext context, Tva tvaData) {
                    final pieSections = _generatePieSections(tvaData);

                    return RefreshIndicator(
                      onRefresh: () => _fetchData(
                        dateRangeState.selectedFromDate,
                        dateRangeState.selectedToDate,
                      ),
                      child: LayoutBuilder(
                        // Use LayoutBuilder to get constraints for responsive sizing
                        builder: (context, constraints) {
                          // Determine if we should show chart and table side-by-side or stacked
                          bool isWideScreen =
                              constraints.maxWidth > 600; // Example breakpoint

                          if (isWideScreen) {
                            // Side-by-side layout for wider screens
                            return Row(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Expanded(
                                  flex: 2, // Give more space to the table
                                  child: _buildDataTable(
                                    tvaData,
                                    constraints,
                                    textTheme,
                                  ),
                                ),

                                const VerticalDivider(width: 1, thickness: 1),
                                Expanded(
                                  flex: 3,
                                  // Give more space to the chart or adjust as needed
                                  child: _buildPieChartCard(
                                    pieSections,
                                    textTheme,
                                  ),
                                ),
                              ],
                            );
                          } else {
                            // Stacked layout for narrower screens
                            return SingleChildScrollView(
                              padding: const EdgeInsets.all(4.0),
                              child: Column(
                                children: [
                                  _buildDataTable(
                                    tvaData,
                                    constraints,
                                    textTheme,
                                  ),
                                  const SizedBox(height: 8.0),
                                  // Spacing between table and chart
                                  _buildPieChartCard(pieSections, textTheme),
                                ],
                              ),
                            );
                          }
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

  Widget _buildDataTable(
    Tva tvaData,
    BoxConstraints constraints,
    TextTheme textTheme,
  ) {
    // Added textTheme for potential title
    return Card(
      elevation: 0.2,
      // You can adjust elevation
      margin: const EdgeInsets.all(2),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
      clipBehavior: Clip.antiAlias,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        // Make column children take full width
        children: [
          Padding(
            // Padding for the title
            padding: const EdgeInsets.only(
              top: 4.0,
              left: 4.0,
              right: 4.0,
              bottom: 8.0,
            ),
            child: Text(
              "Détail des Données TVA",
              style: textTheme.titleLarge?.copyWith(
                fontWeight: FontWeight.bold,
              ),
              textAlign: TextAlign.center,
            ),
          ),
          Divider(
            height: 1,
            color: Theme.of(context).dividerColor.withValues(alpha: 0.5),
          ),
          // The existing DataTable structure, now a child of the Column
          SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            child: ConstrainedBox(
              constraints: BoxConstraints(
                minWidth: constraints.maxWidth < 600
                    ? constraints.maxWidth
                    : constraints.maxWidth / 2,
              ),
              child: DataTable(
                headingRowHeight: 38.0,
                dataRowMinHeight: 38.0,
                dataRowMaxHeight: 60.0,
                columnSpacing: 10.0,
                dividerThickness: 1,
                showCheckboxColumn: false,
                columns: const <DataColumn>[
                  DataColumn(
                    label: SizedBox(
                      width: 35,
                      child: Text(
                        'Code',
                        textAlign: TextAlign.left,
                        style: TextStyle(fontWeight: FontWeight.bold),
                      ),
                    ),
                  ),
                  DataColumn(
                    label: Text(
                      'TTC',
                      style: TextStyle(fontWeight: FontWeight.bold),
                      textAlign: TextAlign.right,
                    ),
                    numeric: true,
                  ),
                  DataColumn(
                    label: Text(
                      'TVA',
                      style: TextStyle(fontWeight: FontWeight.bold),
                      textAlign: TextAlign.right,
                    ),
                    numeric: true,
                  ),
                  DataColumn(
                    label: Text(
                      'HT',
                      style: TextStyle(fontWeight: FontWeight.bold),
                      textAlign: TextAlign.right,
                    ),
                    numeric: true,
                  ),
                ],
                rows: tvaData.items.map((item) {
                  return DataRow(
                    cells: <DataCell>[
                      DataCell(
                        SizedBox(
                          width: 35, // Forcing width for the cell
                          child: Text(
                            textAlign: TextAlign.left,
                            item.code.toString(),
                          ),
                        ),
                      ),

                      DataCell(
                        Align(
                          alignment: Alignment.centerRight,
                          child: Text(item.ttc),
                        ),
                      ),
                      DataCell(
                        Align(
                          alignment: Alignment.centerRight,
                          child: Text(item.tva),
                        ),
                      ),
                      DataCell(
                        Align(
                          alignment: Alignment.centerRight,
                          child: Text(item.ht),
                        ),
                      ),
                    ],
                  );
                }).toList(),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildLegend(List<PieChartSectionData> sections, TextTheme textTheme) {
    return Wrap(
      // Use Wrap for a responsive legend
      spacing: 16.0,
      runSpacing: 8.0,
      alignment: WrapAlignment.center,
      children: sections.map((section) {
        // Extracting code from title for legend: 'CODE% (PERCENTAGE%)'
        final titleParts = section.title.split(' ');
        final codeText = titleParts.isNotEmpty ? titleParts.first : '';

        return Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Container(width: 16, height: 16, color: section.color),
            const SizedBox(width: 6),
            Text('TVA $codeText', style: textTheme.bodyMedium),
          ],
        );
      }).toList(),
    );
  }

  Widget _buildPieChartCard(
    List<PieChartSectionData> pieSections,
    TextTheme textTheme,
  ) {
    return Card(
      elevation: 0.2,
      margin: const EdgeInsets.all(2),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
      clipBehavior: Clip.antiAlias,
      child: SingleChildScrollView(
        padding: const EdgeInsets.all(2.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text(
              "Montant TTC", // Chart Title
              style: textTheme.titleLarge?.copyWith(
                fontWeight: FontWeight.bold,
              ),
              textAlign: TextAlign.center,
            ),
            Divider(
              height: 1,
              color: Theme.of(context).dividerColor.withValues(alpha: 0.5),
            ),
            const SizedBox(height: 12),
            AspectRatio(
              aspectRatio: 1.0, // Adjust aspect ratio as needed
              child: PieChart(
                // The PieChart widget itself
                PieChartData(
                  // All configuration goes into PieChartData
                  pieTouchData: PieTouchData(
                    touchCallback: (FlTouchEvent event, pieTouchResponse) {
                      setState(() {
                        if (!event.isInterestedForInteractions ||
                            pieTouchResponse == null ||
                            pieTouchResponse.touchedSection == null) {
                          _touchedIndex = -1;
                          return;
                        }
                        _touchedIndex = pieTouchResponse
                            .touchedSection!
                            .touchedSectionIndex;
                      });
                    },
                  ),
                  borderData: FlBorderData(show: false),
                  sectionsSpace: 2,
                  // Space between sections
                  centerSpaceRadius: 50,
                  // Radius of the center hole (0 for a full pie)
                  sections: pieSections,
                  startDegreeOffset: -90, // Start sections from the top
                ),
              ),
            ),
            const SizedBox(height: 12), // Potentially reduce spacing
            _buildLegend(pieSections, textTheme),
          ],
        ),
      ),
    );
  }
}
