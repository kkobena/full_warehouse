import 'package:mobile/src/models/tva.dart';
import 'package:mobile/src/utils/service/shared_service.dart';
import 'package:mobile/src/utils/service_state_wrapper.dart';

class TvaService extends BaseServiceNotifier {
  final SharedService _sharedService;
  Tva? _tva;
  bool _isLoading = false;
  String _errorMessage = '';

  Tva? get tva => _tva;

  @override
  bool get isLoading => _isLoading;

  @override
  String get errorMessage => _errorMessage;

  @override
  bool get hasData => _tva != null && _tva!.items.isNotEmpty;

  TvaService() : _sharedService = SharedService();

  Future<void> fetch(String fromDate, String toDate) async {
    final Map<String, String> queryParameters = {};
    if (fromDate.isNotEmpty) {
      queryParameters['fromDate'] = fromDate;
    }
    if (toDate.isNotEmpty) {
      queryParameters['toDate'] = toDate;
    }

    _tva = await _sharedService.fetchData<Tva>(
      endpoint: '/mobile/tva',
      queryParameters: queryParameters,
      parserFromJson: (jsonData) => Tva.fromJson(jsonData),
      setLoading: (loading) => _isLoading = loading,
      setError: (error) => _errorMessage = error,
      notifyListenersCallback: notifyListeners,
    );
  }
}
