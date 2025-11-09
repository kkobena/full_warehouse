import 'package:mobile/src/models/produit/produit.dart';
import 'package:mobile/src/utils/service/shared_service.dart';
import 'package:mobile/src/utils/service_state_wrapper.dart';

class ProduitService extends BaseServiceNotifier {
  final SharedService _sharedService;
  Produit? _produit;
  bool _isLoading = false;
  String _errorMessage = '';
  List<Produit>? _items ;

  Produit? get produit => _produit;
  List<Produit>? get items => _items;

  @override
  bool get isLoading => _isLoading;

  @override
  String get errorMessage => _errorMessage;

  @override
  bool get hasData => _produit != null;

  ProduitService() : _sharedService = SharedService();

  Future<void> fetch(String produitId,String fromDate, String toDate) async {
    final Map<String, String> queryParameters = {};
    if (fromDate.isNotEmpty) {
      queryParameters['fromDate'] = fromDate;
    }
    if (toDate.isNotEmpty) {
      queryParameters['toDate'] = toDate;
    }

    _produit = await _sharedService.fetchData<Produit>(
      endpoint: '/mobile/produit/$produitId',
      queryParameters: queryParameters,
      parserFromJson: (jsonData) => Produit.fromJson(jsonData),
      setLoading: (loading) => _isLoading = loading,
      setError: (error) => _errorMessage = error,
      notifyListenersCallback: notifyListeners,
    );
  }

  Future<void> fetchAll(String search) async {
    final Map<String, String> queryParameters = {};
    if (search.isNotEmpty) {
      queryParameters['search'] = search;
    }


    _items = await _sharedService.fetchListData<Produit>(
      endpoint: '/mobile/produits',
      queryParameters: queryParameters,
      itemParserFromJson: (jsonData) => Produit.fromJson(jsonData),
      setLoading: (loading) => _isLoading = loading,
      setError: (error) => _errorMessage = error,
      notifyListenersCallback: notifyListeners,
    );
  }
}
