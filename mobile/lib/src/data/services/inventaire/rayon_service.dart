import 'package:hive/hive.dart';
import 'package:mobile/src/models/inventaire/rayon.dart';
import 'package:mobile/src/utils/constant.dart';
import 'package:mobile/src/utils/service/shared_service.dart';
import 'package:mobile/src/utils/service_state_wrapper.dart';

class RayonService extends BaseServiceNotifier {
  final SharedService _sharedService;
  late Box<Rayon> _rayonBox;
  bool _isLoading = false;
  String _errorMessage = '';
  List<Rayon>? _rayons;

  List<Rayon>? get rayons => _rayons;

  @override
  bool get isLoading => _isLoading;

  @override
  String get errorMessage => _errorMessage;

  @override
  bool get hasData => _rayons != null && _rayons!.isNotEmpty;

  // RayonService() : _sharedService = SharedService();
  RayonService() : _sharedService = SharedService() {
    _initRayonBox();
  }

  Future<void> _initRayonBox() async {
    // Initialize the Hive box for Rayon
    if (!Hive.isBoxOpen(Constant.hiveRayonBox)) {
      await Hive.openBox<Rayon>(Constant.hiveRayonBox);
    } else {
      _rayonBox = Hive.box<Rayon>(Constant.hiveRayonBox);
    }
  }

  Future<void> fetchAll(int id) async {
    _rayons = await _sharedService.fetchListData<Rayon>(
      endpoint: '/mobile/inventories/rayons/$id',
      itemParserFromJson: (jsonData) => Rayon.fromJson(jsonData),
      setLoading: (loading) => _isLoading = loading,
      setError: (error) => _errorMessage = error,
      notifyListenersCallback: notifyListeners,
      saveListToHive: _saveAllToHive,
      loadListFromHive: _loadFromHive,
      forceRefresh:
          false, // Set to true if you want to bypass cache for this call
    );
  }

  Future<void> _saveAllToHive(List<Rayon> data) async {
    await _rayonBox.clear(); // Clear existing data if needed
    for (var rayon in data) {
      await _rayonBox.put(rayon.id, rayon);
    }
  }

  Future<List<Rayon>?> _loadFromHive() async {
    final loadedData = _rayonBox.values.toList();
    _rayons = loadedData;
    return loadedData;
  }
}
