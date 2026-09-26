import 'package:flutter/material.dart';
import 'package:fluentui_system_icons/fluentui_system_icons.dart';
import '../core/widgets/glass_nav_bar.dart';
import '../data/models/auth_models.dart';
import '../data/services/session_storage_service.dart';
import '../viewmodels/auth_viewmodel.dart';
import '../viewmodels/community_viewmodel.dart';
import '../viewmodels/home_viewmodel.dart';
import '../viewmodels/housing_viewmodel.dart';
import '../viewmodels/payments_viewmodel.dart';
import '../viewmodels/voting_viewmodel.dart';
import 'community/community_view.dart';
import 'community/voting_view.dart';
import 'home/home_view.dart';
import 'payments/payments_view.dart';
import 'profile/profile_view.dart';

/// Contenedor de navegación principal con barra flotante Glassmórfica.
class MainNavigationShell extends StatefulWidget {
  final AuthViewModel authViewModel;
  final HomeViewModel homeViewModel;
  final PaymentsViewModel paymentsViewModel;
  final CommunityViewModel communityViewModel;
  final VotingViewModel votingViewModel;
  final HousingViewModel housingViewModel;
  final SessionStorageService storage;
  final VoidCallback onToggleTheme;
  final MeResponse profile;

  const MainNavigationShell({
    super.key,
    required this.authViewModel,
    required this.homeViewModel,
    required this.paymentsViewModel,
    required this.communityViewModel,
    required this.votingViewModel,
    required this.housingViewModel,
    required this.storage,
    required this.onToggleTheme,
    required this.profile,
  });

  @override
  State<MainNavigationShell> createState() => _MainNavigationShellState();
}

class _MainNavigationShellState extends State<MainNavigationShell> {
  int _currentIndex = 0;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      final memberId = widget.profile.member?.id;
      widget.paymentsViewModel.loadPayments(idMiembro: memberId);
      widget.communityViewModel.loadCommunityData();
      widget.votingViewModel.loadVotaciones();
    });
  }

  void _navigateToTab(int index) {
    setState(() => _currentIndex = index);
  }

  @override
  Widget build(BuildContext context) {
    final screens = [
      HomeView(
        viewModel: widget.homeViewModel,
        paymentsViewModel: widget.paymentsViewModel,
        profile: widget.profile,
        onNavigateToPayments: () => _navigateToTab(1),
        onNavigateToCommunity: () => _navigateToTab(2),
        onNavigateToVoting: () => _navigateToTab(3),
      ),
      PaymentsView(
        viewModel: widget.paymentsViewModel,
        idMiembro: widget.profile.member?.id,
      ),
      CommunityView(
        viewModel: widget.communityViewModel,
        paymentsViewModel: widget.paymentsViewModel,
        votingViewModel: widget.votingViewModel,
        idMiembro: widget.profile.member?.id,
      ),
      VotingView(
        viewModel: widget.votingViewModel,
        idMiembro: widget.profile.member?.id,
      ),
      ProfileView(
        authViewModel: widget.authViewModel,
        housingViewModel: widget.housingViewModel,
        storage: widget.storage,
        onToggleTheme: widget.onToggleTheme,
      ),
    ];

    return Scaffold(
      extendBody: true,
      body: AnimatedSwitcher(
        duration: const Duration(milliseconds: 300),
        switchInCurve: Curves.easeOutCubic,
        switchOutCurve: Curves.easeInCubic,
        transitionBuilder: (child, animation) {
          return FadeTransition(
            opacity: animation,
            child: SlideTransition(
              position: Tween<Offset>(
                begin: const Offset(0, 0.03),
                end: Offset.zero,
              ).animate(animation),
              child: child,
            ),
          );
        },
        child: KeyedSubtree(
          key: ValueKey<int>(_currentIndex),
          child: screens[_currentIndex],
        ),
      ),
      bottomNavigationBar: GlassNavBar(
        currentIndex: _currentIndex,
        onTap: _navigateToTab,
        items: const [
          GlassNavItem(
            icon: FluentIcons.home_24_regular,
            activeIcon: FluentIcons.home_24_filled,
            label: 'Inicio',
          ),
          GlassNavItem(
            icon: FluentIcons.wallet_24_regular,
            activeIcon: FluentIcons.wallet_24_filled,
            label: 'Aportaciones',
          ),
          GlassNavItem(
            icon: FluentIcons.people_community_24_regular,
            activeIcon: FluentIcons.people_community_24_filled,
            label: 'Comunidad',
          ),
          GlassNavItem(
            icon: FluentIcons.vote_24_regular,
            activeIcon: FluentIcons.vote_24_filled,
            label: 'Votaciones',
          ),
          GlassNavItem(
            icon: FluentIcons.person_circle_24_regular,
            activeIcon: FluentIcons.person_circle_24_filled,
            label: 'Perfil',
          ),
        ],
      ),
    );
  }
}
