import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:http/http.dart' as http;
import 'package:mobile/src/data/services/utils/api_client.dart';

typedef SaveToHiveFunction<T> = Future<void> Function(T data);
typedef SaveListToHiveFunction<T> = Future<void> Function(List<T> data);
typedef LoadFromHiveFunction<T> = Future<T?> Function(); // For single item
typedef LoadListFromHiveFunction<T> = Future<List<T>?> Function();

class SharedService {
  final ApiClient _apiClient;

  SharedService({ApiClient? apiClient}) : _apiClient = apiClient ?? ApiClient();

  Future<T?> fetchData<T>({
    required String endpoint,
    Map<String, String>? queryParameters,
    required T Function(Map<String, dynamic> json) parserFromJson,
    required ValueChanged<bool> setLoading,
    required ValueChanged<String> setError,
    required VoidCallback notifyListenersCallback,
    // Optional Hive handlers
    LoadFromHiveFunction<T>? loadFromHive,
    SaveToHiveFunction<T>? saveToHive,
    bool forceRefresh =
        false, // If true, skip loading from Hive and always fetch from API
  }) async {
    setLoading(true);
    setError('');
    notifyListenersCallback();

    // 1. Try loading from Hive if a handler is provided and not forcing refresh
    if (!forceRefresh && loadFromHive != null) {
      try {
        final T? localData = await loadFromHive();
        if (localData != null) {
          if (kDebugMode) {
            print("SharedService: Loaded data from Hive for $endpoint");
          }
          // No need to call setLoading(false) here, finally block will do it.
          // notifyListenersCallback(); // Data loaded, UI might need update
          return localData; // Return local data and skip API call
        }
      } catch (e) {
        if (kDebugMode) {
          print("SharedService: Error loading from Hive for $endpoint: $e");
          // Continue to fetch from API
        }
      }
    }

    // 2. Fetch from API
    try {
      // Use javaClientUrl for mobile endpoints, apiUrl for others
      final String baseUrl = _apiClient.javaClientUrl ?? _apiClient.apiUrl ?? '';
      final String apiUrl = '$baseUrl$endpoint';
      final uri = Uri.parse(apiUrl).replace(
        queryParameters: queryParameters?.isNotEmpty == true
            ? queryParameters
            : null,
      );

      final response = await http
          .get(uri, headers: _apiClient.headers)
          .timeout(const Duration(seconds: 10));

      if (response.statusCode == 200) {
        if (response.body.isEmpty || response.body.toLowerCase() == 'null') {
          setError('Aucun résultat trouvé.');
          return null;
        }
        final decodedBody = json.decode(response.body);
        if (decodedBody is Map<String, dynamic>) {
          final T parsedData = parserFromJson(decodedBody);
          // 3. Save to Hive if a handler is provided
          if (saveToHive != null) {
            try {
              await saveToHive(parsedData);
              if (kDebugMode) {
                print("SharedService: Saved data to Hive for $endpoint");
              }
            } catch (e) {
              if (kDebugMode) {
                print("SharedService: Error saving to Hive for $endpoint: $e");
                // Don't let Hive error prevent returning fetched data
              }
            }
          }
          return parsedData;
        } else {
          setError('Format de données inattendu.');
          if (kDebugMode) {
            print(
              "SharedService: Expected Map<String, dynamic> but got ${decodedBody.runtimeType} for $endpoint",
            );
          }
          return null;
        }
      } else {
        if (response.statusCode == 401 || response.statusCode == 403) {
          setError('Accès non autorisé.');
        } else {
          setError(
            'Erreur de communication avec le serveur (Code: ${response.statusCode}).',
          );
        }
        if (kDebugMode) {
          print(
            "SharedService HTTP Error ${response.statusCode} for $endpoint: ${response.body}",
          );
        }
        return null;
      }
    } finally {
      setLoading(false);
      notifyListenersCallback();
    }
  }

