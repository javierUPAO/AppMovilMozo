package com.donabere.amm.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.donabere.amm.model.Cuenta
import com.donabere.amm.model.DetallePedido
import com.donabere.amm.model.Pedido
import com.donabere.amm.model.enums.EstadoCuenta
import com.donabere.amm.model.enums.EstadoPedido
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.Result.Companion.failure

private const val TAG = "PedidoRepository"


class PedidoRepository {

    private val db         = FirebaseFirestore.getInstance()
    private val pedidosRef = db.collection("pedidos")
    private val mesasRef   = db.collection("mesas")
    private val stockRef   = db.collection("productos")

    // ─── Estado en memoria ───────────────

    private var pedidoActual: Pedido? = null
    private val detallesBorrador = mutableListOf<DetallePedido>()

    private val cuentasBorrador = mutableListOf<Cuenta>()

    private var cuentaActiva: Cuenta? = null
    private var contadorDetalle  = 1
    private var pedidoIdActualMonitoreado: String? = null

    private val _detalles = MutableLiveData<List<DetallePedido>>(emptyList())
    private val _pedido   = MutableLiveData<Pedido?>(null)
    private val _estadoSincronizacion = MutableLiveData<EstadoSincronizacion>(EstadoSincronizacion.SINCRONIZADO)

    fun observarDetalles(): LiveData<List<DetallePedido>> = _detalles
    fun observarPedido():   LiveData<Pedido?>             = _pedido
    fun observarEstadoSincronizacion(): LiveData<EstadoSincronizacion> = _estadoSincronizacion

    fun getDetallesByPedido(pedidoId: String): LiveData<List<DetallePedido>> = _detalles
    fun getPedidoActivoPorMesa(mesaId: String): LiveData<Pedido?>             = _pedido

    // ─── Crear borrador ──────────────────────────────────────────────────────

    /**
     * Crea el documento del pedido en Firestore como PENDIENTE_PREPARACION.
     * Los detalles se guardan solo en memoria hasta confirmarPedido().
     * No espera la red para no bloquear en modo offline.
     */
    suspend fun crearPedidoBorrador(mesasIds: List<String>, mozoId: String): String {
        // Corregido: Asegurar que los IDs tengan el formato "m1", "m2", etc.
        val mesasIdsFormateadas = mesasIds.map { id ->
            if (id.startsWith("m")) id else "m$id"
        }

        val data = hashMapOf(
            "mozoId"     to mozoId,
            "mesasIds"   to mesasIdsFormateadas,
            "estado"     to EstadoPedido.PENDIENTE_PREPARACION.name,
            "totalPagar" to 0.0,
            "creadoEn"   to Timestamp.now() // Corregido: Se guarda como Timestamp nativo de Firebase
        )
        val pedidoId = pedidosRef.document().id

        // Guardar sin esperar (Firestore maneja offline cache)
        pedidosRef.document(pedidoId)
            .set(data)
            .addOnFailureListener { e ->
                Log.w(TAG, "No se pudo guardar borrador en Firestore (offline ok): ${e.message}")
            }

        pedidoActual = Pedido(
            id       = pedidoId,
            mozoId   = mozoId,
            mesasIds = mesasIdsFormateadas,
            estado   = EstadoPedido.PENDIENTE_PREPARACION
        )
        pedidoIdActualMonitoreado = pedidoId
        detallesBorrador.clear()
        contadorDetalle = 1
        iniciarMonitoreoSincronizacion(pedidoId)
        emitirEstado()

        Log.d(TAG, "Borrador creado: $pedidoId para mesas $mesasIdsFormateadas")
        return pedidoId
    }

    // ─── Gestión de detalles (en memoria, sobre el borrador) ─────────────────


