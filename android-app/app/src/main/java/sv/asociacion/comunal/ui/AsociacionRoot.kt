package sv.asociacion.comunal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import sv.asociacion.comunal.LoginValidator
import sv.asociacion.comunal.MainViewModel
import sv.asociacion.comunal.R
import sv.asociacion.comunal.SessionState
import sv.asociacion.comunal.data.MeResponse
import sv.asociacion.comunal.update.UpdateInstaller
import sv.asociacion.comunal.update.UpdateState

private val BrandBlue = Color(0xFF0759C7)
private val DeepBlue = Color(0xFF002D72)

@Composable fun AsociacionRoot(viewModel: MainViewModel) {
    Box {
        when (val state = viewModel.state.collectAsStateWithLifecycle().value) {
            SessionState.Loading -> LoadingScreen()
            SessionState.SignedOut -> LoginScreen(onLogin = viewModel::login)
            is SessionState.Error -> LoginScreen(state.message, viewModel::login, viewModel::dismissError)
            is SessionState.SignedIn -> ResidentApp(state.profile, viewModel::logout)
        }
        UpdateOverlay(viewModel.updateState.collectAsStateWithLifecycle().value, viewModel::downloadUpdate, viewModel::dismissUpdate)
    }
}

@Composable private fun UpdateOverlay(state: UpdateState, download: (sv.asociacion.comunal.update.AppUpdate) -> Unit, dismiss: () -> Unit) {
    val context = LocalContext.current
    when (state) {
        is UpdateState.Available -> AlertDialog(
            onDismissRequest = dismiss,
            icon = { Icon(Icons.Default.SystemUpdate, null) },
            title = { Text("Actualización ${state.update.version}") },
            text = { Text(state.update.notes.ifBlank { "Hay una nueva versión disponible con mejoras y correcciones." }) },
            confirmButton = { Button({ download(state.update) }) { Text("Actualizar") } },
            dismissButton = { TextButton(dismiss) { Text("Más tarde") } }
        )
        is UpdateState.Downloading -> AlertDialog(
            onDismissRequest = {},
            title = { Text("Actualizando…") },
            text = { Column { Text("Descargando archivos. No cierres la aplicación."); Spacer(Modifier.height(18.dp)); LinearProgressIndicator(progress = { state.percent / 100f }, Modifier.fillMaxWidth()); Spacer(Modifier.height(8.dp)); Text("${state.percent}%", Modifier.fillMaxWidth(), textAlign = TextAlign.Center) } },
            confirmButton = {}
        )
        is UpdateState.Ready -> AlertDialog(
            onDismissRequest = dismiss,
            icon = { Icon(Icons.Default.DownloadDone, null) },
            title = { Text("Descarga completada") },
            text = { Text("Android abrirá el instalador para actualizar la aplicación. Tus datos y sesión se conservarán.") },
            confirmButton = { Button({ UpdateInstaller.install(context, state.file) }) { Text("Instalar") } },
            dismissButton = { TextButton(dismiss) { Text("Después") } }
        )
        is UpdateState.Error -> AlertDialog(onDismissRequest = dismiss, title = { Text("Actualización") }, text = { Text(state.message) }, confirmButton = { TextButton(dismiss) { Text("Aceptar") } })
        else -> Unit
    }
}

@Composable private fun LoadingScreen() = Box(
    Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(DeepBlue, BrandBlue))),
    contentAlignment = Alignment.Center
) { CircularProgressIndicator(color = Color.White) }

