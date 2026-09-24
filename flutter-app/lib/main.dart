import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'core/theme/app_colors.dart';
import 'core/theme/app_theme.dart';
import 'core/widgets/glass_container.dart';
import 'data/repositories/auth_repository.dart';
import 'data/repositories/community_repository.dart';
import 'data/repositories/housing_repository.dart';
import 'data/repositories/payments_repository.dart';
import 'data/repositories/voting_repository.dart';
import 'data/services/api_client.dart';
import 'data/services/session_storage_service.dart';
import 'viewmodels/auth_viewmodel.dart';
import 'viewmodels/community_viewmodel.dart';
import 'viewmodels/home_viewmodel.dart';
import 'viewmodels/housing_viewmodel.dart';
import 'viewmodels/payments_viewmodel.dart';
import 'viewmodels/voting_viewmodel.dart';
import 'views/auth/change_password_view.dart';
import 'views/auth/login_view.dart';
import 'views/main_navigation_shell.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();

  // Bloquear orientación a vertical para diseño móvil óptimo
  await SystemChrome.setPreferredOrientations([
    DeviceOrientation.portraitUp,
    DeviceOrientation.portraitDown,
  ]);

  // Inicializar almacenamiento local
  final storage = await SessionStorageService.init();

  // Inicializar capa de red y repositorios
  final apiClient = ApiClient(storage: storage);
  final authRepository = AuthRepository(api: apiClient, storage: storage);
  final paymentsRepository = PaymentsRepository(api: apiClient);
  final communityRepository = CommunityRepository(api: apiClient);
  final votingRepository = VotingRepository(api: apiClient);
  final housingRepository = HousingRepository(api: apiClient);

  // Inicializar ViewModels
  final authViewModel = AuthViewModel(authRepository: authRepository);
  final homeViewModel = HomeViewModel(
    paymentsRepository: paymentsRepository,
    communityRepository: communityRepository,
    votingRepository: votingRepository,
  );
  final paymentsViewModel = PaymentsViewModel(paymentsRepository: paymentsRepository);
  final communityViewModel = CommunityViewModel(communityRepository: communityRepository);
  final votingViewModel = VotingViewModel(repository: votingRepository);
  final housingViewModel = HousingViewModel(repository: housingRepository);

  // Intentar restaurar sesión guardada
  await authViewModel.restoreSession();

  runApp(AsociacionComunalApp(
    storage: storage,
    authViewModel: authViewModel,
    homeViewModel: homeViewModel,
    paymentsViewModel: paymentsViewModel,
    communityViewModel: communityViewModel,
    votingViewModel: votingViewModel,
    housingViewModel: housingViewModel,
  ));
}

class AsociacionComunalApp extends StatefulWidget {
  final SessionStorageService storage;
  final AuthViewModel authViewModel;
  final HomeViewModel homeViewModel;
  final PaymentsViewModel paymentsViewModel;
  final CommunityViewModel communityViewModel;
  final VotingViewModel votingViewModel;
  final HousingViewModel housingViewModel;

  const AsociacionComunalApp({
    super.key,
    required this.storage,
    required this.authViewModel,
    required this.homeViewModel,
    required this.paymentsViewModel,
    required this.communityViewModel,
    required this.votingViewModel,
    required this.housingViewModel,
  });

  @override
  State<AsociacionComunalApp> createState() => _AsociacionComunalAppState();
}

class _AsociacionComunalAppState extends State<AsociacionComunalApp> {
  late ThemeMode _themeMode;

  @override
  void initState() {
    super.initState();
    final savedMode = widget.storage.themeMode;
    if (savedMode == 'dark') {
      _themeMode = ThemeMode.dark;
    } else if (savedMode == 'light') {
      _themeMode = ThemeMode.light;
    } else {
      _themeMode = ThemeMode.system;
    }
  }

  void _toggleTheme() {
    setState(() {
      if (_themeMode == ThemeMode.dark) {
        _themeMode = ThemeMode.light;
        widget.storage.setThemeMode('light');
      } else {
        _themeMode = ThemeMode.dark;
        widget.storage.setThemeMode('dark');
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Asociación Comunal',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.lightTheme,
      darkTheme: AppTheme.darkTheme,
      themeMode: _themeMode,
      home: ListenableBuilder(
        listenable: widget.authViewModel,
        builder: (context, _) {
          final status = widget.authViewModel.status;

          if (status == AuthStatus.initial || status == AuthStatus.loading && widget.authViewModel.profile == null) {
            return const _SplashScreen();
          }

          if (status == AuthStatus.passwordChangeRequired) {
            return ChangePasswordView(viewModel: widget.authViewModel);
          }

          if (status == AuthStatus.authenticated && widget.authViewModel.profile != null) {
            return MainNavigationShell(
              authViewModel: widget.authViewModel,
              homeViewModel: widget.homeViewModel,
              paymentsViewModel: widget.paymentsViewModel,
              communityViewModel: widget.communityViewModel,
              votingViewModel: widget.votingViewModel,
              housingViewModel: widget.housingViewModel,
              storage: widget.storage,
              onToggleTheme: _toggleTheme,
              profile: widget.authViewModel.profile!,
            );
          }

          return LoginView(
            viewModel: widget.authViewModel,
            storage: widget.storage,
          );
        },
      ),
    );
  }
}

class _SplashScreen extends StatelessWidget {
  const _SplashScreen();

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: AmbientBackground(
        child: Center(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              GlassContainer.frosted(
                padding: const EdgeInsets.all(24),
                borderRadius: BorderRadius.circular(32),
                child: Container(
                  width: 64,
                  height: 64,
                  decoration: const BoxDecoration(
                    shape: BoxShape.circle,
                    gradient: AppColors.brandGradient,
                  ),
                  child: const Icon(
                    Icons.apartment_rounded,
                    color: Colors.white,
                    size: 36,
                  ),
                ),
              ),
              const SizedBox(height: 24),
              const SizedBox(
                width: 28,
                height: 28,
                child: CircularProgressIndicator(
                  strokeWidth: 2.5,
                  valueColor: AlwaysStoppedAnimation<Color>(AppColors.brandBlue),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
