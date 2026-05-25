package com.donabere.amm.model.enums

enum class EstadoPedido {
    COMANDADO,
    PENDIENTE_PREPARACION,
    PENDIENTE_CORRECCION_STOCK,
    COCINA,
    LISTO_PARA_ENTREGAR,
    ATENDIDO,
    PAGADO,
    PAGADO_PARCIAL,
    PAGO_EN_PROCESO,
    BORRADOR,
}