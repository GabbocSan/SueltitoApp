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

@Composable
fun NFCPagoScreen(
    pasajeroNombre: String,
    saldoActual: Double,
    onPagar: (Double) -> Unit,
    onCancelar: () -> Unit,
    isProcessing: Boolean = false,
    mensaje: String = ""
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
            text = "Pago con NFC",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Información del pasajero
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
                    text = "Pasajero: $pasajeroNombre",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Saldo disponible: Bs. $saldoActual",
                    fontSize = 18.sp,
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
                errorMensaje = null
            },
            label = { Text("Monto a pagar") },
            placeholder = { Text("Ingrese el monto en Bs.") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            enabled = !isProcessing,
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

        // Mostrar mensaje de error
        errorMensaje?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Mostrar estado del proceso
        if (isProcessing) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Acerca tu teléfono a la etiqueta NFC del conductor...",
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            // Botones de acción
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
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
                            monto > saldoActual -> {
                                errorMensaje = "Saldo insuficiente"
                            }
                            monto > 100 -> {
                                errorMensaje = "El monto máximo de pago es Bs. 100"
                            }
                            else -> {
                                onPagar(monto)
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Pagar")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Mostrar mensajes del sistema
        if (mensaje.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (mensaje.contains("exitoso"))
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = mensaje,
                    modifier = Modifier.padding(16.dp),
                    color = if (mensaje.contains("exitoso"))
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Instrucciones
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
                    text = "Instrucciones:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "1. Ingresa el monto a pagar\n" +
                            "2. Presiona 'Pagar'\n" +
                            "3. Acerca tu teléfono a la etiqueta NFC del conductor\n" +
                            "4. El pago se procesará automáticamente",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}