package com.example.vibetrack.data.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.vibetrack.R
import com.example.vibetrack.data.SensorDataPoint
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SensorCollectionService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var latestHeartRate: Float = 0.0f
    private var latestAccel = FloatArray(3)
    private var latestGyro = FloatArray(3)
    private val dataBuffer = mutableListOf<SensorDataPoint>()

    private val handler = Handler(Looper.getMainLooper())
    private val dataAssemblyRunnable = object : Runnable {
        override fun run() {
            assembleDataPoint()
            handler.postDelayed(this, 1000)
        }
    }

    private val binder = LocalBinder()
    inner class LocalBinder : Binder() {
        fun getService(): SensorCollectionService = this@SensorCollectionService
    }

    private val _dataFlow = MutableSharedFlow<SensorDataPoint>()
    val dataFlow: SharedFlow<SensorDataPoint> = _dataFlow

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val NOTIFICATION_CHANNEL_ID = "VibeTrackChannel"
        const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                dataBuffer.clear()
                startForeground(NOTIFICATION_ID, createNotification())
                registerSensors()
                handler.post(dataAssemblyRunnable)
                Log.d("SensorService", "Coleta iniciada.")
            }
            ACTION_STOP -> {
                unregisterSensors()
                handler.removeCallbacks(dataAssemblyRunnable)
                if (dataBuffer.isNotEmpty()) {
                    saveDataToFile()
                }
                dataBuffer.clear()
                stopForeground(true)
                stopSelf()
                Log.d("SensorService", "Coleta parada.")
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        when (event.sensor.type) {
            Sensor.TYPE_HEART_RATE -> latestHeartRate = event.values[0]
            Sensor.TYPE_ACCELEROMETER -> latestAccel = event.values.clone()
            Sensor.TYPE_GYROSCOPE -> latestGyro = event.values.clone()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Pode deixar este método vazio
    }

    private fun registerSensors() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        sensorManager.registerListener(this, heartRateSensor, SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_NORMAL)
    }

    private fun unregisterSensors() {
        if (::sensorManager.isInitialized) {
            sensorManager.unregisterListener(this)
        }
    }

    private fun assembleDataPoint() {
        val newPoint = SensorDataPoint(
            timestamp = System.currentTimeMillis(),
            heartRate = latestHeartRate,
            accelX = latestAccel.getOrElse(0) { 0f },
            accelY = latestAccel.getOrElse(1) { 0f },
            accelZ = latestAccel.getOrElse(2) { 0f },
            gyroX = latestGyro.getOrElse(0) { 0f },
            gyroY = latestGyro.getOrElse(1) { 0f },
            gyroZ = latestGyro.getOrElse(2) { 0f }
        )
        dataBuffer.add(newPoint)
        Log.d("SensorService", "Novo Ponto Coletado: $newPoint")
        _dataFlow.tryEmit(newPoint)
    }

    private fun saveDataToFile() {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "session_$timestamp.csv"
        val header = "timestamp,heartRate,accelX,accelY,accelZ,gyroX,gyroY,gyroZ\n"
        try {
            val fileOutputStream: FileOutputStream = openFileOutput(fileName, Context.MODE_PRIVATE)
            val writer = OutputStreamWriter(fileOutputStream)
            writer.write(header)
            dataBuffer.forEach { dataPoint ->
                val line = "${dataPoint.timestamp},${dataPoint.heartRate},${dataPoint.accelX},${dataPoint.accelY},${dataPoint.accelZ},${dataPoint.gyroX},${dataPoint.gyroY},${dataPoint.gyroZ}\n"
                writer.write(line)
            }
            writer.close()
            fileOutputStream.close()
            Log.d("SensorService", "Dados salvos com sucesso no arquivo: $fileName")
        } catch (e: Exception) {
            Log.e("SensorService", "Erro ao salvar o arquivo: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun createNotificationChannel() {
        val name = "VibeTrack Data Collection"
        val descriptionText = "Notificação mostrando que o VibeTrack está coletando dados"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    // AQUI ESTÁ A CORREÇÃO - O CÓDIGO COMPLETO DA FUNÇÃO
    private fun createNotification() = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
        .setContentTitle("VibeTrack Coletando")
        .setContentText("A coleta de dados está ativa.")
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setOngoing(true)
        .build()
}