@Composable private fun LoginScreen(error: String? = null, onLogin: (String, String) -> Unit, onDismissError: () -> Unit = {}) {
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxWidth().weight(.42f).background(Brush.verticalGradient(listOf(DeepBlue, BrandBlue))), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(shape = CircleShape, color = Color.White, modifier = Modifier.size(112.dp)) {
                        androidx.compose.foundation.Image(painterResource(R.drawable.logo_asociacion_comunal), "Logo Asociación Comunal", Modifier.padding(12.dp), contentScale = ContentScale.Fit)
                    }
                    Spacer(Modifier.height(14.dp)); Text("Asociación Comunal", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Text("Portal del residente", color = Color.White.copy(alpha = .8f))
                }
            }
            Column(Modifier.fillMaxWidth().weight(.58f).padding(horizontal = 26.dp, vertical = 24.dp)) {
                Text("Iniciar sesión", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Ingresa con tu cuenta de miembro", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(20.dp))
                OutlinedTextField(username, { username = it; onDismissError() }, Modifier.fillMaxWidth(), label = { Text("Usuario") }, leadingIcon = { Icon(Icons.Default.Person, null) }, singleLine = true, shape = RoundedCornerShape(12.dp))
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(password, { password = it; onDismissError() }, Modifier.fillMaxWidth(), label = { Text("Contraseña") }, leadingIcon = { Icon(Icons.Default.Lock, null) }, singleLine = true, visualTransformation = PasswordVisualTransformation(), shape = RoundedCornerShape(12.dp))
                if (error != null) Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 10.dp))
                Spacer(Modifier.height(18.dp))
                Button({ onLogin(username, password) }, Modifier.fillMaxWidth().height(52.dp), enabled = LoginValidator.isValid(username, password), shape = RoundedCornerShape(12.dp)) { Text("Ingresar", fontWeight = FontWeight.Bold) }
                Spacer(Modifier.weight(1f)); Text("¿No tienes cuenta? Contacta a la administración", Modifier.fillMaxWidth(), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private enum class Tab(val label: String, val icon: ImageVector) {
    HOME("Inicio", Icons.Default.Home), PAYMENTS("Pagos", Icons.Default.Wallet),
    COMMUNITY("Comunidad", Icons.Default.Groups), ACCOUNT("Mi cuenta", Icons.Default.Person)
}

@Composable private fun ResidentApp(profile: MeResponse, onLogout: () -> Unit) {
    var tab by rememberSaveable { mutableStateOf(Tab.HOME) }
    Scaffold(bottomBar = {
        NavigationBar(tonalElevation = 8.dp) { Tab.entries.forEach { item ->
            NavigationBarItem(tab == item, { tab = item }, { Icon(item.icon, null) }, label = { Text(item.label, fontSize = 11.sp) })
        } }
    }) { padding -> when (tab) {
        Tab.HOME -> Home(profile, Modifier.padding(padding))
        Tab.PAYMENTS -> Payments(Modifier.padding(padding))
        Tab.COMMUNITY -> Community(Modifier.padding(padding))
        Tab.ACCOUNT -> Account(profile, onLogout, Modifier.padding(padding))
    } }
}

@Composable private fun Home(profile: MeResponse, modifier: Modifier = Modifier) {
    val name = profile.member?.names?.substringBefore(' ') ?: profile.user.username
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Box(Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(DeepBlue, BrandBlue))).padding(22.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("Hola, $name 👋", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("Asociación Comunal", color = Color.White.copy(alpha = .8f)) }
                Icon(Icons.Default.NotificationsNone, "Notificaciones", tint = Color.White)
            }
        }
        Column(Modifier.padding(16.dp)) {
            FeatureCard("Cuenta activa", "Tu perfil está vinculado correctamente", Icons.Default.VerifiedUser, "ACTIVO", Color(0xFF21A453))
            Spacer(Modifier.height(12.dp)); FeatureCard("Pagos y cuotas", "Disponible en la siguiente fase", Icons.Default.Wallet, "PRÓXIMAMENTE", BrandBlue)
            Spacer(Modifier.height(12.dp)); FeatureCard("Próxima reunión", "Aún no hay datos publicados", Icons.Default.CalendarMonth, null, BrandBlue)
            Spacer(Modifier.height(12.dp)); FeatureCard("Votaciones", "Consulta y participa desde tu teléfono", Icons.Default.HowToVote, "PRÓXIMAMENTE", Color(0xFF25A449))
        }
    }
}

