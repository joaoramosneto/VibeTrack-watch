// Arquivo MainActivity.kt ATUALIZADO
package com.example.vibetrack.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue // Importar o getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.PhoneAndroid
import com.example.vibetrack.presentation.theme.VibeTrackTheme

// Importações para o ViewModel e State
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Não passamos mais argumentos aqui
            WearApp()
        }
    }
}

@Composable
fun WearApp(
    // Injetamos o ViewModel aqui
    viewModel: MainViewModel = viewModel()
) {
    // Coletamos os valores do ViewModel como "estados" que o Compose pode observar
    val steps by viewModel.steps.collectAsStateWithLifecycle()
    val calories by viewModel.calories.collectAsStateWithLifecycle()
    val heartRate by viewModel.heartRate.collectAsStateWithLifecycle()
    val watchBattery by viewModel.watchBattery.collectAsStateWithLifecycle()
    val phoneBattery by viewModel.phoneBattery.collectAsStateWithLifecycle()
    val isCollecting by viewModel.isCollecting.collectAsStateWithLifecycle()

    VibeTrackTheme {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Passamos os dados dinâmicos e o estado de coleta para a tela
            VibeTrackScreen(
                steps = steps,
                calories = calories,
                heartRate = heartRate,
                watchBattery = watchBattery,
                phoneBattery = phoneBattery,
                isCollecting = isCollecting,
                onStartClick = { viewModel.startCollection() }, // Passa a função de start
                onStopClick = { viewModel.stopCollection() }      // Passa a função de stop
            )
        }
    }
}

@Composable
fun VibeTrackScreen(
    steps: String,
    calories: String,
    heartRate: String,
    watchBattery: Int,
    phoneBattery: Int,
    isCollecting: Boolean, // Novo parâmetro para controlar a UI
    onStartClick: () -> Unit, // Nova função para o clique de Start
    onStopClick: () -> Unit   // Nova função para o clique de Stop
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // ... (A Row com os ícones de Passos, Calorias e Batimentos continua igual)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            InfoColumn(icon = { Icon(Icons.Default.DirectionsWalk, contentDescription = "Passos") }, value = steps)
            InfoColumn(icon = { Icon(Icons.Default.LocalFireDepartment, contentDescription = "Calorias") }, value = calories)
            InfoColumn(icon = { Icon(Icons.Default.Favorite, contentDescription = "Frequência Cardíaca") }, value = heartRate)
        }


        Spacer(modifier = Modifier.height(12.dp))

        // AQUI ESTÁ A LÓGICA DE INTERAÇÃO
        if (isCollecting) {
            Text(
                text = "Monitoring...",
                fontSize = 14.sp,
                color = Color(0xFF81C784) // Verde para indicar que está ativo
            )
            Spacer(modifier = Modifier.height(8.dp))
            CompactChip(
                onClick = onStopClick,
                label = { Text("STOP") },
                colors = ChipDefaults.chipColors(backgroundColor = Color(0xFFE57373)) // Vermelho
            )
        } else {
            Text(
                text = "VibeTrack",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFC107)
            )
            Spacer(modifier = Modifier.height(8.dp))
            CompactChip(
                onClick = onStartClick,
                label = { Text("START") }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ... (A Row com as baterias continua igual)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.BatteryStd, contentDescription = "Bateria do Relógio", tint = Color(0xFF87CEEB))
            Text(text = " $watchBattery%", modifier = Modifier.padding(end = 16.dp))

            Icon(Icons.Default.PhoneAndroid, contentDescription = "Bateria do Celular", tint = Color(0xFF87CEEB))
            Text(text = " $phoneBattery%")
        }
    }
}


@Composable
fun InfoColumn(icon: @Composable () -> Unit, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        icon()
        Text(text = value)
    }
}

@Preview(device = Devices.WEAR_OS_SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    WearApp()
}