    suspend fun agregarItemACuenta(
        productoId: String,
        nombreProducto: String,
        precioUnitario: Double,
        cantidad: Int,
        nota: String,
        imagenUrl: String
    ): Result<String> {

        return try {

            // VALIDAR STOCK
            val stock = obtenerStock(productoId)

            // CREAR CUENTA LOCAL SI NO EXISTE
            if (cuentaActiva == null) {

                cuentaActiva = Cuenta(
                    id = "c_1",
                    nombre = "Cuenta 1",
                    detalles = emptyList(),
                    estadoPago = EstadoCuenta.PENDIENTE
                )
            }

            val cuenta = cuentaActiva!!

            // BUSCAR ITEM EXISTENTE
            val existente = cuenta.detalles.find {
                it.productoId == productoId &&
                        it.nota == nota &&
                        !it.anulado
            }

            val cantidadActual = existente?.cantidad ?: 0

            // VALIDAR STOCK
            if (stock != null && (cantidadActual + cantidad) > stock) {

                return Result.failure(
                    IllegalStateException(
                        "Stock insuficiente. Disponible: ${stock - cantidadActual}"
                    )
                )
            }

            // NUEVA LISTA
            val nuevosDetalles = if (existente != null) {

                cuenta.detalles.map {

                    if (it.id == existente.id) {

                        it.copy(
                            cantidad = it.cantidad + cantidad
                        )

                    } else it
                }

            } else {

                val nuevoDetalle = DetallePedido(
                    id = "d_${contadorDetalle++}",
                    productoId = productoId,
                    nombreProducto = nombreProducto,
                    precioUnitario = precioUnitario,
                    cuentaId = cuenta.id,
                    cantidad = cantidad,
                    nota = nota,
                    imagenProducto = imagenUrl,
                    anulado = false,
                    motivoAnulacion = ""
                )

                cuenta.detalles + nuevoDetalle
            }

            // ACTUALIZAR CUENTA LOCAL
            cuentaActiva = cuenta.copy(
                detalles = nuevosDetalles
            )

            emitirEstado()

            Result.success("OK")

        } catch (e: Exception) {

            Result.failure(e)
        }
    }

    private suspend fun recargarDetallesCuenta(
        pedidoId: String,
        cuentaId: String
    ) {

        val detallesSnapshot = pedidosRef
            .document(pedidoId)
            .collection("cuentas")
            .document(cuentaId)
            .collection("detalles")
            .get()
            .await()

        val detalles = detallesSnapshot.documents.mapNotNull {
            it.toObject(DetallePedido::class.java)
        }.filter { !it.anulado }

        _detalles.postValue(detalles)
    }
    fun actualizarCantidadDetalle(
        detalle: DetallePedido,
        nuevaCantidad: Int
    ) {

        val cuenta = cuentaActiva ?: return

        val nuevosDetalles = if (nuevaCantidad <= 0) {

            cuenta.detalles.filter {
                it.id != detalle.id
            }

        } else {

            cuenta.detalles.map {

                if (it.id == detalle.id) {

                    it.copy(
                        cantidad = nuevaCantidad
                    )

                } else it
            }
        }

        cuentaActiva = cuenta.copy(
            detalles = nuevosDetalles
        )

        emitirEstado()
    }
    fun actualizarNotaDetalle(
        detalle: DetallePedido,
        nuevaNota: String
    ) {

        val cuenta = cuentaActiva ?: return

        val nuevosDetalles = cuenta.detalles.map {

            if (it.id == detalle.id) {

                it.copy(
                    nota = nuevaNota
                )

            } else it
        }

        cuentaActiva = cuenta.copy(
            detalles = nuevosDetalles
        )

        emitirEstado()
    }
    fun eliminarDetalle(
        detalle: DetallePedido
    ) {

        val cuenta = cuentaActiva ?: return

        val nuevosDetalles = cuenta.detalles.map {

            if (it.id == detalle.id) {

                it.copy(
                    anulado = true,
                    motivoAnulacion = "Eliminado desde carrito"
                )

            } else it
        }

        cuentaActiva = cuenta.copy(
            detalles = nuevosDetalles
        )

        emitirEstado()
    }
    fun restaurarDetalle(
        detalle: DetallePedido
    ) {

        val cuenta = cuentaActiva ?: return

        val nuevosDetalles = cuenta.detalles.map {

            if (it.id == detalle.id) {

                it.copy(
                    anulado = false,
                    motivoAnulacion = ""
                )

            } else it
        }

        cuentaActiva = cuenta.copy(
            detalles = nuevosDetalles
        )

        emitirEstado()
    }

    suspend fun obtenerStock(productoId: String): Int? {
        return try {
            // Leer del cache local para no bloquear en modo offline
            val doc = withTimeoutOrNull(300) {
                stockRef.document(productoId).get(Source.CACHE).await()
            }
            if (doc?.exists() == true) doc.getLong("stock")?.toInt() else null
        } catch (e: Exception) {
            Log.w(TAG, "No se puede obtener stock (offline ok): ${e.message}")
            null
        }
    }


    // ─── Confirmar pedido (enviar a cocina) ───────────────────────────────────

