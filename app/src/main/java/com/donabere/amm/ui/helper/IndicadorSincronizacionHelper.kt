package com.donabere.amm.ui.helper

import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.core.content.ContextCompat
import com.donabere.amm.R
import com.donabere.amm.repository.PedidoRepository
import com.google.android.material.snackbar.Snackbar

/**
 * Helper para mostrar indicadores de sincronización offline
 * con animaciones y estilos consistentes.
 */
object IndicadorSincronizacionHelper {

    private var snackbarActual: Snackbar? = null
    private var estadoAnterior: PedidoRepository.EstadoSincronizacion? = null
    private var mostroSincronizacionPendiente = false

    /**
     * Muestra el indicador según el estado de sincronización
     * y detecta transiciones de estado para mostrar notificaciones
     */
    fun mostrarIndicador(
        contenedor: View,
        estado: PedidoRepository.EstadoSincronizacion
    ) {
        // Detectar transición: de PENDIENTE a SINCRONIZADO
        if (estadoAnterior == PedidoRepository.EstadoSincronizacion.PENDIENTE_SINCRONIZACION &&
            estado == PedidoRepository.EstadoSincronizacion.SINCRONIZADO &&
            mostroSincronizacionPendiente) {
            mostrarNotificacionSincronizacionExitosa(contenedor)
            mostroSincronizacionPendiente = false
        }
        
        estadoAnterior = estado
        
        when (estado) {
            PedidoRepository.EstadoSincronizacion.SINCRONIZADO -> {
                if (estadoAnterior != PedidoRepository.EstadoSincronizacion.PENDIENTE_SINCRONIZACION) {
                    ocultarIndicador()
                }
            }
            PedidoRepository.EstadoSincronizacion.PENDIENTE_SINCRONIZACION -> {
                mostrarIndicadorPendiente(contenedor)
                mostroSincronizacionPendiente = true
            }
            PedidoRepository.EstadoSincronizacion.ERROR_CONEXION -> {
                mostrarIndicadorError(contenedor)
            }
        }
    }

    /**
     * Muestra notificación de sincronización exitosa
     * Usa Snackbar si la View está visible, sino notificación del sistema
     */
    private fun mostrarNotificacionSincronizacionExitosa(contenedor: View) {
        ocultarIndicador()

        val contexto = contenedor.context
        val mensaje = "✅ Cambios sincronizados exitosamente"
        
        // Si la View está visible, mostrar Snackbar
        if (contenedor.isShown) {
            val colorExito = ContextCompat.getColor(contexto, R.color.success_color)
            snackbarActual = Snackbar.make(
                contenedor,
                mensaje,
                Snackbar.LENGTH_SHORT
            ).apply {
                setBackgroundTint(colorExito)
                setTextColor(ContextCompat.getColor(contexto, R.color.white))
                show()
            }
        } else {
            // Si no está visible, mostrar notificación del sistema
            mostrarNotificacionSistema(contexto, mensaje)
        }
    }

    /**
     * Muestra notificación del sistema (cuando Activity está cerrada)
     */
    private fun mostrarNotificacionSistema(
        contexto: android.content.Context,
        mensaje: String
    ) {
        val channelId = "sincronizacion_channel"
        val notificationId = 999
        
        // Crear canal si es Android 8+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                channelId,
                "Sincronización",
                android.app.NotificationManager.IMPORTANCE_LOW
            )
            val notificationManager = contexto.getSystemService(
                android.content.Context.NOTIFICATION_SERVICE
            ) as android.app.NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
        
        val notification = androidx.core.app.NotificationCompat.Builder(contexto, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Pedido")
            .setContentText(mensaje)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()
        
        val notificationManager = androidx.core.app.NotificationManagerCompat.from(contexto)
        notificationManager.notify(notificationId, notification)
    }

    /**
     * Muestra indicador de sincronización pendiente (sin conexión)
     */
    private fun mostrarIndicadorPendiente(contenedor: View) {
        ocultarIndicador()

        val contexto = contenedor.context
        val mensaje = "📡 No hay conexión, se guardó de manera temporal..."
        val colorAdvertencia = ContextCompat.getColor(contexto, R.color.warning_color)

        snackbarActual = Snackbar.make(
            contenedor,
            mensaje,
            Snackbar.LENGTH_INDEFINITE
        ).apply {
            setBackgroundTint(colorAdvertencia)
            setTextColor(ContextCompat.getColor(contexto, R.color.black))
            setActionTextColor(ContextCompat.getColor(contexto, R.color.text_primary))
            setAction("ENTENDIDO") { dismiss() }
            show()
        }
    }

    /**
     * Muestra indicador de error de conexión
     */
    private fun mostrarIndicadorError(contenedor: View) {
        ocultarIndicador()

        val contexto = contenedor.context
        val mensaje = "⚠️ Problemas de conexión. Cambios guardados localmente."
        val colorError = ContextCompat.getColor(contexto, R.color.error_color)

        snackbarActual = Snackbar.make(
            contenedor,
            mensaje,
            Snackbar.LENGTH_INDEFINITE
        ).apply {
            setBackgroundTint(colorError)
            setTextColor(ContextCompat.getColor(contexto, R.color.white))
            setActionTextColor(ContextCompat.getColor(contexto, R.color.white))
            setAction("ENTENDIDO") { dismiss() }
            show()
        }
    }

    /**
     * Oculta el indicador actual
     */
    fun ocultarIndicador() {
        snackbarActual?.dismiss()
        snackbarActual = null
    }
}
