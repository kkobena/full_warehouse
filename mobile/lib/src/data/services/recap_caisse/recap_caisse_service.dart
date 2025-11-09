import 'package:mobile/src/models/caisse/recap_caisse.dart';
import 'package:mobile/src/utils/service/shared_service.dart';
import 'package:mobile/src/utils/service_state_wrapper.dart';

class RecapCaisseService extends BaseServiceNotifier {
  final SharedService _sharedService;
  RecapCaisse? _recapCaisse;
  bool _isLoading = false;
  String _errorMessage = '';

  RecapCaisse? get recapCaisse => _recapCaisse;
  @override
  bool get isLoading => _isLoading;
  @override
  String get errorMessage => _errorMessage;
  @override
  bool get hasData => _recapCaisse != null && _recapCaisse!.items.isNotEmpty;
  RecapCaisseService() : _sharedService = SharedService();

  Future<void> fetch(String fromDate, String toDate,bool onlyVente) async {
    final Map<String, String> queryParameters = {};
    if (fromDate.isNotEmpty) {
      queryParameters['fromDate'] = fromDate;
    }
    if (toDate.isNotEmpty) {
      queryParameters['toDate'] = toDate;
    }
    if (onlyVente) {
      queryParameters['onlyVente'] = 'true';
    }else{
      queryParameters['onlyVente'] = 'false';
    }

    _recapCaisse = await _sharedService.fetchData<RecapCaisse>(
      endpoint: '/mobile/recap-caisse',
      queryParameters: queryParameters,
      parserFromJson: (jsonData) => RecapCaisse.fromJson(jsonData),
      setLoading: (loading) => _isLoading = loading,
      setError: (error) => _errorMessage = error,
      notifyListenersCallback: notifyListeners,

    );
  }

}