  Future<List<T>?> fetchListData<T>({
    required String endpoint,
    Map<String, String>? queryParameters,
    required T Function(Map<String, dynamic> json) itemParserFromJson,
    required ValueChanged<bool> setLoading,
    required ValueChanged<String> setError,
    required VoidCallback notifyListenersCallback,
    // Optional Hive handlers
    LoadListFromHiveFunction<T>? loadListFromHive,
    SaveListToHiveFunction<T>? saveListToHive,
    bool forceRefresh =
        false, // If true, skip loading from Hive and always fetch from API
  }) async {
    setLoading(true);
    setError('');
    notifyListenersCallback();

    // 1. Try loading list from Hive if a handler is provided and not forcing refresh
    if (!forceRefresh && loadListFromHive != null) {
      final List<T>? localListData = await loadListFromHive();
      if (localListData != null && localListData.isNotEmpty) {
        // Also check if not empty
        if (kDebugMode) {
          print("SharedService: Loaded list from Hive for $endpoint");
        }
        // No need to call setLoading(false) here, finally block will do it.
        // notifyListenersCallback();
        return localListData;
      }
    }

    // 2. Fetch list from API
    try {
      // Use javaClientUrl for mobile endpoints, apiUrl for others
      final String baseUrl = _apiClient.javaClientUrl ?? _apiClient.apiUrl ?? '';
      final String apiUrl = '$baseUrl$endpoint';
      final Uri uri = Uri.parse(apiUrl).replace(
        queryParameters: queryParameters?.isEmpty ?? true
            ? null
            : queryParameters,
      );

      final http.Response response = await http
          .get(uri, headers: _apiClient.headers)
          .timeout(
            const Duration(seconds: 20),
          ); // Increased timeout slightly for lists

      if (response.statusCode == 200) {
        if (response.body.isEmpty || response.body.toLowerCase() == 'null') {
          setError(
            '',
          ); // No results found is not necessarily an error for lists
          final List<T> emptyList = [];
          // 3a. Save empty list to Hive if a handler is provided (to clear old data)
          if (saveListToHive != null) {
            try {
              await saveListToHive(emptyList);
              if (kDebugMode) {
                print(
                  "SharedService: Saved empty list to Hive for $endpoint (cleared old)",
                );
              }
            } catch (e) {
              if (kDebugMode) {
                print(
                  "SharedService: Error saving empty list to Hive for $endpoint: $e",
                );
              }
            }
          }
          return emptyList;
        }

        final decodedBody = json.decode(response.body);
        if (decodedBody is List) {
          if (decodedBody.isEmpty) {
            final List<T> emptyList = [];
            // 3b. Save empty list to Hive
            if (saveListToHive != null) {
              try {
                await saveListToHive(emptyList);
                if (kDebugMode) {
                  print(
                    "SharedService: Saved empty list to Hive for $endpoint",
                  );
                }
              } catch (e) {
                if (kDebugMode) {
                  print(
                    "SharedService: Error saving empty list to Hive for $endpoint: $e",
                  );
                }
              }
            }
            return emptyList;
          }

          final List<T> results = decodedBody.map((itemJson) {
            if (itemJson is Map<String, dynamic>) {
              return itemParserFromJson(itemJson);
            } else {
              // This specific error should ideally not prevent the rest of the list from parsing
              // or saving, but it's a critical data format issue.
              final formatErrorMsg =
                  'Format de données inattendu pour un élément de la liste.';
              setError(formatErrorMsg);
              throw FormatException(formatErrorMsg, itemJson);
            }
          }).toList();

          // 3c. Save fetched list to Hive
          if (saveListToHive != null) {
            try {
              await saveListToHive(results);
              if (kDebugMode) {
                print("SharedService: Saved list to Hive for $endpoint");
              }
            } catch (e) {
              if (kDebugMode) {
                print(
                  "SharedService: Error saving list to Hive for $endpoint: $e",
                );
              }
            }
          }
          return results;
        } else {
          setError('Format de données inattendu. Attendu une liste.');
          if (kDebugMode) {
            print(
              "SharedService: Expected List for $endpoint but got ${decodedBody.runtimeType}",
            );
          }
          return null;
        }
      } else {
        if (response.statusCode == 401 || response.statusCode == 403) {
          setError('Non autorisé. Veuillez vérifier vos identifiants.');
        } else if (response.statusCode == 404) {
          setError('Ressource non trouvée.');
          return []; // Return empty list for 404
        } else if (response.statusCode >= 500) {
          setError('Erreur du serveur. Veuillez réessayer plus tard.');
        } else {
          setError(
            'Erreur lors du chargement des données (Code: ${response.statusCode}).',
          );
        }
        if (kDebugMode) {
          print(
            "SharedService HTTP Error ${response.statusCode} for $endpoint: ${response.body}",
          );
        }
        return null; // Return null on HTTP error (unless it's a 404 for list)
      }
    } finally {
      setLoading(false);
      notifyListenersCallback();
    }
  }

  Future<http.Response?> postListData({
    required String endpoint,
    required List<Map<String, dynamic>>
    jsonDataList, // List of already JSON-serializable maps
    required ValueChanged<bool> setLoading,
    required ValueChanged<String> setError,
    required VoidCallback notifyListenersCallback,
    Duration timeoutDuration = const Duration(
      seconds: 30,
    ), // Configurable timeout
  }) async {
    setLoading(true);
    setError('');
    notifyListenersCallback(); // Notify loading started, error cleared

    try {
      // Use javaClientUrl for mobile endpoints, apiUrl for others
      final String baseUrl = _apiClient.javaClientUrl ?? _apiClient.apiUrl ?? '';
      final String apiUrl = '$baseUrl$endpoint';
      final Uri uri = Uri.parse(apiUrl);

      if (kDebugMode) {
        print(
          "SharedService (postListData) POST to $uri with ${jsonDataList.length} items.",
        );
      }

      final http.Response response = await http
          .post(
            uri,
            headers: _apiClient.headers, // Use headers from ApiClient
            body: json.encode(
              jsonDataList,
            ), // Encode the list of maps to a JSON array string
          )
          .timeout(timeoutDuration);

      if (response.statusCode < 200 || response.statusCode >= 300) {
        // Not a success status code (2xx)
        _handleHttpError(response, setError, endpoint, context: "postListData");
      } else {
        setError(
          '${jsonDataList.length} produits synchronisés avec success',
        ); // Clear any error if the call was successful (2xx)
      }
      return response; // Return the response for the caller to process
    } finally {
      setLoading(false);
      notifyListenersCallback(); // Notify loading finished
    }
  }

  void _handleHttpError(
    http.Response response,
    ValueChanged<String> setError,
    String endpoint, {
    String context = "",
  }) {
    final String contextMsg = context.isNotEmpty ? "($context) " : "";
    if (response.statusCode == 401 || response.statusCode == 403) {
      setError('Non autorisé. Veuillez vérifier vos identifiants.');
    } else if (response.statusCode == 404 && context != "postListData") {
      // 404 for list GET is handled differently
      setError('Ressource non trouvée.');
    } else if (response.statusCode >= 500) {
      setError('Erreur du serveur. Veuillez réessayer plus tard.');
    } else {
      setError(
        'Erreur ${contextMsg}lors du chargement des données (Code: ${response.statusCode}).',
      );
    }
    if (kDebugMode) {
      print(
        "SharedService ${contextMsg}HTTP Error ${response.statusCode} for $endpoint: ${response.body}",
      );
    }
  }
}
