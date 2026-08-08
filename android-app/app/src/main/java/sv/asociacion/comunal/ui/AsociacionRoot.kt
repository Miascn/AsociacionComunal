package sv.asociacion.comunal.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import sv.asociacion.comunal.MainViewModel
import sv.asociacion.comunal.SessionState
import sv.asociacion.comunal.LoginValidator
import sv.asociacion.comunal.data.MeResponse

@Composable fun AsociacionRoot(viewModel: MainViewModel) {
    when (val state = viewModel.state.collectAsStateWithLifecycle().value) {
        SessionState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        SessionState.SignedOut -> LoginScreen(onLogin = viewModel::login)
        is SessionState.Error -> LoginScreen(error = state.message, onLogin = viewModel::login, onDismissError = viewModel::dismissError)
        is SessionState.SignedIn -> ResidentApp(state.profile, viewModel::logout)
    }
}

@Composable private fun LoginScreen(error: String? = null, onLogin: (String, String) -> Unit, onDismissError: () -> Unit = {}) {
    var username by remember { mutableStateOf("") }; var password by remember { mutableStateOf("") }
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.Center) {
            Icon(Icons.Default.Groups, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(52.dp))
            Spacer(Modifier.height(18.dp)); Text("Bienvenido", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text("Portal del residente de la Asociación Comunal", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(30.dp))
            OutlinedTextField(username, { username = it; onDismissError() }, Modifier.fillMaxWidth(), label = { Text("Usuario") }, singleLine = true)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(password, { password = it; onDismissError() }, Modifier.fillMaxWidth(), label = { Text("Contraseña") }, singleLine = true, visualTransformation = PasswordVisualTransformation())
            if (error != null) Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp))
            Spacer(Modifier.height(22.dp))
            Button({ onLogin(username, password) }, Modifier.fillMaxWidth(), enabled = LoginValidator.isValid(username, password)) { Text("Ingresar") }
            Text("Acceso seguro para miembros autorizados", style = MaterialTheme.typography.bodySmall, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 14.dp))
        }
    }
}

private enum class Tab(val label: String) { HOME("Inicio"), PAYMENTS("Pagos"), COMMUNITY("Comunidad"), ACCOUNT("Mi cuenta") }

@Composable private fun ResidentApp(profile: MeResponse, onLogout: () -> Unit) {
    var tab by rememberSaveable { mutableStateOf(Tab.HOME) }
    Scaffold(bottomBar = { NavigationBar { Tab.entries.forEach { item -> NavigationBarItem(selected = tab == item, onClick = { tab = item }, icon = { Icon(when(item) { Tab.HOME -> Icons.Default.Home; Tab.PAYMENTS -> Icons.Default.Payments; Tab.COMMUNITY -> Icons.Default.Groups; Tab.ACCOUNT -> Icons.Default.Person }, null) }, label = { Text(item.label) }) } } }) { padding ->
        when (tab) {
            Tab.HOME -> Home(profile, Modifier.padding(padding))
            Tab.PAYMENTS -> Placeholder("Pagos", "Tus cuotas e historial estarán disponibles en una fase posterior.", Modifier.padding(padding))
            Tab.COMMUNITY -> Placeholder("Comunidad", "Aquí encontrarás proyectos, reuniones y votaciones.", Modifier.padding(padding))
            Tab.ACCOUNT -> Account(profile, onLogout, Modifier.padding(padding))
        }
    }
}

@Composable private fun Home(profile: MeResponse, modifier: Modifier = Modifier) {
    val name = profile.member?.names?.substringBefore(' ') ?: profile.user.username
    Column(modifier.fillMaxSize().padding(20.dp)) {
        Text("Buenos días, $name", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Este es el resumen de tu comunidad", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp)); SummaryCard("Cuenta activa", "Tu acceso está vinculado correctamente", Icons.Default.VerifiedUser)
        Spacer(Modifier.height(12.dp)); SummaryCard("Próximamente", "Cuotas, reuniones y votaciones", Icons.Default.Event)
    }
}

@Composable private fun SummaryCard(title: String, text: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    ElevatedCard(Modifier.fillMaxWidth()) { Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(16.dp)); Column { Text(title, fontWeight = FontWeight.SemiBold); Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
}

@Composable private fun Placeholder(title: String, message: String, modifier: Modifier = Modifier) = Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); Spacer(Modifier.height(8.dp)); Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant) } }

@Composable private fun Account(profile: MeResponse, onLogout: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(24.dp)) { Text("Mi cuenta", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); Spacer(Modifier.height(20.dp)); Text(profile.member?.let { "${it.names} ${it.lastNames}" } ?: profile.user.username); Text(profile.user.role, color = MaterialTheme.colorScheme.primary); Spacer(Modifier.weight(1f)); OutlinedButton(onLogout, Modifier.fillMaxWidth()) { Icon(Icons.AutoMirrored.Filled.Logout, null); Spacer(Modifier.width(8.dp)); Text("Cerrar sesión") } }
}
