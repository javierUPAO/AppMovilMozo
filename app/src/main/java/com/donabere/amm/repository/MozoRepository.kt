package com.donabere.amm.repository
import android.util.Log
import kotlinx.coroutines.tasks.await
import com.donabere.amm.model.Mozo
import com.google.firebase.firestore.FirebaseFirestore

class MozoRepository {

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    suspend fun obtenerMozoPorUsuarioId(usuarioId: String): Result<Mozo> {
        return try {

            val snapshot = db.collection("mozo")
                .whereEqualTo("usuarioId", usuarioId)
                .get()
                .await()

            val mozo = snapshot.documents
                .firstOrNull()
                ?.toObject(Mozo::class.java)

            if (mozo != null) {
                Result.success(mozo)
            } else {
                Result.failure(Exception("Mozo no encontrado"))
            }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}