@Composable private fun FeatureCard(title: String, subtitle: String, icon: ImageVector, badge: String?, accent: Color) {
    ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(accent.copy(alpha = .12f)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = accent) }
            Spacer(Modifier.width(14.dp)); Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Bold); Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
            if (badge != null) Text(badge, color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable private fun Payments(modifier: Modifier = Modifier) = SectionScreen("Pagos", "Aquí podrás consultar cuotas e historial.", Icons.Default.Wallet, modifier) {
    FeatureCard("Módulo en preparación", "No se procesan pagos reales en esta versión", Icons.Default.Construction, "PRÓXIMAMENTE", BrandBlue)
}

@Composable private fun Community(modifier: Modifier = Modifier) = SectionScreen("Comunidad", "Todo lo que sucede en tu asociación.", Icons.Default.Groups, modifier) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CommunityTile("Votaciones", Icons.Default.HowToVote, Modifier.weight(1f)); CommunityTile("Reuniones", Icons.AutoMirrored.Filled.MenuBook, Modifier.weight(1f))
    }
    Spacer(Modifier.height(12.dp)); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CommunityTile("Proyectos", Icons.Default.HolidayVillage, Modifier.weight(1f)); CommunityTile("Avisos", Icons.Default.Campaign, Modifier.weight(1f))
    }
    Spacer(Modifier.height(16.dp)); Text("Estas opciones se conectarán progresivamente con la API.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
}

@Composable private fun CommunityTile(label: String, icon: ImageVector, modifier: Modifier) { ElevatedCard(modifier.height(126.dp), shape = RoundedCornerShape(16.dp)) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) { Icon(icon, null, tint = BrandBlue, modifier = Modifier.size(32.dp)); Spacer(Modifier.weight(1f)); Text(label, fontWeight = FontWeight.Bold); Text("Próximamente", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }

@Composable private fun SectionScreen(title: String, subtitle: String, icon: ImageVector, modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = BrandBlue, modifier = Modifier.size(34.dp)); Spacer(Modifier.width(12.dp)); Column { Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant) } }; Spacer(Modifier.height(24.dp)); content() }
}

@Composable private fun Account(profile: MeResponse, onLogout: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Text("Mi cuenta", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); Spacer(Modifier.height(18.dp))
        ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) { Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(54.dp).clip(CircleShape).background(BrandBlue.copy(alpha = .12f)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Person, null, tint = BrandBlue, modifier = Modifier.size(32.dp)) }; Spacer(Modifier.width(14.dp)); Column { Text(profile.member?.let { "${it.names} ${it.lastNames}" } ?: profile.user.username, fontWeight = FontWeight.Bold, fontSize = 18.sp); Text("● Miembro activo", color = Color(0xFF21A453), style = MaterialTheme.typography.bodySmall) } } }
        Spacer(Modifier.height(16.dp)); AccountRow("Información personal", Icons.Default.Badge); AccountRow("Mi vivienda", Icons.Default.HomeWork); AccountRow("Configuración", Icons.Default.Settings)
        Spacer(Modifier.height(24.dp)); OutlinedButton(onLogout, Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error), shape = RoundedCornerShape(12.dp)) { Icon(Icons.AutoMirrored.Filled.Logout, null); Spacer(Modifier.width(8.dp)); Text("Cerrar sesión") }
        Text("Versión 0.2.0", Modifier.fillMaxWidth().padding(top = 18.dp), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable private fun AccountRow(label: String, icon: ImageVector) { Row(Modifier.fillMaxWidth().padding(vertical = 15.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = BrandBlue); Spacer(Modifier.width(14.dp)); Text(label, Modifier.weight(1f)); Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) }; HorizontalDivider() }
