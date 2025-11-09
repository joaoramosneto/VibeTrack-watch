// ARQUIVO ATUALIZADO: MainActivity.kt (Layout Minimalista - Sem Sobreposição)
package com.example.vibetrack.presentation

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background // Importar background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow // Ícone de Play
import androidx.compose.material.icons.filled.Stop // Ícone de Stop
import com.example.vibetrack.presentation.theme.VibeTrackTheme
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vibetrack.presentation.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WearApp()
        }
    }
}

@Composable
fun WearApp(
    viewModel: MainViewModel = viewModel()
) {
    var hasPermission by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        hasPermission = isGranted
    }

    VibeTrackTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            contentAlignment = Alignment.Center
        ) {
            if (hasPermission) {
                val heartRate by viewModel.heartRate.collectAsStateWithLifecycle()
                val isCollecting by viewModel.isCollecting.collectAsStateWithLifecycle()

                VibeTrackScreen(
                    heartRate = heartRate,
                    isCollecting = isCollecting,
                    onStartClick = { viewModel.startCollection() },
                    onStopClick = { viewModel.stopCollection() }
                )
            } else {
                PermissionScreen(
                    onRequestPermission = {
                        permissionLauncher.launch(Manifest.permission.BODY_SENSORS)
                    }
                )
            }
        }
    }
}

// --- DESIGN MINIMALISTA CORRIGIDO ---

@Composable
fun VibeTrackScreen(
    heartRate: String,
    isCollecting: Boolean,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit
) {
    // Usamos um Box para alinhar o conteúdo no centro e o botão na base
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center // Centraliza tudo por padrão
    ) {
        // --- CONTADOR DE BATIMENTOS ---
        // Este Column será centralizado, mas o "empurramos" um pouco para cima
        // para dar espaço ao botão.
        Column(
            modifier = Modifier
                .padding(bottom = 60.dp), // <-- ESSA É A MUDANÇA! Empurra o contador para CIMA
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = "Frequência Cardíaca",
                tint = if (isCollecting && heartRate != "--") Color(0xFFE57373) else MaterialTheme.colors.onBackground,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = heartRate,
                style = MaterialTheme.typography.display1,
                fontSize = 72.sp,
                fontWeight = FontWeight.Light,
                color = if (isCollecting && heartRate != "--") MaterialTheme.colors.primary else MaterialTheme.colors.onBackground
            )
            Text(
                text = "BPM",
                style = MaterialTheme.typography.body1,
                color = MaterialTheme.colors.secondary
            )
        }

        // --- BOTÃO DE AÇÃO (START/STOP) ---
        // Alinhado na parte inferior
        ActionButton(
            isCollecting = isCollecting,
            onStartClick = onStartClick,
            onStopClick = onStopClick,
            modifier = Modifier
                .align(Alignment.BottomCenter) // Alinha na base do Box pai
                .padding(bottom = 24.dp) // Adiciona um padding do fundo da tela
        )
    }
}

/**
 * O botão de ação (redondo)
 */
@Composable
fun ActionButton(
    isCollecting: Boolean,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = if (isCollecting) onStopClick else onStartClick,
        modifier = modifier.size(ButtonDefaults.LargeButtonSize),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = if (isCollecting) Color(0xFFE57373) else MaterialTheme.colors.primary, // Vermelho ou Primário
            contentColor = MaterialTheme.colors.onPrimary
        )
    ) {
        Icon(
            imageVector = if (isCollecting) Icons.Default.Stop else Icons.Default.PlayArrow,
            contentDescription = if (isCollecting) "Parar" else "Iniciar",
            modifier = Modifier.size(ButtonDefaults.LargeIconSize)
        )
    }
}

// --- TELA DE PERMISSÃO (SEM MUDANÇAS) ---
@Composable
fun PermissionScreen(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Permissão Necessária",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "O VibeTrack precisa de acesso aos sensores corporais para medir sua frequência cardíaca.",
            textAlign = TextAlign.Center,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRequestPermission) {
            Text("Conceder Permissão")
        }
    }
}

// --- PREVIEWS ATUALIZADOS ---

@Preview(device = Devices.WEAR_OS_SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    VibeTrackTheme {
        VibeTrackScreen("67", false, {}, {})
    }
}

@Preview(device = Devices.WEAR_OS_SMALL_ROUND, showSystemUi = true)
@Composable
fun CollectingPreview() {
    VibeTrackTheme {
        VibeTrackScreen("68", true, {}, {})
    }
}