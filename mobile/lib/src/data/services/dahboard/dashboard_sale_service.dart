import 'package:flutter/material.dart';
import 'package:mobile/src/models/dashboard.dart';
import 'package:mobile/src/utils/service/shared_service.dart';

class DashboardSaleService extends ChangeNotifier {
  final SharedService _sharedService;
  Dashboard? _dashboard;
  bool _isLoading = false;
  String _errorMessage = '';

  Dashboard? get dashboard => _dashboard;

  bool get isLoading => _isLoading;

  String get errorMessage => _errorMessage;

  DashboardSaleService() : _sharedService = SharedService();

  Future<void> fetchDashboard(String fromDate,String toDate ) async {
    final Map<String, String> queryParameters = {};
    if (fromDate.isNotEmpty) {
      queryParameters['fromDate'] = fromDate;
    }
    if (toDate.isNotEmpty) {
      queryParameters['toDate'] = toDate;
    }

    final Dashboard? result = await _sharedService.fetchData<Dashboard>(
      endpoint: '/mobile/dashboard/data',
      queryParameters: queryParameters,
      parserFromJson: (jsonData) => Dashboard.fromJson(jsonData),
      setLoading: (loading) => _isLoading = loading,
      setError: (error) => _errorMessage = error,
      notifyListenersCallback: notifyListeners,

    );

    if (result != null) {
      _dashboard = result;
    }
  }
}
