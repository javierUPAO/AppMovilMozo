package com.donabere.amm.repository

import android.util.Log
import com.donabere.amm.model.Producto
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

private const val TAG = "ProductoRepository"

class ProductoRepository {
    private val db = FirebaseFirestore.getInstance()
    private val productosRef = db.collection("productos")

    suspend fun obtenerProductos(): Result<List<Producto>> {
        return try {
            val snapshot = productosRef.get().await()
            val productos = snapshot.documents.mapNotNull { doc ->
                try {
                    Producto(
                        id = doc.id,
                        nombre = doc.getString("nombre") ?: "",
                        descripcion = doc.getString("descripcion") ?: "",
                        precio = doc.getDouble("precio") ?: 0.0,
                        imagen = doc.getString("imagen") ?: "",
                        tipo = com.donabere.amm.model.enums.TipoProducto.valueOf(
                            doc.getString("tipo") ?: "PLATO"
                        ),
                        stock = (doc.getLong("stock") ?: 0L).toInt()
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error mapeando producto ${doc.id}: ${e.message}")
                    null
                }
            }
            Result.success(productos)
        } catch (e: Exception) {
            Log.e(TAG, "Error al cargar productos: ${e.message}")
            Result.failure(e)
        }
    }
}
