import 'package:flutter/material.dart';
import '../data/models/housing_model.dart';
import '../data/repositories/housing_repository.dart';

/// ViewModel para la información de la vivienda del residente.
class HousingViewModel extends ChangeNotifier {
  final HousingRepository _repository;

  bool _isLoading = false;
  HousingModel? _housing;
  String? _errorMessage;

  HousingViewModel({required HousingRepository repository}) : _repository = repository;

  bool get isLoading => _isLoading;
  HousingModel? get housing => _housing;
  String? get errorMessage => _errorMessage;

  Future<void> loadHousing({required int idMiembro}) async {
    _isLoading = true;
    _errorMessage = null;
    notifyListeners();

    try {
      _housing = await _repository.getByMemberId(idMiembro);
    } catch (e) {
      _errorMessage = e.toString();
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }
}
