package com.example.suletitoapp

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color

@Composable
fun RecargaSaldoScreen(
    saldoActual: Double,
    onRecargar: (Double) -> Unit,
    onCancelar: () -> Unit
) {
    var montoTexto by remember { mutableStateOf("") }
    var errorMensaje by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Recargar Saldo",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Mostrar saldo actual
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Saldo actual",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Bs. $saldoActual",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Campo para ingresar monto
        OutlinedTextField(
            value = montoTexto,
            onValueChange = {
                montoTexto = it
                errorMensaje = null // Limpiar error al escribir
            },
            label = { Text("Monto a recargar") },
            placeholder = { Text("Ingrese el monto en Bs.") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = {
                Text(
                    text = "Bs.",
                    modifier = Modifier.padding(start = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Mostrar mensaje de error si existe
        errorMensaje?.let { mensaje ->
            Text(
                text = mensaje,
                color = MaterialTheme.colorScheme.error,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Botones de acción
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Botón Cancelar
            OutlinedButton(
                onClick = { onCancelar() },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Atras")
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Botón Recargar
            Button(
                onClick = {
                    val monto = montoTexto.toDoubleOrNull()

                    when {
                        montoTexto.isBlank() -> {
                            errorMensaje = "Por favor ingrese un monto"
                        }
                        monto == null || monto <= 0 -> {
                            errorMensaje = "Ingrese un monto válido mayor a 0"
                        }
                        monto > 1000 -> {
                            errorMensaje = "El monto máximo de recarga es Bs. 1000"
                        }
                        else -> {
                            onRecargar(monto)
                        }
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Recargar")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Información adicional
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Información importante:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "• Monto mínimo: Bs. 1\n• Monto máximo: Bs. 1000\n• La recarga se procesará inmediatamente",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}