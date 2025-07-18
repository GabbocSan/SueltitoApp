package com.example.suletitoapp

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

fun cargarHistorialPasajero(
    pasajeroId: String,
    onResult: (List<Pago>, String) -> Unit
) {
    val db = FirebaseDatabase.getInstance().reference

    db.child("pagos")
        .orderByChild("pasajeroId")
        .equalTo(pasajeroId)
        .addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val pagos = mutableListOf<Pago>()

                for (pagoSnapshot in snapshot.children) {
                    try {
                        val pago = pagoSnapshot.getValue(Pago::class.java)
                        pago?.let { pagos.add(it) }
                    } catch (e: Exception) {
                        Log.e("HISTORIAL", "Error al parsear pago: ${e.message}")
                    }
                }

                onResult(pagos, "")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("HISTORIAL", "Error al cargar historial: ${error.message}")
                onResult(emptyList(), "Error al cargar historial: ${error.message}")
            }
        })
}