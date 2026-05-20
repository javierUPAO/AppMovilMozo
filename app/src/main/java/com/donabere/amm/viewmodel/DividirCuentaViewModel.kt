package com.donabere.amm.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.donabere.amm.model.Cuenta
import com.donabere.amm.model.DetallePedido
import com.donabere.amm.model.ItemDivisible
import com.donabere.amm.model.PersonaCuenta
import com.donabere.amm.model.enums.EstadoCuenta
import com.donabere.amm.repository.PedidoRepository
import kotlinx.coroutines.launch

class DividirCuentaViewModel(
    private val repository: PedidoRepository,
    private val pedidoId: String,
    detalles: List<DetallePedido>
) : ViewModel() {

    // ── Personas ────────────────────────────────────────────────────────────
    private val _personas = MutableLiveData<List<PersonaCuenta>>(
        listOf(
            PersonaCuenta(0, "Persona 1"),
            PersonaCuenta(1, "Persona 2")
        )
    )
    val personas: LiveData<List<PersonaCuenta>> = _personas

    // ── Ítems divisibles (1 por unidad) ─────────────────────────────────────
    private val _items = MutableLiveData<List<ItemDivisible>>(
        detalles.flatMap { detalle ->
            (1..detalle.cantidad).map { unidad ->
                ItemDivisible(
                    detalleId      = detalle.id,
                    unidad         = unidad,
                    totalUnidades  = detalle.cantidad,
                    nombreProducto = detalle.nombreProducto,
                    precio         = detalle.precioUnitario
                )
            }
        }
    )
    val items: LiveData<List<ItemDivisible>> = _items

    // ── UI state ─────────────────────────────────────────────────────────────
    sealed class UiState {
        object Idle    : UiState()
        object Loading : UiState()
        object Guardado : UiState()
        data class Error(val msg: String) : UiState()
    }

    private val _uiState = MutableLiveData<UiState>(UiState.Idle)
    val uiState: LiveData<UiState> = _uiState

    // ── Resumen calculado ────────────────────────────────────────────────────
    /** Para cada persona: lista de ítems asignados y su subtotal */
    val resumen: LiveData<List<Pair<PersonaCuenta, List<ItemDivisible>>>>
        get() {
            val lista = _personas.value ?: emptyList()
            val items = _items.value ?: emptyList()
            return MutableLiveData(
                lista.map { persona ->
                    persona to items.filter { it.cuentaPersonaIndex == persona.index }
                }
            )
        }

    // ── Personas ─────────────────────────────────────────────────────────────

    fun agregarPersona() {
        val actual = _personas.value?.toMutableList() ?: mutableListOf()
        val nuevoIndex = (actual.maxOfOrNull { it.index } ?: -1) + 1
        actual.add(PersonaCuenta(nuevoIndex, "Persona ${nuevoIndex + 1}"))
        _personas.value = actual
    }

    fun eliminarPersona(index: Int) {
        val actual = _personas.value?.toMutableList() ?: return
        if (actual.size <= 1) return  // mínimo 1 persona
        actual.removeAll { it.index == index }
        _personas.value = actual

        // Desasignar ítems que tenían a esta persona
        val items = _items.value?.map { item ->
            if (item.cuentaPersonaIndex == index) item.copy(cuentaPersonaIndex = null)
            else item
        } ?: return
        _items.value = items
    }

    fun renombrarPersona(index: Int, nuevoNombre: String) {
        val actual = _personas.value?.toMutableList() ?: return
        val i = actual.indexOfFirst { it.index == index }
        if (i == -1) return
        actual[i] = actual[i].copy(nombre = nuevoNombre.ifBlank { "Persona ${index + 1}" })
        _personas.value = actual
    }

    // ── Asignación de ítems ──────────────────────────────────────────────────

    /**
     * Asigna (o desasigna) un ítem a una persona.
     * Si ya estaba asignado a esa misma persona → lo desasigna (toggle).
     */
    fun asignarItem(itemDetalleId: String, unidad: Int, personaIndex: Int?) {
        val lista = _items.value?.toMutableList() ?: return
        val i = lista.indexOfFirst {
            it.detalleId == itemDetalleId && it.unidad == unidad
        }
        if (i == -1) return

        val item = lista[i]
        lista[i] = item.copy(
            cuentaPersonaIndex = if (item.cuentaPersonaIndex == personaIndex) null
            else personaIndex
        )
        _items.value = lista
    }

    // ── Totales por persona ───────────────────────────────────────────────────

    fun totalPersona(personaIndex: Int): Double =
        _items.value
            ?.filter { it.cuentaPersonaIndex == personaIndex }
            ?.sumOf { it.precio }
            ?: 0.0

    fun itemsSinAsignar(): List<ItemDivisible> =
        _items.value?.filter { it.cuentaPersonaIndex == null } ?: emptyList()

    // ── Guardar en Firestore ──────────────────────────────────────────────────

    fun guardarDivision() {
        val personas = _personas.value ?: return
        val items    = _items.value   ?: return

        // Validar que todos los ítems estén asignados
        val sinAsignar = items.filter { it.cuentaPersonaIndex == null }
        if (sinAsignar.isNotEmpty()) {
            _uiState.value = UiState.Error(
                "Faltan ${sinAsignar.size} ítem(s) por asignar"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = UiState.Loading

            // Construir el mapa personaIndex → lista de DetallePedido ficticios
            // (agrupamos unidades del mismo producto asignadas a la misma persona)
            val division = mutableMapOf<String, List<DetallePedido>>()

            personas.forEach { persona ->
                val itemsPersona = items.filter { it.cuentaPersonaIndex == persona.index }

                // Agrupar por detalleId para reconstruir DetallePedido con la cantidad correcta
                val detallesPersona = itemsPersona
                    .groupBy { it.detalleId }
                    .map { (detalleId, unidades) ->
                        DetallePedido(
                            id             = "${detalleId}_p${persona.index}",
                            productoId     = detalleId,  // referencia al detalle original
                            nombreProducto = unidades.first().nombreProducto,
                            precioUnitario = unidades.first().precio,
                            cantidad       = unidades.size,
                            cuentaId       = persona.index.toString()
                        )
                    }

                if (detallesPersona.isNotEmpty()) {
                    division[persona.index.toString()] = detallesPersona
                }
            }

            // Persistir en Firestore via PedidoRepository
            repository.dividirCuentas(pedidoId, division).fold(
                onSuccess = { _uiState.value = UiState.Guardado },
                onFailure = { e ->
                    _uiState.value = UiState.Error(e.message ?: "Error al guardar")
                }
            )
        }
    }

    fun resetUiState() { _uiState.value = UiState.Idle }

    // ── Factory ──────────────────────────────────────────────────────────────
    class Factory(
        private val repository: PedidoRepository,
        private val pedidoId: String,
        private val detalles: List<DetallePedido>
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(DividirCuentaViewModel::class.java))
            return DividirCuentaViewModel(repository, pedidoId, detalles) as T
        }
    }
}