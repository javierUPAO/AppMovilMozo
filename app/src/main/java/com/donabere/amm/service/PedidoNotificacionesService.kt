package com.donabere.amm.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.donabere.amm.R
import com.donabere.amm.model.enums.EstadoPedido
import com.donabere.amm.model.enums.TipoNotificacion
import com.donabere.amm.notification.NotificacionesManager
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

/**
 * HU 5.1 · Servicio en primer plano que mantiene vivo un listener de Firestore
 * sobre los pedidos del mozo (del día). Cuando detecta un cambio de estado
 * relevante o la confirmación de un pago, lanza una notificación local.
 *
 * Funciona con la app en primer plano y en segundo plano/minimizada porque el
 * servicio foreground mantiene el proceso y el listener activos.
 */
class PedidoNotificacionesService : Service() {

    private val db = FirebaseFirestore.getInstance()
    private var registro: ListenerRegistration? = null

    /** Último estado conocido por pedido, para detectar transiciones. */
    private val estadosPrevios = mutableMapOf<String, EstadoPedido>()

    /** Evita disparar notificaciones por los pedidos ya existentes al arrancar. */
    private var primerSnapshot = true

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        arrancarEnPrimerPlano()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val mozoId = intent?.getStringExtra(EXTRA_MOZO_ID)
            ?: obtenerMozoIdGuardado()

        if (mozoId.isNullOrBlank()) {
            Log.w(TAG, "Sin mozoId; deteniendo servicio de notificaciones.")
            stopSelf()
            return START_NOT_STICKY
        }

        if (registro == null) {
            escucharPedidos(mozoId)
        }
        // START_STICKY: el sistema reinicia el servicio si lo mata por memoria.
        return START_STICKY
    }

    private fun escucharPedidos(mozoId: String) {
        val hoy = Timestamp.now()
        val inicioDia = Timestamp(hoy.seconds - (hoy.seconds % 86400), 0)
        val finDia = Timestamp(inicioDia.seconds + 86400, 0)

        registro = db.collection("pedidos")
            .whereEqualTo("mozoId", mozoId)
            .whereGreaterThanOrEqualTo("creadoEn", inicioDia)
            .whereLessThan("creadoEn", finDia)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error escuchando pedidos: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener

                for (doc in snapshot.documents) {
                    val pedidoId = doc.id
                    val nuevoEstado = try {
                        EstadoPedido.valueOf(doc.getString("estado") ?: continue)
                    } catch (e: IllegalArgumentException) {
                        continue
                    }

                    val estadoPrevio = estadosPrevios[pedidoId]
                    estadosPrevios[pedidoId] = nuevoEstado

                    // El primer snapshot solo siembra el estado actual; no notifica.
                    if (primerSnapshot || estadoPrevio == null || estadoPrevio == nuevoEstado) {
                        continue
                    }

                    val tipo = tipoNotificacionPara(nuevoEstado) ?: continue
                    val mesasIds = (doc.get("mesasIds") as? List<*>)
                        ?.mapNotNull { it as? String }
                        ?: emptyList()
                    val mesaId = mesasIds.firstOrNull() ?: ""

                    NotificacionesManager.mostrar(
                        context = applicationContext,
                        tipo = tipo,
                        pedidoId = pedidoId,
                        mesaId = mesaId,
                        etiquetaMesa = etiquetaMesa(mesasIds)
                    )
                }

                primerSnapshot = false
            }
    }

    /** Mapea la transición de estado del pedido al tipo de notificación de la HU. */
    private fun tipoNotificacionPara(estado: EstadoPedido): TipoNotificacion? = when (estado) {
        EstadoPedido.COCINA -> TipoNotificacion.ENPREPARACION
        EstadoPedido.LISTO_PARA_ENTREGAR -> TipoNotificacion.PEDIDOLISTO
        EstadoPedido.PAGADO, EstadoPedido.PAGADO_PARCIAL -> TipoNotificacion.PAGADO
        else -> null
    }

    /** "m1" -> "Mesa 1"; varias mesas -> "Mesas 1, 2". */
    private fun etiquetaMesa(mesasIds: List<String>): String {
        if (mesasIds.isEmpty()) return "Mesa"
        val numeros = mesasIds.map { it.removePrefix("m") }
        return if (numeros.size == 1) "Mesa ${numeros.first()}"
        else "Mesas ${numeros.joinToString(", ")}"
    }

    private fun obtenerMozoIdGuardado(): String? =
        getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .getString("mozoId", null)

    private fun arrancarEnPrimerPlano() {
        crearCanalServicio()
        val notif: Notification = NotificationCompat.Builder(this, CHANNEL_SERVICIO)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle("Bere Mozos activo")
            .setContentText("Escuchando cambios de tus pedidos.")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                SERVICE_NOTIF_ID,
                notif,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(SERVICE_NOTIF_ID, notif)
        }
    }

    private fun crearCanalServicio() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                CHANNEL_SERVICIO,
                "Servicio de notificaciones",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Mantiene la escucha de cambios de pedidos activa."
            }
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(canal)
        }
    }

    override fun onDestroy() {
        registro?.remove()
        registro = null
        super.onDestroy()
    }

    companion object {
        private const val TAG = "PedidoNotifService"
        private const val CHANNEL_SERVICIO = "pedidos_servicio"
        private const val SERVICE_NOTIF_ID = 1001
        const val EXTRA_MOZO_ID = "extra_mozo_id"

        fun iniciar(context: Context, mozoId: String) {
            val intent = Intent(context, PedidoNotificacionesService::class.java)
                .putExtra(EXTRA_MOZO_ID, mozoId)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun detener(context: Context) {
            context.stopService(Intent(context, PedidoNotificacionesService::class.java))
        }
    }
}
