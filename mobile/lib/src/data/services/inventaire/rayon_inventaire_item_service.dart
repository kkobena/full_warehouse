import 'package:hive/hive.dart';
import 'package:http/http.dart' as http;
import 'package:mobile/src/models/inventaire/inventaire_item.dart';
import 'package:mobile/src/utils/constant.dart';

import 'package:mobile/src/utils/service/shared_service.dart';
import 'package:mobile/src/utils/service_state_wrapper.dart';

class RayonInventaireItemService extends BaseServiceNotifier {
  final SharedService _sharedService;
  late Box<InventaireItem> _inventaireItemBox;
  bool _isLoading = false;
  String _errorMessage = '';
  List<InventaireItem>? _rayonInventaireItems;

  List<InventaireItem>? get rayonInventaireItems => _rayonInventaireItems;

  @override
  bool get isLoading => _isLoading;

  @override
  String get errorMessage => _errorMessage;

  @override
  bool get hasData =>
      rayonInventaireItems != null && rayonInventaireItems!.isNotEmpty;

  RayonInventaireItemService() : _sharedService = SharedService() {
    _initRayonBox();
  }

  Future<void> _initRayonBox() async {
    if (!Hive.isBoxOpen(Constant.hiverayonInventaireItemsBox)) {
      await Hive.openBox<InventaireItem>(Constant.hiverayonInventaireItemsBox);
    } else {
      _inventaireItemBox = Hive.box<InventaireItem>(
        Constant.hiverayonInventaireItemsBox,
      );
    }
  }

  Future<void> fetchAll(int idRayon, idInventaire) async {
    _rayonInventaireItems = await _sharedService.fetchListData<InventaireItem>(
      endpoint: '/mobile/rayons/$idRayon/items/$idInventaire',
      itemParserFromJson: (jsonData) => InventaireItem.fromJson(jsonData),
      setLoading: (loading) => _isLoading = loading,
      setError: (error) => _errorMessage = error,
      notifyListenersCallback: notifyListeners,
      saveListToHive: _saveAllToHive,
      loadListFromHive: _loadFromHive,
      forceRefresh:
          false, // Set to true if you want to bypass cache for this call
    );
  }

  Future<void> _saveAllToHive(List<InventaireItem> data) async {
    for (var item in data) {
      await _inventaireItemBox.put(item.id, item);
    }
  }

  Future<List<InventaireItem>?> _loadFromHive() async {
    final loadedData = _inventaireItemBox.values.toList();
    _rayonInventaireItems = loadedData;
    return loadedData;
  }



  Future<List<InventaireItem>> _getItemsToSynchronize(int rayonId) async {
    final List<InventaireItem> itemsToSync = await _loadFromHive() ?? [];
    _rayonInventaireItems = itemsToSync;
    return itemsToSync;
  }
  Future<bool> synchronizeItems(int rayonId) async {
    _isLoading = true;
    _errorMessage = '';
    notifyListeners();

    final List<InventaireItem> items = await _getItemsToSynchronize(rayonId);

    if (items.isEmpty) {
      _errorMessage = 'Aucun élément à synchroniser.';
      _isLoading = false;
      notifyListeners();
      return true; // Nothing to do, considered success
    }

    try {
      // Convert List<InventaireItem> to List<Map<String, dynamic>>
      final List<Map<String, dynamic>> jsonDataList = items.map((item) => item.toJson()).toList();

      final http.Response? response = await _sharedService.postListData( // Or a new method in SharedService
        endpoint: '/mobile/inventaire-items/synchronize', // Replace with your actual sync endpoint
        jsonDataList: jsonDataList,
        setLoading: (loading) => _isLoading = loading,
        setError: (error) => _errorMessage = error,
        notifyListenersCallback: notifyListeners,
      );

      if (response != null && (response.statusCode == 200 || response.statusCode == 201 || response.statusCode == 204)) {
        _errorMessage = ''; // Clear error on success
        _isLoading = false;
        return true;
      } else {
        // _errorMessage is already set by postListData or the catch block below
        if (_errorMessage.isEmpty && response != null) {
          _errorMessage = 'Erreur de synchronisation (Code: ${response.statusCode}).';
        } else if (_errorMessage.isEmpty) {
          _errorMessage = 'Erreur de synchronisation inconnue.';
        }
        _isLoading = false;
        return false;
      }
    } catch (e) {
      _errorMessage = 'Erreur inattendue lors de la synchronisation: $e';

      _isLoading = false;
      notifyListeners();
      return false;
    }
  }
}
