import 'package:flutter/foundation.dart';
import 'package:mobile/src/utils/constant.dart';

class DateRangeState with ChangeNotifier {
  DateTime _selectedFromDate = Constant.fromDate; // Initialize with defaults
  DateTime _selectedToDate = Constant.toDate;

  DateTime get selectedFromDate => _selectedFromDate;
  DateTime get selectedToDate => _selectedToDate;

  void updateDateRange(DateTime fromDate, DateTime toDate) {
    bool changed = false;
    if (_selectedFromDate != fromDate) {
      _selectedFromDate = fromDate;
      changed = true;
    }
    if (_selectedToDate != toDate) {
      _selectedToDate = toDate;
      changed = true;
    }


    if (_selectedToDate.isBefore(_selectedFromDate)) {
      _selectedToDate = _selectedFromDate;
      changed = true; // Could have changed again
    }

    if (changed) {
      notifyListeners();
    }
  }

  void updateFromDate(DateTime fromDate) {
    if (_selectedFromDate == fromDate) return;
    _selectedFromDate = fromDate;
    if (_selectedToDate.isBefore(_selectedFromDate)) {
      _selectedToDate = _selectedFromDate;
    }
    notifyListeners();
  }

  void updateToDate(DateTime toDate) {
    if (_selectedToDate == toDate) return;
    _selectedToDate = toDate;
    if (_selectedFromDate.isAfter(_selectedToDate)) {
      _selectedFromDate = _selectedToDate;
    }
    notifyListeners();
  }
}
