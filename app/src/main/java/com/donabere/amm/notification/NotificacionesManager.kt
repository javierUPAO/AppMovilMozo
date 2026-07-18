package com.donabere.amm.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import com.donabere.amm.R
import com.donabere.amm.model.enums.TipoNotificacion
import com.donabere.amm.ui.DetallePedidoActivity
import com.donabere.amm.ui.MainActivity

/**
 * HU 5.1 · Construye y muestra notificaciones locales de estado del pedido.
 *
 * Al tocar la notificación se abre directamente [DetallePedidoActivity] con la
 * mesa correspondiente (criterio de aceptación 5), con [MainActivity] como
 * pantalla padre para que el botón "atrás" regrese al flujo normal.
 */
object NotificacionesManager {

    private const val CHANNEL_ID = "pedidos_estado"
    private const val CHANNEL_NOMBRE = "Estado de pedidos"
    private const val CHANNEL_DESC = "Avisos de cambios de estado del pedido y confirmación de pago"

    fun crearCanal(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NOMBRE,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC
                enableVibration(true)
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(canal)
        }
    }

    /**
     * Muestra una notificación para un cambio de estado/pago.
     *
     * @param pedidoId id del pedido afectado (para abrir su detalle).
     * @param mesaId   id de la mesa a mostrar (deep-link al detalle).
     */
    fun mostrar(
        context: Context,
        tipo: TipoNotificacion,
        pedidoId: String,
        mesaId: String,
        etiquetaMesa: String
    ) {
        crearCanal(context)

        val (titulo, mensaje) = textoPara(tipo, etiquetaMesa)

        val detalleIntent = DetallePedidoActivity.newIntent(context, pedidoId, mesaId)
        val pendingIntent = TaskStackBuilder.create(context).run {
            addNextIntent(android.content.Intent(context, MainActivity::class.java))
            addNextIntent(detalleIntent)
            getPendingIntent(
                pedidoId.hashCode(),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val notificacion = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle(titulo)
            .setContentText(mensaje)
            .setStyle(NotificationCompat.BigTextStyle().bigText(mensaje))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            // Un id estable por pedido+tipo evita duplicados y permite actualizar.
            val notifId = (pedidoId + tipo.name).hashCode()
            NotificationManagerCompat.from(context).notify(notifId, notificacion)
        } catch (_: SecurityException) {
            // Sin permiso POST_NOTIFICATIONS (Android 13+): se ignora silenciosamente.
        }
    }

    private fun textoPara(tipo: TipoNotificacion, mesa: String): Pair<String, String> = when (tipo) {
        TipoNotificacion.ENPREPARACION ->
            "🍳 $mesa · Pedido en preparación" to
                "El pedido de $mesa entró en preparación en cocina."
        TipoNotificacion.PEDIDOLISTO ->
            "🔔 $mesa · Pedido listo" to
                "El pedido de $mesa está listo para entregar."
        TipoNotificacion.PAGADO ->
            "💰 $mesa · Cuenta pagada" to
                "La cuenta de $mesa fue pagada exitosamente. Puedes confirmar el cierre."
    }
}
