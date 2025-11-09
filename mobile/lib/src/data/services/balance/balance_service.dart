
import 'package:mobile/src/models/balance.dart';
import 'package:mobile/src/utils/service/shared_service.dart';
import 'package:mobile/src/utils/service_state_wrapper.dart';

class BalanceService extends BaseServiceNotifier {
  final SharedService _sharedService;
  Balance? _balance;
  bool _isLoading = false;
  String _errorMessage = '';

  Balance? get balance => _balance;
  @override
  bool get isLoading => _isLoading;
  @override
  String get errorMessage => _errorMessage;
  @override
  bool get hasData => _balance != null && _balance!.items.isNotEmpty;
  BalanceService() : _sharedService = SharedService();

  Future<void> fetch(String fromDate, String toDate) async {
    final Map<String, String> queryParameters = {};
    if (fromDate.isNotEmpty) {
      queryParameters['fromDate'] = fromDate;
    }
    if (toDate.isNotEmpty) {
      queryParameters['toDate'] = toDate;
    }

    _balance = await _sharedService.fetchData<Balance>(
      endpoint: '/mobile/balance',
      queryParameters: queryParameters,
      parserFromJson: (jsonData) => Balance.fromJson(jsonData),
      setLoading: (loading) => _isLoading = loading,
      setError: (error) => _errorMessage = error,
      notifyListenersCallback: notifyListeners,

    );
  }
}
