package com.example.vibetrack.presentation.viewmodel

import com.example.vibetrack.data.service.SensorCollectionService
import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.vibetrack.data.SensorDataPoint // Importe seu data class
import java.text.DecimalFormat

// Mude a herança para AndroidViewModel e passe 'application' no construtor
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _isCollecting = MutableStateFlow(false)
    val isCollecting: StateFlow<Boolean> = _isCollecting.asStateFlow()

    // ... (o resto dos StateFlows continua igual) ...
    private val _steps = MutableStateFlow("0")
    val steps: StateFlow<String> = _steps.asStateFlow()
    private val _calories = MutableStateFlow("0")
    val calories: StateFlow<String> = _calories.asStateFlow()
    private val _heartRate = MutableStateFlow("--")
    val heartRate: StateFlow<String> = _heartRate.asStateFlow()
    private val _watchBattery = MutableStateFlow(100)
    val watchBattery: StateFlow<Int> = _watchBattery.asStateFlow()
    private val _phoneBattery = MutableStateFlow(100)
    val phoneBattery: StateFlow<Int> = _phoneBattery.asStateFlow()


    fun startCollection() {
        _isCollecting.value = true
        // Criamos uma "ordem" para iniciar o serviço
        val intent = Intent(getApplication(), SensorCollectionService::class.java).apply {
            action = SensorCollectionService.ACTION_START
        }
        // Enviamos a ordem para o sistema Android
        getApplication<Application>().startForegroundService(intent)
    }

    fun stopCollection() {
        _isCollecting.value = false
        // Criamos uma "ordem" para parar o serviço
        val intent = Intent(getApplication(), SensorCollectionService::class.java).apply {
            action = SensorCollectionService.ACTION_STOP
        }
        // Enviamos a ordem
        getApplication<Application>().startService(intent)
    }

    fun updateData(dataPoint: SensorDataPoint) {
        _heartRate.value = dataPoint.heartRate?.toInt().toString()

        // TODO: Adicionar lógica para calcular passos e calorias com base no acelerômetro
        // Por enquanto, vamos apenas mostrar um valor do acelerômetro como exemplo
        val formattedAccel = DecimalFormat("#.##").format(dataPoint.accelX)
        _steps.value = formattedAccel
    }
}