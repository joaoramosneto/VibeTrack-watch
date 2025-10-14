package com.example.vibetrack.data

// Em um arquivo SensorDataPoint.kt
data class SensorDataPoint(
    val timestamp: Long,         // Momento da leitura
    val heartRate: Float?,       // Frequência cardíaca
    val accelX: Float,           // Acelerômetro Eixo X
    val accelY: Float,           // Acelerômetro Eixo Y
    val accelZ: Float,           // Acelerômetro Eixo Z
    val gyroX: Float,            // Giroscópio Eixo X
    val gyroY: Float,            // Giroscópio Eixo Y
    val gyroZ: Float             // Giroscópio Eixo Z
)