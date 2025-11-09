import 'package:flutter/material.dart';

abstract class BaseServiceNotifier with ChangeNotifier {
  bool get isLoading;
  String get errorMessage;
  bool get hasData; // You'll need to define what "hasData" means for each service
}
class ServiceStateWrapper<T_SERVICE extends BaseServiceNotifier, T_DATA> extends StatelessWidget {
  final T_SERVICE service;
  final T_DATA? data; // The actual data object from the service (e.g., Balance, Tva)
  final Widget Function(BuildContext context, T_DATA data) successBuilder;
  final Widget? emptyDataWidget; // Optional: custom widget for empty state
  final String? customEmptyDataMessage; // Optional: custom message for empty state
  final Widget? loadingWidget; // Optional: custom loading widget
  final Widget Function(BuildContext context, String errorMessage)? errorBuilder; // Optional: custom error widget builder

  const ServiceStateWrapper({
    super.key,
    required this.service,
    required this.data,
    required this.successBuilder,
    this.emptyDataWidget,
    this.customEmptyDataMessage,
    this.loadingWidget,
    this.errorBuilder,
  });

  @override
  Widget build(BuildContext context) {
    if (service.isLoading && data == null) {
      return loadingWidget ?? const Center(child: CircularProgressIndicator());
    }

    if (service.errorMessage.isNotEmpty && data == null) {
      if (errorBuilder != null) {
        return errorBuilder!(context, service.errorMessage);
      }
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(16.0),
          child: Text(
            service.errorMessage,
            style: const TextStyle(color: Colors.red),
            textAlign: TextAlign.center,

          ),
        ),
      );
    }


    bool isDataConsideredEmpty;
    if (data == null) {
      isDataConsideredEmpty = true;
    } else if (data is List) {
      isDataConsideredEmpty = (data as List).isEmpty;
    } else if (data is Map) {
      isDataConsideredEmpty = (data as Map).isEmpty;
    }

    else {

      isDataConsideredEmpty = !service.hasData && data == null; // Adjust if service.hasData is more accurate
    }


    if (isDataConsideredEmpty) {
      return emptyDataWidget ?? Center(
        child: Padding(
          padding: const EdgeInsets.all(16.0),
          child: Text(
            customEmptyDataMessage ?? 'Aucune donnée .', // Default empty message
            style: const TextStyle(color: Colors.red),
            textAlign: TextAlign.center,
          ),
        ),
      );
    }

    // If data is not null and not empty, call the successBuilder
    return successBuilder(context, data as T_DATA); // Cast to T_DATA (non-null)
  }
}

// --- Option 2: Simpler, less type-safe without BaseServiceNotifier (if you prefer) ---
// You would pass isLoading, errorMessage, and data directly.

class SimpleStateWrapper<T_DATA> extends StatelessWidget {
  final bool isLoading;
  final String errorMessage;
  final T_DATA? data; // The actual data from the service
  final bool Function(T_DATA? data) isDataEmptyChecker; // Function to check if data is empty
  final Widget Function(BuildContext context, T_DATA data) successBuilder;
  final String noDataMessage;

  const SimpleStateWrapper({
    super.key,
    required this.isLoading,
    required this.errorMessage,
    required this.data,
    required this.isDataEmptyChecker,
    required this.successBuilder,
    this.noDataMessage = 'Aucune donnée pour la période sélectionnée.',
  });

  @override
  Widget build(BuildContext context) {
    if (isLoading && data == null) { // Or a more robust check if data can be partially loaded
      return const Center(child: CircularProgressIndicator());
    }

    if (errorMessage.isNotEmpty && data == null) { // Or if error should override partial data
      return Center(child: Text(errorMessage, style: const TextStyle(color: Colors.red)));
    }

    if (data == null || isDataEmptyChecker(data)) {
      return Center(child: Text(noDataMessage, style: const TextStyle(color: Colors.red)));
    }

    // Data is present and not empty
    return successBuilder(context, data as T_DATA); // We've checked for null, so cast is safe
  }
}