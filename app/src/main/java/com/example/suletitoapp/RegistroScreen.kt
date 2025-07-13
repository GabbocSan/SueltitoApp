package com.example.suletitoapp

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.suletitoapp.model.Usuario
import androidx.compose.material3.TextButton


@Composable
fun RegistroScreen(
    onRegistrar: (Usuario) -> Unit,
    onVolverALogin: () -> Unit
) {
    var nombres by remember { mutableStateOf("") }
    var apellidos by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var rol by remember { mutableStateOf("Pasajero") }

    var errorMensaje by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        OutlinedTextField(
            value = nombres,
            onValueChange = { nombres = it },
            label = { Text("Nombres") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = apellidos,
            onValueChange = { apellidos = it },
            label = { Text("Apellidos") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = telefono,
            onValueChange = { telefono = it },
            label = { Text("Teléfono") },
            placeholder = { Text("+59171234567") },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("Pasajero", "Chofer").forEach { tipo ->
                Button(
                    onClick = { rol = tipo },
                    enabled = rol != tipo
                ) {
                    Text(tipo)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        errorMensaje?.let {
            Text(
                text = it,
                color = Color.Red,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        Button(
            onClick = {
                if (nombres.isBlank() || apellidos.isBlank() || telefono.isBlank()) {
                    errorMensaje = "Por favor completa todos los campos."
                } else if (!telefono.matches(Regex("^\\+\\d{11,15}\$"))) {
                    errorMensaje = "Número de teléfono inválido. Usa el formato +591..."
                } else {
                    errorMensaje = null
                    onRegistrar(Usuario(nombres, apellidos, rol, telefono, saldo = 0.0))
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Registrar")
        }
        TextButton(
            onClick = { onVolverALogin() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("¿Ya tienes cuenta? Iniciar sesión")
        }

    }
}
