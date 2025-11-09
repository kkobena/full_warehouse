

import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';
import 'package:mobile/src/utils/constant.dart';
import 'package:mobile/src/utils/date_range_state.dart';

class FilterParamsUtils extends StatefulWidget{
  final DateTime initialFromDate;
  final DateTime initialToDate;
  final DateTime firstDate;
  final DateTime lastDate;
  final Function(DateTime fromDate, DateTime toDate) onDateRangeSelected;

  // --- NEW: Optional Widgets ---
  final List<Widget>? leadingWidgets;  // Widgets above the date pickers
  final List<Widget>? trailingWidgets; // Widgets below the date pickers but before action buttons

  // --- NEW: Optional Text for Buttons and Title ---
  final String? sheetTitleText;        // Overall title for the sheet
  final String? datePickerSectionTitleText; // Title specifically for the date section
  final String? applyButtonText;
  final String? cancelButtonText;
  final VoidCallback? onApplyAll;
  const FilterParamsUtils({
    super.key,
    required this.initialFromDate,
    required this.initialToDate,
    required this.firstDate,
    required this.lastDate,
    required this.onDateRangeSelected,
    // Initialize new optional parameters
    this.leadingWidgets,
    this.trailingWidgets,
    this.sheetTitleText,
    this.datePickerSectionTitleText, // Defaulted in build if null
    this.applyButtonText,      // Defaulted in build if null
    this.cancelButtonText,     // Defaulted in build if null
    this.onApplyAll,
  });

  @override
  State<FilterParamsUtils> createState() => _FilterParamsUtilsState();
}
class _FilterParamsUtilsState extends State<FilterParamsUtils> {
  late DateTime _selectedFromDate;
  late DateTime _selectedToDate;

  @override
  void initState() {
    super.initState();
    final dateRangeState = Provider.of<DateRangeState>(context, listen: false);

    _selectedFromDate = dateRangeState.selectedFromDate;
    _selectedToDate = dateRangeState.selectedToDate;
  }

  Future<void> _pickDate(BuildContext context, bool isFromDate) async {
    final dateRangeState = Provider.of<DateRangeState>(context, listen: false);
    final DateTime? picked = await showDatePicker(
      context: context,
      initialDate: isFromDate ? _selectedFromDate : _selectedToDate,
      firstDate: widget.firstDate,
      lastDate: widget.lastDate,
      locale: const Locale('fr', 'FR'), // Consider making this configurable
      helpText: isFromDate
          ? 'SÉLECTIONNER DATE DE DÉBUT' // Consider making this configurable
          : 'SÉLECTIONNER DATE DE FIN', // Consider making this configurable
      cancelText: widget.cancelButtonText ?? Constant.annuler, // Use provided or default
      confirmText: Constant.select, // Or make this configurable too
    );

    if (picked != null) {
      setState(() {
        if (isFromDate) {
          _selectedFromDate = picked;
          if (_selectedToDate.isBefore(_selectedFromDate)) {
            _selectedToDate = _selectedFromDate;
          }
        } else {
          _selectedToDate = picked;
          if (_selectedFromDate.isAfter(_selectedToDate)) {
            _selectedFromDate = _selectedToDate;
          }
        }

      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final DateFormat dateFormat = DateFormat('dd/MM/yyyy', 'fr_FR'); // Consider making locale part of widget params
    final ThemeData theme = Theme.of(context);
    final TextTheme textTheme = theme.textTheme;
    final ColorScheme colorScheme = theme.colorScheme;

    // Default texts if not provided
    final String actualSheetTitle = widget.sheetTitleText ?? Constant.selectPeriodeOptions ;
    final String actualApplyButtonText = widget.applyButtonText ?? Constant.valider;
    final String actualCancelButtonText = widget.cancelButtonText ?? Constant.annuler;

    return Padding(
      padding: EdgeInsets.only(
        top: 20,
        left: 16,
        right: 16,
        bottom: MediaQuery.of(context).viewInsets.bottom + 20,
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: <Widget>[
          // 1. Optional Overall Sheet Title
          if (widget.sheetTitleText != null) ...[
            Text(
              actualSheetTitle,
              textAlign: TextAlign.center,
              style: textTheme.titleLarge?.copyWith(fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 12),
            Divider(height: 1, color: theme.dividerColor.withValues(alpha: 0.5)),
            const SizedBox(height: 12),
          ],

          // 2. Optional Leading Widgets
          if (widget.leadingWidgets != null && widget.leadingWidgets!.isNotEmpty) ...[
            ...widget.leadingWidgets!,
            const SizedBox(height: 16),
          ],

          _buildDateSelector(
            context,
            label: Constant.du, // Make labels configurable if needed
            selectedDate: _selectedFromDate,
            dateFormat: dateFormat,
            onTap: () => _pickDate(context, true),
          ),
          const SizedBox(height: 10),
          _buildDateSelector(
            context,
            label: Constant.au, // Make labels configurable if needed
            selectedDate: _selectedToDate,
            dateFormat: dateFormat,
            onTap: () => _pickDate(context, false),
          ),
          const SizedBox(height: 16),

          // 4. Optional Trailing Widgets
          if (widget.trailingWidgets != null && widget.trailingWidgets!.isNotEmpty) ...[
            ...widget.trailingWidgets!,
            const SizedBox(height: 16),
          ],

          // 5. Action Buttons
          const SizedBox(height: 12), // Space before buttons
          ElevatedButton(
            style: ElevatedButton.styleFrom(
              backgroundColor: colorScheme.primary,
              foregroundColor: colorScheme.onPrimary,
              padding: const EdgeInsets.symmetric(vertical: 12),
            ),
            child: Text(actualApplyButtonText, style: const TextStyle(fontSize: 16)),
            onPressed: () {
              final dateRangeState = Provider.of<DateRangeState>(context, listen: false);
              dateRangeState.updateDateRange(_selectedFromDate, _selectedToDate);
              widget.onDateRangeSelected(_selectedFromDate, _selectedToDate);


              if (widget.onApplyAll != null) {
                widget.onApplyAll!();
              }
              if (Navigator.canPop(context)) {
                Navigator.pop(context); // Close the bottom sheet
              }
            },
          ),
          const SizedBox(height: 8),
          TextButton(
            child: Text(
              actualCancelButtonText,
              style: TextStyle(color: colorScheme.primary),
            ),
            onPressed: () {
              if (Navigator.canPop(context)) {
                Navigator.pop(context); // Close without applying changes
              }
            },
          ),
        ],
      ),
    );
  }



  Widget _buildDateSelector(
      BuildContext context, {
        required String label,
        required DateTime selectedDate,
        required DateFormat dateFormat,
        required VoidCallback onTap,
      }) {
    final textTheme = Theme.of(context).textTheme;
    final colorScheme = Theme.of(context).colorScheme;

    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Text(label, style: textTheme.titleMedium),
        InkWell(
          onTap: onTap,
          child: Container(
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
            decoration: BoxDecoration(
              border: Border.all(
                color: colorScheme.outline.withValues(alpha: 0.5),
              ),
              borderRadius: BorderRadius.circular(8),
            ),
            child: Row(
              children: [
                Icon(
                  Icons.calendar_today,
                  size: 18,
                  color: colorScheme.primary,
                ),
                const SizedBox(width: 8),
                Text(
                  dateFormat.format(selectedDate),
                  style: textTheme.titleMedium?.copyWith(
                    color: colorScheme.primary,
                  ),
                ),
              ],
            ),
          ),
        ),
      ],
    );
  }
}