    /**
     * 1. Guarda los detalles en la subcolección Firestore
     * 2. Actualiza el estado del pedido a PENDIENTE_PREPARACION
     * 3. Marca las mesas como OCUPADA en la colección 'mesas'
     * 4. Descuenta el stock
     */
    suspend fun confirmarPedido(
        mozoId: String,
        mesasIds: List<String>
    ): Result<Unit> {
        return confirmarPedidoOnline(mozoId, mesasIds)
    }

    suspend fun confirmarPedidoOnline(
        mozoId: String,
        mesasIds: List<String>
    ): Result<Unit> {
        return try {
            val cuenta = cuentaActiva
                ?: return Result.failure(IllegalStateException("No hay cuenta activa"))

            val detallesActivos = cuenta.detalles.filter { !it.anulado }
            if (detallesActivos.isEmpty()) {
                return Result.failure(IllegalStateException("El pedido no tiene productos"))
            }

            val total = detallesActivos.sumOf {
                it.precioUnitario * it.cantidad
            }

            val pedidoRef = pedidosRef.document()
            val pedido = Pedido(
                id = pedidoRef.id,
                estado = EstadoPedido.PENDIENTE_PREPARACION,
                totalPagar = total,
                mesasIds = mesasIds,
                mozoId = mozoId,
                cuentas = emptyList()
            )

            val batch = db.batch()
            batch.set(pedidoRef, pedido)

            val cuentaRef = pedidoRef
                .collection("cuentas")
                .document(cuenta.id)

            batch.set(
                cuentaRef,
                cuenta.copy(detalles = emptyList())
            )

            detallesActivos.forEach { detalle ->
                val detalleRef = cuentaRef
                    .collection("detalles")
                    .document(detalle.id)
                batch.set(detalleRef, detalle)
            }

            mesasIds.forEach { mesaId ->
                val mesaRef = mesasRef.document(mesaId.trim())
                batch.update(
                    mesaRef,
                    mapOf(
                        "estado" to "OCUPADA",
                        "pedidoId" to pedido.id
                    )
                )
            }

            batch.commit().await()

            detallesActivos.forEach { detalle ->
                descontarStock(
                    detalle.productoId,
                    detalle.cantidad
                )
            }

            cuentaActiva = null
            _detalles.postValue(emptyList())
            pedidoActual = null
            _pedido.postValue(null)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun confirmarPedidoOffline(
        mozoId: String,
        mesasIds: List<String>
    ): Result<Unit> {
        return try {
            val cuenta = cuentaActiva
                ?: return Result.failure(IllegalStateException("No hay cuenta activa"))

            val detallesActivos = cuenta.detalles.filter { !it.anulado }
            if (detallesActivos.isEmpty()) {
                return Result.failure(IllegalStateException("El pedido no tiene productos"))
            }

            val total = detallesActivos.sumOf {
                it.precioUnitario * it.cantidad
            }

            val pedidoRef = pedidosRef.document()
            val pedido = Pedido(
                id = pedidoRef.id,
                estado = EstadoPedido.PENDIENTE_PREPARACION,
                totalPagar = total,
                mesasIds = mesasIds,
                mozoId = mozoId,
                cuentas = emptyList()
            )

            val batch = db.batch()
            batch.set(pedidoRef, pedido)

            val cuentaRef = pedidoRef
                .collection("cuentas")
                .document(cuenta.id)

            batch.set(
                cuentaRef,
                cuenta.copy(detalles = emptyList())
            )

            detallesActivos.forEach { detalle ->
                val detalleRef = cuentaRef
                    .collection("detalles")
                    .document(detalle.id)
                batch.set(detalleRef, detalle)
            }

            mesasIds.forEach { mesaId ->
                val mesaRef = mesasRef.document(mesaId.trim())
                batch.update(
                    mesaRef,
                    mapOf(
                        "estado" to "OCUPADA",
                        "pedidoId" to pedido.id
                    )
                )
            }

            batch.commit()
                .addOnFailureListener { e ->
                    Log.w(TAG, "Commit offline fallido: ${e.message}")
                }

            cuentaActiva = null
            _detalles.postValue(emptyList())
            pedidoActual = null
            _pedido.postValue(null)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ─── División de cuentas ──────────────────────────────────────────────────

    suspend fun dividirCuentas(
        pedidoId: String,
        division: Map<String, List<DetallePedido>>
    ): Result<List<Cuenta>> {
        return try {
            val cuentasCol = pedidosRef.document(pedidoId).collection("cuentas")
            val batch      = db.batch()
            val cuentas    = mutableListOf<Cuenta>()
            var letra      = 'A'

            division.forEach { (_, detalles) ->
                val cuentaRef  = cuentasCol.document()
                val nombre     = "Cuenta $letra"
                val totalCuenta = detalles.sumOf { it.subtotal }
                letra++

                batch.set(cuentaRef, hashMapOf(
                    "nombre"      to nombre,
                    "totalCuenta" to totalCuenta,
                    "estadoPago"  to EstadoCuenta.PENDIENTE.name,
                    "detallesIds" to detalles.map { it.id }
                ))

                cuentas.add(
                    Cuenta(
                        id         = cuentaRef.id,
                        nombre     = nombre,
                        detalles   = detalles,
                        estadoPago = EstadoCuenta.PENDIENTE
                    )
                )
            }

            batch.commit().await()
            Log.d(TAG, "Cuentas creadas: ${cuentas.size} para pedido $pedidoId")
            Result.success(cuentas)

        } catch (e: Exception) {
            Log.e(TAG, "Error al dividir cuentas: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun marcarCuentaPagada(pedidoId: String, cuentaId: String): Result<Unit> {
        return try {
            val cuentaRef = pedidosRef.document(pedidoId)
                .collection("cuentas").document(cuentaId)
            cuentaRef.update("estadoPago", EstadoCuenta.PAGADO.name).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ─── Privados ─────────────────────────────────────────────────────────────

    private fun emitirEstado() {

        val cuenta = cuentaActiva ?: return

        val activos = cuenta.detalles.filter {
            !it.anulado
        }

        _detalles.postValue(activos)

        val total = activos.sumOf {
            it.subtotal
        }

        pedidoActual = pedidoActual?.copy(
            totalPagar = total
        )

        _pedido.postValue(pedidoActual)
    }
    private suspend fun descontarStock(productoId: String, cantidad: Int) {
        try {
            val docRef     = stockRef.document(productoId)
            val stockActual = docRef.get().await().getLong("stock")?.toInt() ?: return

            docRef.update("stock", maxOf(0, stockActual - cantidad)).await()

        } catch (e: Exception) {
            Log.e(TAG, "Error descontando stock $productoId: ${e.message}")
        }
    }

    /**
     * Descuenta stock SIN BLOQUEAR - fire and forget
     */
    private fun descontarStockAsync(productoId: String, cantidad: Int) {
        val docRef = stockRef.document(productoId)
        
        // Fire and forget - no esperar
        docRef.get()
            .addOnSuccessListener { snapshot ->
                val stockActual = snapshot.getLong("stock")?.toInt() ?: return@addOnSuccessListener
                docRef.update("stock", maxOf(0, stockActual - cantidad))
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error actualizando stock $productoId: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error obteniendo stock $productoId: ${e.message}")
            }
    }

    // ─── Obtener pedidos del mozo del día ───────────────────────────────────

    /**
     * Obtiene todos los pedidos del mozo del día actual
     * Retorna LiveData<List<Pedido>> para observar cambios
     */
    fun obtenerPedidosDelMozoDiaActual(mozoId: String): LiveData<List<Pedido>> {
        val result = MutableLiveData<List<Pedido>>()

        try {
            val hoy = com.google.firebase.Timestamp.now()
            val startOfDay = com.google.firebase.Timestamp(
                com.google.firebase.Timestamp(hoy.seconds - (hoy.seconds % 86400), 0).seconds,
                0
            )
            val endOfDay = com.google.firebase.Timestamp(
                startOfDay.seconds + 86400,
                0
            )

            pedidosRef
                .whereEqualTo("mozoId", mozoId)
                .whereGreaterThanOrEqualTo("creadoEn", startOfDay)
                .whereLessThan("creadoEn", endOfDay)
                .orderBy("creadoEn", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, exception ->
                    if (exception != null) {
                        Log.e(TAG, "Error obteniendo pedidos: ${exception.message}")
                        return@addSnapshotListener
                    }

                    val pedidos = snapshot?.documents?.mapNotNull { doc ->
                        try {
                            Pedido(
                                id = doc.id,
                                mozoId = doc.getString("mozoId") ?: "",
                                mesasIds = (doc.get("mesasIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                                estado = EstadoPedido.valueOf(doc.getString("estado") ?: "BORRADOR"),
                                totalPagar = doc.getDouble("totalPagar") ?: 0.0,
                                creadoEn = doc.getTimestamp("creadoEn") ?: com.google.firebase.Timestamp.now()
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Error mapeando pedido: ${e.message}")
                            null
                        }
                    } ?: emptyList()
                    result.postValue(pedidos)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error en obtenerPedidosDelMozoDiaActual: ${e.message}")
            result.postValue(emptyList())
        }

        return result
    }

    // ─── Obtener detalles de un pedido ────────────────────────────────────

    /**
     * Obtiene todos los detalles (platos) de un pedido específico
     * Retorna LiveData<List<DetallePedido>>
     */
    fun obtenerDetallesPedido(
        pedidoId: String
    ): LiveData<List<DetallePedido>> {

        val result = MutableLiveData<List<DetallePedido>>()

        try {

            pedidosRef
                .document(pedidoId)
                .collection("cuentas")
                .addSnapshotListener { cuentasSnapshot, exception ->

                    if (exception != null) {

                        Log.e(
                            TAG,
                            "Error obteniendo cuentas: ${exception.message}"
                        )

                        return@addSnapshotListener
                    }

                    if (cuentasSnapshot == null) {

                        result.postValue(emptyList())

                        return@addSnapshotListener
                    }

                    val todosLosDetalles = mutableListOf<DetallePedido>()

                    val cuentas = cuentasSnapshot.documents

                    if (cuentas.isEmpty()) {

                        result.postValue(emptyList())

                        return@addSnapshotListener
                    }

                    var cuentasProcesadas = 0

                    cuentas.forEach { cuentaDoc ->

                        cuentaDoc.reference
                            .collection("detalles")
                            .get()
                            .addOnSuccessListener { detallesSnapshot ->

                                val detalles = detallesSnapshot.documents.mapNotNull { doc ->

                                    try {

                                        DetallePedido(
                                            id = doc.id,
                                            productoId = doc.getString("productoId") ?: "",
                                            nombreProducto = doc.getString("nombreProducto") ?: "",
                                            precioUnitario = doc.getDouble("precioUnitario") ?: 0.0,
                                            cantidad = (doc.getLong("cantidad") ?: 1).toInt(),
                                            nota = doc.getString("nota") ?: "",
                                            imagenProducto = doc.getString("imagenProducto") ?: "",
                                            anulado = doc.getBoolean("anulado") ?: false,
                                            cuentaId = cuentaDoc.id
                                        )

                                    } catch (e: Exception) {

                                        Log.e(
                                            TAG,
                                            "Error mapeando detalle: ${e.message}"
                                        )

                                        null
                                    }
                                }

                                todosLosDetalles.addAll(
                                    detalles.filter { !it.anulado }
                                )

                                cuentasProcesadas++

                                // Cuando termine todas las cuentas
                                if (cuentasProcesadas == cuentas.size) {

                                    result.postValue(
                                        todosLosDetalles
                                    )
                                }
                            }
                            .addOnFailureListener { e ->

                                Log.e(
                                    TAG,
                                    "Error obteniendo detalles: ${e.message}"
                                )

                                cuentasProcesadas++

                                if (cuentasProcesadas == cuentas.size) {

                                    result.postValue(
                                        todosLosDetalles
                                    )
                                }
                            }
                    }
                }

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Error en obtenerDetallesPedido: ${e.message}"
            )

            result.postValue(emptyList())
        }

        return result
    }

    // ─── Actualizar estado de un detalle ───────────────────────────────────

    /**
     * Obtiene un detalle específico
     */
    suspend fun obtenerDetallePedido(
        pedidoId: String,
        detalleId: String
    ): DetallePedido? {
        return try {
            pedidosRef.document(pedidoId)
                .collection("detalles")
                .document(detalleId)
                .get()
                .await()
                .toObject(DetallePedido::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo detalle: ${e.message}")
            null
        }
    }

    /**
     * Elimina un plato del pedido
     */
    suspend fun eliminarDetallePedido(
        pedidoId: String,
        detalleId: String
    ): Result<Unit> {
        return try {
            pedidosRef.document(pedidoId)
                .collection("detalles")
                .document(detalleId)
                .delete()
                .await()
            Log.d(TAG, "Detalle eliminado: $detalleId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error eliminando detalle: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Cambia el estado del Pedido completo al siguiente estado
     * BORRADOR → PENDIENTE_PREPARACION (Enviar a cocina)
     * PENDIENTE_PREPARACION → COCINA
     * COCINA → LISTO_PARA_ENTREGAR
     * LISTO_PARA_ENTREGAR → ATENDIDO
     * ATENDIDO → PAGADO (y libera mesas automáticamente)
     */
    suspend fun cambiarEstadoPedido(pedidoId: String): Result<Unit> {
        return try {
            val pedido = pedidosRef.document(pedidoId).get().await().toObject(Pedido::class.java)
            if (pedido == null) {
                return Result.failure(IllegalStateException("Pedido no encontrado"))
            }

            val nuevoEstado = when (pedido.estado) {
                EstadoPedido.BORRADOR -> EstadoPedido.PENDIENTE_PREPARACION
                EstadoPedido.COMANDADO -> EstadoPedido.PENDIENTE_PREPARACION
                EstadoPedido.PENDIENTE_PREPARACION -> EstadoPedido.COCINA
                EstadoPedido.COCINA -> EstadoPedido.LISTO_PARA_ENTREGAR
                EstadoPedido.LISTO_PARA_ENTREGAR -> EstadoPedido.ATENDIDO
                EstadoPedido.ATENDIDO -> EstadoPedido.PAGADO
                EstadoPedido.PAGADO -> EstadoPedido.PAGADO // Ya terminado
                EstadoPedido.PAGADO_PARCIAL -> EstadoPedido.PAGADO
            }

            // Si el nuevo estado es PAGADO, liberar mesas automáticamente
            if (nuevoEstado == EstadoPedido.PAGADO) {
                val batch = db.batch()

                // Marcar todas las mesas como LIBRE
                pedido.mesasIds.forEach { mesaId ->
                    batch.update(mesasRef.document(mesaId), "estado", "LIBRE")
                }

                // Actualizar estado del pedido a PAGADO
                batch.update(pedidosRef.document(pedidoId), "estado", nuevoEstado)

                batch.commit().await()
                Log.d(TAG, "Estado del Pedido cambió a: $nuevoEstado y mesas liberadas: ${pedido.mesasIds}")
            } else {
                // Para otros estados, solo actualizar sin liberar mesas
                pedidosRef.document(pedidoId)
                    .update("estado", nuevoEstado)
                    .await()
                Log.d(TAG, "Estado del Pedido cambió a: $nuevoEstado")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error cambiando estado del pedido: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Libera las mesas asociadas al pedido (marca como LIBRE)
     */
    suspend fun liberarMesas(pedido: Pedido): Result<Unit> {
        return try {
            val batch = db.batch()

            // Marcar todas las mesas como LIBRE
            pedido.mesasIds.forEach { mesaId ->
                batch.update(mesasRef.document(mesaId), "estado", "LIBRE")
            }

            // Opcionalmente marcar el pedido como PAGADO o cerrar
            batch.update(pedidosRef.document(pedido.id), "estado", EstadoPedido.PAGADO)

            batch.commit().await()
            Log.d(TAG, "Mesas liberadas: ${pedido.mesasIds}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error liberando mesas: ${e.message}")
            Result.failure(e)
        }
    }


    // ─── Monitoreo de sincronización ──────────────────────────────────────────

    /**
     * Inicia un listener que monitorea si hay escrituras pendientes
     * en Firestore (cuando se pierde conexión).
     */
    private fun iniciarMonitoreoSincronizacion(pedidoId: String) {
        pedidosRef.document(pedidoId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error en listener de sincronización: ${error.message}")
                _estadoSincronizacion.postValue(EstadoSincronizacion.ERROR_CONEXION)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val tienePendientes = snapshot.metadata.hasPendingWrites()
                val nuevoEstado = if (tienePendientes) {
                    EstadoSincronizacion.PENDIENTE_SINCRONIZACION
                } else {
                    EstadoSincronizacion.SINCRONIZADO
                }
                _estadoSincronizacion.postValue(nuevoEstado)
                Log.d(TAG, "Estado sincronización: $nuevoEstado (pendientes: $tienePendientes)")
            }
        }
    }

    /**
     * Verifica si hay escrituras pendientes para el pedido actual
     */
    suspend fun verificarEscriturasPendientes(pedidoId: String): Boolean {
        return try {
            val snapshot = pedidosRef.document(pedidoId).get().await()
            snapshot.metadata.hasPendingWrites()
        } catch (e: Exception) {
            Log.e(TAG, "Error verificando escrituras pendientes: ${e.message}")
            false
        }
    }

    // ─── Enum para estado de sincronización ────────────────────────────────────

    enum class EstadoSincronizacion {
        SINCRONIZADO,
        PENDIENTE_SINCRONIZACION,
        ERROR_CONEXION
    }
}