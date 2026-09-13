import 'package:flutter/material.dart';
import '../data/models/meeting_model.dart';
import '../data/models/project_model.dart';
import '../data/repositories/community_repository.dart';

/// ViewModel para la sección de comunidad: asambleas, reuniones y proyectos comunales.
class CommunityViewModel extends ChangeNotifier {
  final CommunityRepository _communityRepository;

  bool _isLoading = false;
  List<MeetingModel> _meetings = [];
  List<ProjectModel> _projects = [];
  int _selectedTab = 0; // 0 = Reuniones, 1 = Proyectos

  CommunityViewModel({required CommunityRepository communityRepository})
      : _communityRepository = communityRepository;

  bool get isLoading => _isLoading;
  List<MeetingModel> get meetings => _meetings;
  List<ProjectModel> get projects => _projects;
  int get selectedTab => _selectedTab;

  void setSelectedTab(int tab) {
    _selectedTab = tab;
    notifyListeners();
  }

  Future<void> loadCommunityData() async {
    _isLoading = true;
    notifyListeners();

    try {
      final meetingsFuture = _communityRepository.getMeetings();
      final projectsFuture = _communityRepository.getProjects();

      final results = await Future.wait([meetingsFuture, projectsFuture]);
      _meetings = results[0] as List<MeetingModel>;
      _projects = results[1] as List<ProjectModel>;
    } catch (_) {
      // Ignorar fallo de red
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }
}
