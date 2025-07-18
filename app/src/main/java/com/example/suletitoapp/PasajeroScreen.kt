package com.example.suletitoapp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp


@Composable
fun PasajeroScreen(nombre: String,
                   saldo: Double,
                   onCerrarSesion: () -> Unit,
                   onRecargarSaldo: () -> Unit,
                   onPagarNFC: () -> Unit,
                   onVerHistorial: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "¡Bienvenido Pasajero!",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Hola, $nombre",
            fontSize = 18.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

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
                    text = "Saldo disponible",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Bs. $saldo",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Botones para funcionalidades futuras
        Button(
            onClick = {onPagarNFC()},
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Pagar con NFC")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { onRecargarSaldo() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Recargar saldo")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {onVerHistorial()},
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Ver historial")
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Botón de cerrar sesión
        OutlinedButton(
            onClick = { onCerrarSesion() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text("Cerrar sesión")
        }
    }
}