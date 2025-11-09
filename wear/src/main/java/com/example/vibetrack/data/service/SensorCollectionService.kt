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
import com.example.vibetrack.data.HealthData
import com.example.vibetrack.data.HeartRate
import com.google.android.gms.wearable.Wearable
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class SensorCollectionService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var latestHeartRate: Float = 0.0f
    private var latestAccel = FloatArray(3)
    private var latestGyro = FloatArray(3)
    private val dataBuffer = mutableListOf<SensorDataPoint>()

    // --- LÓGICA DE PASSOS ATUALIZADA ---
    private var stepCounterSensor: Sensor? = null
    private var initialStepCount: Int = -1
    private var latestStepCount: Int = -1
    // ------------------------------------

    private val handler = Handler(Looper.getMainLooper())
    private val dataAssemblyRunnable = object : Runnable {
        override fun run() {
            assembleDataPoint()
            handler.postDelayed(this, 1000) // Coleta a cada 1 segundo
        }
    }

    private val binder = LocalBinder()
    inner class LocalBinder : Binder() {
        fun getService(): SensorCollectionService = this@SensorCollectionService
    }

    private val _dataFlow = MutableSharedFlow<SensorDataPoint>()
    val dataFlow: SharedFlow<SensorDataPoint> = _dataFlow

    private val EXPERIMENT_DATA_PATH = "/experiment-data"

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val NOTIFICATION_CHANNEL_ID = "VibeTrackChannel"
        const val NOTIFICATION_ID = 1
        private const val TAG = "SensorCollectionService"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                dataBuffer.clear()
                // Reseta a contagem de passos
                initialStepCount = -1
                latestStepCount = -1
                startForeground(NOTIFICATION_ID, createNotification())
                registerSensors()
                handler.post(dataAssemblyRunnable)
                Log.d(TAG, "Coleta iniciada.")
            }
            ACTION_STOP -> {
                unregisterSensors()
                handler.removeCallbacks(dataAssemblyRunnable)

                if (dataBuffer.isNotEmpty()) {
                    sendDataToMobile()
                } else {
                    Log.w(TAG, "Buffer de dados vazio, nada para enviar.")
                }

                dataBuffer.clear()
                stopForeground(true)
                stopSelf()
                Log.d(TAG, "Coleta parada e dados enviados.")
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

            // --- LÓGICA DE PASSOS ATUALIZADA ---
            Sensor.TYPE_STEP_COUNTER -> {
                val count = event.values[0].toInt()
                if (initialStepCount == -1) {
                    // Armazena o primeiro valor que o sensor reporta
                    initialStepCount = count
                }
                // Atualiza o valor mais recente
                latestStepCount = count
                Log.d(TAG, "Contagem de passos atualizada: $latestStepCount (inicial: $initialStepCount)")
            }
            // ------------------------------------
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

        // --- LÓGICA DE PASSOS ATUALIZADA ---
        stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        // ------------------------------------

        sensorManager.registerListener(this, heartRateSensor, SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_NORMAL)

        // --- LÓGICA DE PASSOS ATUALIZADA ---
        stepCounterSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
            Log.d(TAG, "Sensor de passos registrado.")
        } ?: run {
            Log.e(TAG, "Sensor de passos (STEP_COUNTER) não encontrado neste dispositivo.")
        }
        // ------------------------------------
    }

    private fun unregisterSensors() {
        if (::sensorManager.isInitialized) {
            sensorManager.unregisterListener(this)
            Log.d(TAG, "Todos os listeners de sensores foram desregistrados.")
        }
    }

    private fun assembleDataPoint() {
        val newPoint = SensorDataPoint(
            timestamp = System.currentTimeMillis(),
            heartRate = if (latestHeartRate > 0) latestHeartRate else null, // Salva null se HR for 0
            accelX = latestAccel.getOrElse(0) { 0f },
            accelY = latestAccel.getOrElse(1) { 0f },
            accelZ = latestAccel.getOrElse(2) { 0f },
            gyroX = latestGyro.getOrElse(0) { 0f },
            gyroY = latestGyro.getOrElse(1) { 0f },
            gyroZ = latestGyro.getOrElse(2) { 0f }
        )
        dataBuffer.add(newPoint)
        // Não precisamos mais logar *cada* ponto, vamos comentar para logs mais limpos
        // Log.d(TAG, "Novo Ponto Coletado: $newPoint")
    }

    private fun sendDataToMobile() {
        // 1. Processar dados de Frequência Cardíaca
        val validHeartRates = dataBuffer.mapNotNull { it.heartRate }.filter { it > 0 }

        val restingHr = validHeartRates.minOrNull()?.toInt() ?: 0
        val averageHr = if (validHeartRates.isNotEmpty()) validHeartRates.average().toInt() else 0
        val maxHr = validHeartRates.maxOrNull()?.toInt() ?: 0

        // 2. Processar dados de Passos
        // --- LÓGICA DE PASSOS ATUALIZADA ---
        val steps = if (initialStepCount != -1 && latestStepCount != -1 && latestStepCount > initialStepCount) {
            latestStepCount - initialStepCount
        } else {
            0 // Nenhum passo detectado ou dados inválidos
        }
        // ------------------------------------

        // 3. Montar os objetos de dados
        val heartRateData = HeartRate(resting = restingHr, average = averageHr, max = maxHr)
        val healthData = HealthData(steps = steps, heartRate = heartRateData)

        // 4. Serializar para JSON
        val gson = Gson()
        val jsonMessage = gson.toJson(healthData)
        Log.d(TAG, "Enviando JSON para o celular: $jsonMessage") // << VERIFIQUE ESTE LOG!

        // 5. Enviar mensagem para o celular
        Handler(Looper.getMainLooper()).post {
            try {
                val nodesTask = Wearable.getNodeClient(this).connectedNodes

                nodesTask.addOnSuccessListener { nodes ->
                    if (nodes.isEmpty()) {
                        Log.w(TAG, "Nenhum celular conectado encontrado.")
                        return@addOnSuccessListener
                    }
                    nodes.forEach { node ->
                        Log.d(TAG, "Tentando enviar mensagem para: ${node.displayName}")
                        Wearable.getMessageClient(this).sendMessage(
                            node.id,
                            EXPERIMENT_DATA_PATH,
                            jsonMessage.toByteArray(Charsets.UTF_8)
                        ).apply {
                            addOnSuccessListener { Log.d(TAG, "Mensagem enviada com sucesso para ${node.displayName}") }
                            addOnFailureListener { e -> Log.e(TAG, "Falha ao enviar mensagem para ${node.displayName}", e) }
                        }
                    }
                }
                nodesTask.addOnFailureListener { e ->
                    Log.e(TAG, "Falha ao buscar nós conectados", e)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exceção ao tentar enviar mensagem", e)
            }
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

    private fun createNotification() = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
        .setContentTitle("VibeTrack Coletando")
        .setContentText("A coleta de dados está ativa.")
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setOngoing(true)
        .build()
}