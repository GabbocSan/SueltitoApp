package com.example.suletitoapp

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.suletitoapp.ui.theme.SuletitoAppTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.FirebaseException
import java.util.concurrent.TimeUnit
import com.google.firebase.database.FirebaseDatabase
import com.example.suletitoapp.model.Usuario



class MainActivity : ComponentActivity() {

    //Variables de Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var storedVerificationId: String
    private lateinit var resendToken: PhoneAuthProvider.ForceResendingToken

    //Inicio del Programa
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        enableEdgeToEdge()

        setContent {
            SuletitoAppTheme {
                val pantallaActual = remember { mutableStateOf("login") }

                when (pantallaActual.value) {
                    "login" -> LoginScreen(
                        onSendCode = { phone -> sendVerificationCode(phone) },
                        onVerifyCode = { code -> verifyCode(code) },
                        onCambiarAPantallaRegistro = { pantallaActual.value = "registro" }
                    )

                    "registro" -> RegistroScreen(
                        onRegistrar = { usuario -> registrarUsuario(usuario) },
                        onVolverALogin = { pantallaActual.value = "login" }
                    )
                }
            }
        }
    }

    private fun sendVerificationCode(phoneNumber: String) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this) // MainActivity
            .setCallbacks(callbacks)
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    //Callback de Firebase
    private val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        //Clase para manejar las respeustas de firebase
        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            signInWithPhoneAuthCredential(credential)
        }

        //Si Firebase detecta el código automáticamente (SMS recibido), inicia sesión.
        override fun onVerificationFailed(e: FirebaseException) {
            Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }

        //Si algo falla (número inválido, red, etc.), muestra un mensaje.
        override fun onCodeSent(
            verificationId: String,
            token: PhoneAuthProvider.ForceResendingToken
        ) {
            storedVerificationId = verificationId
            resendToken = token
            Toast.makeText(this@MainActivity, "Código enviado", Toast.LENGTH_SHORT).show()
        }
    }

    //Convierte el código ingresado en credenciales, llama a signInWithPhoneAuthCredential.
    private fun verifyCode(code: String) {
        val credential = PhoneAuthProvider.getCredential(storedVerificationId, code)
        signInWithPhoneAuthCredential(credential)
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Sesión iniciada correctamente", Toast.LENGTH_SHORT).show()
                    // Aquí puedes navegar a tu pantalla principal
                } else {
                    Toast.makeText(this, "Código incorrecto", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun registrarUsuario(usuario: Usuario) {
        val db = FirebaseDatabase.getInstance().reference
        val userId = usuario.telefono.replace("+", "")
        db.child("usuarios").child(userId).setValue(usuario)
            .addOnSuccessListener {
                Toast.makeText(this, "Usuario registrado correctamente", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al registrar: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}



//Pantalla de inicio
@Composable
fun LoginScreen(
    onSendCode: (String) -> Unit,
    onVerifyCode: (String) -> Unit,
    onCambiarAPantallaRegistro: () -> Unit
) {
    //Recibe dos funciones: para enviar y verificar código.
    var phoneNumber by remember { mutableStateOf("") }
    var verificationCode by remember { mutableStateOf("") }

    //Interfaz grafica
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        //Campo para ingresar el numero
        OutlinedTextField(
            value = phoneNumber,
            onValueChange = { phoneNumber = it },
            label = { Text("Número de teléfono") },
            placeholder = { Text("+59171234567") },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        //Boton
        Button(
            onClick = { onSendCode(phoneNumber) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Enviar código")
        }

        Spacer(modifier = Modifier.height(24.dp))

        //Campo para escribir el codigo
        OutlinedTextField(
            value = verificationCode,
            onValueChange = { verificationCode = it },
            label = { Text("Código de verificación") },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { onVerifyCode(verificationCode) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Verificar código")
        }
        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = { onCambiarAPantallaRegistro() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("¿No tienes cuenta? Regístrate")
        }
    }
}


