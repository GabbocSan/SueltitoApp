package com.example.suletitoapp

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

fun cargarHistorialConductor(
    conductorId: String,
    onResult: (List<Pago>, String) -> Unit
) {
    val db = FirebaseDatabase.getInstance().reference

    db.child("pagos")
        .orderByChild("conductorId")
        .equalTo(conductorId)
        .addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val cobros = mutableListOf<Pago>()

                for (cobroSnapshot in snapshot.children) {
                    try {
                        val cobro = cobroSnapshot.getValue(Pago::class.java)
                        cobro?.let { cobros.add(it) }
                    } catch (e: Exception) {
                        Log.e("HISTORIAL", "Error al parsear cobro: ${e.message}")
                    }
                }

                onResult(cobros, "")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("HISTORIAL", "Error al cargar historial: ${error.message}")
                onResult(emptyList(), "Error al cargar historial: ${error.message}")
            }
        })
}