package com.example.suletitoapp

class Pago (
    val id: String = "",
    val pasajeroId: String = "",
    val conductorId: String = "",
    val pasajeroNombre: String = "",
    val conductorNombre: String = "",
    val monto: Double = 0.0,
    val fecha: String = "",
    val hora: String = "",
    val timestamp: Long = 0L
)