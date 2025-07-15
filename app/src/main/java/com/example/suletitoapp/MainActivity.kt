package com.example.suletitoapp

//Main Activity
import android.os.Bundle
import android.util.Log
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.Alignment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.FirebaseException
import java.util.concurrent.TimeUnit
import com.google.firebase.database.FirebaseDatabase
import com.example.suletitoapp.model.Usuario
import androidx.compose.runtime.LaunchedEffect



class MainActivity : ComponentActivity() {

    //Variables de Firebase
    private lateinit var auth: FirebaseAuth
    private var storedVerificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null

    private val isLoading = mutableStateOf(false)
    private val codigoEnviado = mutableStateOf(false)
    private val mensajeError = mutableStateOf("")

    private var currentOnSuccess: ((Usuario) -> Unit)? = null
    private var currentOnNeedRegistration: (() -> Unit)? = null


    //Inicio del Programa
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        enableEdgeToEdge()

        setContent {
            SuletitoAppTheme {
                val pantallaActual = remember { mutableStateOf("login") }
                val usuarioActual = remember { mutableStateOf<Usuario?>(null) }

                when (pantallaActual.value) {

                    "login" -> LoginScreen(
                        onSendCode = { phone ->
                            // Establecer callbacks antes de enviar código
                            currentOnSuccess = { usuario ->
                                usuarioActual.value = usuario
                                pantallaActual.value = "principal"
                            }
                            currentOnNeedRegistration = {
                                pantallaActual.value = "registro"
                            }
                            sendVerificationCode(phone)
                        },
                        onVerifyCode = { code ->
                            verifyCode(code)
                        },
                        onCambiarAPantallaRegistro = { pantallaActual.value = "registro" },
                        isLoading = isLoading.value,
                        codigoEnviado = codigoEnviado.value,
                        mensajeError = mensajeError.value
                    )
                    "registro" -> RegistroScreen(
                        onRegistrar = { usuario ->
                            registrarUsuario(usuario)
                            usuarioActual.value = usuario
                            pantallaActual.value = "login"
                        },
                        onVolverALogin = {
                            // Cerrar sesión si existe
                            auth.signOut()
                            pantallaActual.value = "login"
                        }
                    )
                    "principal" -> {
                        usuarioActual.value?.let { usuario ->
                            when (usuario.rol) {
                                "Chofer" -> ConductorScreen(
                                    usuario.nombres,
                                    usuario.saldo,
                                    onCerrarSesion = {
                                        cerrarSesion()
                                        usuarioActual.value = null
                                        pantallaActual.value = "login"
                                    }
                                )
                                "Pasajero" -> PasajeroScreen(
                                    usuario.nombres,
                                    usuario.saldo,
                                    onCerrarSesion = {
                                        cerrarSesion()
                                        usuarioActual.value = null
                                        pantallaActual.value = "login"
                                    },
                                    onRecargarSaldo = {
                                        pantallaActual.value = "recarga"
                                    }
                                )
                                else -> Text("Rol no reconocido")
                            }
                        } ?: Text("Cargando datos...")
                    }
                    "recarga" -> {
                        usuarioActual.value?.let { usuario ->
                            RecargaSaldoScreen(
                                saldoActual = usuario.saldo,
                                onRecargar = { monto ->
                                    recargarSaldo(monto)
                                    // Actualizar el usuario actual con el nuevo saldo
                                    usuarioActual.value = usuario.copy(saldo = usuario.saldo + monto)
                                    // Volver a la pantalla principal
                                    pantallaActual.value = "principal"
                                },
                                onCancelar = {
                                    // Volver a la pantalla principal sin cambios
                                    pantallaActual.value = "principal"
                                }
                            )
                        } ?: Text("Error: Usuario no encontrado")
                    }
                }
            }

        }
    }

    private fun sendVerificationCode(phoneNumber: String) {
        // Validar formato del teléfono
        if (phoneNumber.isBlank()) {
            mensajeError.value = "Ingresa un número de teléfono"
            return
        }

        isLoading.value = true
        mensajeError.value = ""
        codigoEnviado.value = false

        // Formatear número si es necesario
        val formattedPhone = if (phoneNumber.startsWith("+591")) {
            phoneNumber
        } else if (phoneNumber.startsWith("591")) {
            "+$phoneNumber"
        } else if (phoneNumber.length == 8) {
            "+591$phoneNumber"
        } else {
            phoneNumber
        }

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(callbacks)
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    //Callback de Firebase
    private val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        //Clase para manejar las respeustas de firebase
        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            Log.d("AUTH", "Verificación automática completada")
            currentOnSuccess?.let { onSuccess ->
                currentOnNeedRegistration?.let { onNeedRegistration ->
                    signInWithPhoneAuthCredential(credential, onSuccess, onNeedRegistration)
                }
            }
        }

        //Si Firebase detecta el código automáticamente (SMS recibido), inicia sesión.
        override fun onVerificationFailed(e: FirebaseException) {
            Log.e("AUTH", "Verificación falló: ${e.message}")
            isLoading.value = false
            codigoEnviado.value = false
            mensajeError.value = "Error al enviar código: ${e.message}"
            Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }

        //Si algo falla (número inválido, red, etc.), muestra un mensaje.
        override fun onCodeSent(
            verificationId: String,
            token: PhoneAuthProvider.ForceResendingToken
        ) {
            Log.d("AUTH", "Código enviado exitosamente")
            storedVerificationId = verificationId
            resendToken = token
            isLoading.value = false
            codigoEnviado.value = true
            mensajeError.value = ""
            Toast.makeText(this@MainActivity, "Código enviado", Toast.LENGTH_SHORT).show()
        }
    }

    //Convierte el código ingresado en credenciales, llama a signInWithPhoneAuthCredential.
    private fun verifyCode(code: String) {
        if (code.isBlank()) {
            mensajeError.value = "Ingresa el código de verificación"
            return
        }
        if (storedVerificationId == null) {
            mensajeError.value = "Error: Primero debes enviar el código"
            return
        }

        isLoading.value = true
        mensajeError.value = ""

        try {
            val credential = PhoneAuthProvider.getCredential(storedVerificationId!!, code)

            // Usar los callbacks guardados
            currentOnSuccess?.let { onSuccess ->
                currentOnNeedRegistration?.let { onNeedRegistration ->
                    signInWithPhoneAuthCredential(credential, onSuccess, onNeedRegistration)
                }
            } ?: run {
                Log.e("AUTH", "Error: callbacks no definidos")
                isLoading.value = false
                mensajeError.value = "Error interno de la aplicación"
            }
        } catch (e: Exception) {
            Log.e("AUTH", "Error al crear credencial: ${e.message}")
            isLoading.value = false
            mensajeError.value = "Error al verificar código"
            Toast.makeText(this, "Error al verificar código", Toast.LENGTH_SHORT).show()
        }
    }

    private fun signInWithPhoneAuthCredential(
        credential: PhoneAuthCredential,
        onSuccess: (Usuario) -> Unit,
        onNeedRegistration: () -> Unit
    ) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                isLoading.value = false

                if (task.isSuccessful) {
                    Log.d("AUTH", "Autenticación exitosa")
                    val userId = auth.currentUser?.uid

                    if (userId != null) {
                        obtenerDatosUsuario(
                            userId = userId,
                            onSuccess = onSuccess,
                            onUserNotFound = onNeedRegistration
                        )
                    } else {
                        mensajeError.value = "Error: No se pudo obtener el ID del usuario"
                        Toast.makeText(this, "Error de autenticación", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.e("AUTH", "Error en autenticación: ${task.exception?.message}")
                    mensajeError.value = "Código incorrecto"
                    Toast.makeText(this, "Código incorrecto", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun obtenerDatosUsuario(userId: String, onSuccess: (Usuario) -> Unit, onUserNotFound: () -> Unit) {
        val db = FirebaseDatabase.getInstance().reference

        db.child("usuarios").child(userId).get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    try {
                        val nombres = snapshot.child("nombres").value?.toString() ?: ""
                        val apellidos = snapshot.child("apellidos").value?.toString() ?: ""
                        val rol = snapshot.child("rol").value?.toString() ?: ""
                        val telefono = snapshot.child("telefono").value?.toString() ?: ""
                        val saldo = snapshot.child("saldo").getValue(Double::class.java) ?: 0.0

                        val usuario = Usuario(nombres, apellidos, rol, telefono, saldo)
                        Log.d("AUTH", "Usuario encontrado: $rol")
                        onSuccess(usuario)
                    } catch (e: Exception) {
                        Log.e("AUTH", "Error al parsear datos: ${e.message}")
                        Toast.makeText(this, "Error al cargar datos del usuario", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.w("AUTH", "Usuario no encontrado en la base de datos")
                    Toast.makeText(this, "Complete su registro para continuar", Toast.LENGTH_SHORT).show()
                    onUserNotFound()
                }
            }
            .addOnFailureListener { exception ->
                Log.e("AUTH", "Error al obtener datos: ${exception.message}")
                Toast.makeText(this, "Error al obtener datos: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun registrarUsuario(usuario: Usuario) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val phoneNumber = FirebaseAuth.getInstance().currentUser?.phoneNumber

        if (userId != null) {
            // Usar el teléfono del usuario autenticado si no se proporciona
            val usuarioCompleto = if (usuario.telefono.isBlank() && phoneNumber != null) {
                usuario.copy(telefono = phoneNumber, saldo = 0.0)
            } else {
                usuario.copy(saldo = 0.0)
            }

            val db = FirebaseDatabase.getInstance().reference
            db.child("usuarios").child(userId).setValue(usuarioCompleto)
                .addOnSuccessListener {
                    Toast.makeText(this, "Usuario registrado correctamente", Toast.LENGTH_SHORT).show()
                    Log.d("AUTH", "Usuario registrado: ${usuarioCompleto.nombres} - ${usuarioCompleto.rol}")
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error al registrar: ${it.message}", Toast.LENGTH_SHORT).show()
                    Log.e("AUTH", "Error al registrar usuario: ${it.message}")
                }
        } else {
            Toast.makeText(this, "Error: usuario no autenticado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun cerrarSesion() {
        auth.signOut()

        storedVerificationId = null
        resendToken = null
        codigoEnviado.value = false
        mensajeError.value = ""
        isLoading.value = false

        currentOnSuccess = null
        currentOnNeedRegistration = null

        Toast.makeText(this, "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show()
        Log.d("AUTH", "Sesión cerrada por el usuario")
    }

    private fun recargarSaldo(monto: Double) {
        val userId = auth.currentUser?.uid

        if (userId != null) {
            val db = FirebaseDatabase.getInstance().reference

            // Obtener saldo actual y sumarlo
            db.child("usuarios").child(userId).child("saldo").get()
                .addOnSuccessListener { snapshot ->
                    val saldoActual = snapshot.getValue(Double::class.java) ?: 0.0
                    val nuevoSaldo = saldoActual + monto

                    // Actualizar saldo en la base de datos
                    db.child("usuarios").child(userId).child("saldo").setValue(nuevoSaldo)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Recarga exitosa: +Bs. $monto", Toast.LENGTH_SHORT).show()
                            Log.d("RECARGA", "Saldo recargado: $monto. Nuevo saldo: $nuevoSaldo")
                        }
                        .addOnFailureListener { exception ->
                            Toast.makeText(this, "Error al recargar: ${exception.message}", Toast.LENGTH_SHORT).show()
                            Log.e("RECARGA", "Error al recargar saldo: ${exception.message}")
                        }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(this, "Error al obtener saldo actual: ${exception.message}", Toast.LENGTH_SHORT).show()
                    Log.e("RECARGA", "Error al obtener saldo: ${exception.message}")
                }
        } else {
            Toast.makeText(this, "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show()
        }
    }
}



//Pantalla de inicio
@Composable
fun LoginScreen(
    onSendCode: (String) -> Unit,
    onVerifyCode: (String) -> Unit,
    onCambiarAPantallaRegistro: () -> Unit,
    isLoading: Boolean = false,
    codigoEnviado: Boolean = false,
    mensajeError: String = ""
) {
    //Recibe dos funciones: para enviar y verificar código.
    var phoneNumber by remember { mutableStateOf("") }
    var verificationCode by remember { mutableStateOf("") }

    //Interfaz grafica
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        //Campo para ingresar el numero
        OutlinedTextField(
            value = phoneNumber,
            onValueChange = { phoneNumber = it },
            label = { Text("Número de teléfono") },
            placeholder = { Text("+59171234567") },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.height(16.dp))

        //Boton
        Button(
            onClick = { onSendCode(phoneNumber) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading && phoneNumber.isNotBlank()
        ) {
            if (isLoading && !codigoEnviado) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text("Enviar código")
        }

        Spacer(modifier = Modifier.height(24.dp))

        //Campo para escribir el codigo
        if (codigoEnviado) {
            OutlinedTextField(
                value = verificationCode,
                onValueChange = { verificationCode = it },
                label = { Text("Código de verificación") },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onVerifyCode(verificationCode) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && verificationCode.isNotBlank()
            ) {
                if (isLoading && codigoEnviado) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Verificar código")
            }
        }

        // Mostrar mensaje de error si existe
        if (mensajeError.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = mensajeError,
                color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = { onCambiarAPantallaRegistro() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            Text("¿No tienes cuenta? Regístrate")
        }


    }
}


