package com.example.suletitoapp

//Main Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
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
import java.util.Locale


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

    // Variables NFC
    private var nfcAdapter: NfcAdapter? = null
    private var pendingIntent: PendingIntent? = null
    private var intentFiltersArray: Array<IntentFilter>? = null
    private var techListsArray: Array<Array<String>>? = null

    //Estados NFC
    private val nfcWriteMode = mutableStateOf(false)
    private val nfcReadMode = mutableStateOf(false)
    private val nfcMessage = mutableStateOf("")
    private val isProcessingPayment = mutableStateOf(false)
    private val currentPaymentAmount = mutableStateOf(0.0)
    private val currentConductorData = mutableStateOf<Pair<String, String>?>(null)

    private val saldoActualizado = mutableStateOf(0.0)
    private val debeActualizarSaldo = mutableStateOf(false)

    //Inicio del Programa
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        enableEdgeToEdge()

        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_NFC)) {
            Toast.makeText(this, "Este dispositivo no soporta NFC", Toast.LENGTH_LONG).show()
        }
        setupNFC()

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
                                    },
                                    onConfigurarNFC = {
                                        pantallaActual.value = "nfc_conductor"
                                    },
                                    onVerHistorial = {
                                        pantallaActual.value = "historial_conductor"
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
                                    },
                                    onPagarNFC = {
                                        pantallaActual.value = "nfc_pago"
                                    },
                                    onVerHistorial = {
                                        pantallaActual.value = "historial_pasajero"
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
                    "nfc_conductor" -> {
                        usuarioActual.value?.let { usuario ->
                            NFCConductorScreen(
                                conductorNombre = "${usuario.nombres} ${usuario.apellidos}",
                                onEscribirNFC = {
                                    iniciarEscrituraNFC(usuario)
                                },
                                onCancelar = {
                                    nfcWriteMode.value = false
                                    nfcMessage.value = ""
                                    pantallaActual.value = "principal"
                                },
                                isWriting = nfcWriteMode.value,
                                mensaje = nfcMessage.value
                            )
                        }
                    }
                    "nfc_pago" -> {
                        usuarioActual.value?.let { usuario ->

                            if (debeActualizarSaldo.value) {
                                usuarioActual.value = usuario.copy(saldo = saldoActualizado.value)
                                debeActualizarSaldo.value = false
                            }

                            NFCPagoScreen(
                                pasajeroNombre = "${usuario.nombres} ${usuario.apellidos}",
                                saldoActual = usuario.saldo,
                                onPagar = { monto ->
                                    iniciarPagoNFC(monto, usuario)
                                },
                                onCancelar = {
                                    nfcReadMode.value = false
                                    isProcessingPayment.value = false
                                    nfcMessage.value = ""
                                    pantallaActual.value = "principal"
                                },
                                isProcessing = isProcessingPayment.value,
                                mensaje = nfcMessage.value,

                            )
                        }
                    }
                    "historial_pasajero" -> {
                        val userId = auth.currentUser?.uid
                        if (userId != null) {
                            HistorialPasajeroScreen(
                                pasajeroId = userId,
                                onVolver = {
                                    pantallaActual.value = "principal"
                                }
                            )
                        } else {
                            Text("Error: Usuario no autenticado")
                        }
                    }
                    "historial_conductor" -> {
                        val userId = auth.currentUser?.uid
                        if (userId != null) {
                            HistorialConductorScreen(
                                conductorId = userId,
                                onVolver = {
                                    pantallaActual.value = "principal"
                                }
                            )
                        } else {
                            Text("Error: Usuario no autenticado")
                        }
                    }

                }
            }

        }
    }

    private fun setupNFC() {
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        if (nfcAdapter == null) {
            Toast.makeText(this, "Este dispositivo no soporta NFC", Toast.LENGTH_LONG).show()
            return
        }

        if (!nfcAdapter!!.isEnabled) {
            Toast.makeText(this, "NFC está deshabilitado. Por favor, habilítalo en Configuración", Toast.LENGTH_LONG).show()
        }

        // Crear PendingIntent para manejar intenciones NFC
        pendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_MUTABLE
        )



        // Configurar filtros de intención
        val ndef = IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED)
        try {
            ndef.addDataType("*/*")
        } catch (e: IntentFilter.MalformedMimeTypeException) {
            throw RuntimeException("Error en el filtro MIME", e)
        }
        val tech = IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED)
        val tag = IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)

        intentFiltersArray = arrayOf(ndef, tech, tag)

        // Configurar tecnologías soportadas
        techListsArray = arrayOf(
            arrayOf(Ndef::class.java.name),
            arrayOf(NdefFormatable::class.java.name)
        )
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, intentFiltersArray, techListsArray)
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNFCIntent(intent)
    }

    private fun handleNFCIntent(intent: Intent) {
        val action = intent.action
        if (NfcAdapter.ACTION_NDEF_DISCOVERED == action ||
            NfcAdapter.ACTION_TECH_DISCOVERED == action ||
            NfcAdapter.ACTION_TAG_DISCOVERED == action) {

            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
            tag?.let {
                if (nfcWriteMode.value) {
                    // Modo escritura (conductor)
                    handleNFCWrite(it)
                } else if (nfcReadMode.value) {
                    // Modo lectura (pasajero)
                    handleNFCRead(it)
                }
            }
        }
    }

    private fun iniciarEscrituraNFC(usuario: Usuario) {
        if (nfcAdapter == null) {
            nfcMessage.value = "NFC no disponible en este dispositivo"
            return
        }

        if (!nfcAdapter!!.isEnabled) {
            nfcMessage.value = "Por favor, habilita NFC en la configuración"
            try {
                startActivity(Intent(android.provider.Settings.ACTION_NFC_SETTINGS))
            } catch (e: Exception) {
                startActivity(Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS))
            }
            return
        }

        nfcWriteMode.value = true
        nfcMessage.value = "Acerca una etiqueta NFC para configurar..."
        Toast.makeText(this, "Acerca una etiqueta NFC", Toast.LENGTH_SHORT).show()
    }

    private fun handleNFCWrite(tag: Tag) {
        val usuario = auth.currentUser
        val userId = usuario?.uid ?: return

        val db = FirebaseDatabase.getInstance().reference
        db.child("usuarios").child(userId).get()
            .addOnSuccessListener { snapshot ->
                val nombres = snapshot.child("nombres").getValue(String::class.java) ?: ""
                val apellidos = snapshot.child("apellidos").getValue(String::class.java) ?: ""
                val nombreCompleto = "$nombres $apellidos"

                runOnUiThread {
                    val success = NFCUtils.writeNFCTag(tag, userId, nombreCompleto)
                    if (success) {
                        nfcMessage.value = "Etiqueta NFC configurada exitosamente"
                        nfcWriteMode.value = false
                        Toast.makeText(this, "¡Etiqueta NFC lista para recibir pagos!", Toast.LENGTH_LONG).show()
                    } else {
                        nfcMessage.value = "Error al escribir en la etiqueta NFC"
                        Toast.makeText(this, "Error al escribir NFC. Intenta nuevamente.", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .addOnFailureListener {
                runOnUiThread {
                    nfcMessage.value = "Error al obtener datos del conductor"
                    Toast.makeText(this, "Error al obtener datos del usuario", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun iniciarPagoNFC(monto: Double, usuario: Usuario) {
        if (nfcAdapter == null) {
            nfcMessage.value = "NFC no disponible en este dispositivo"
            return
        }

        if (!nfcAdapter!!.isEnabled) {
            nfcMessage.value = "Por favor, habilita NFC en la configuración"
            return
        }

        currentPaymentAmount.value = monto
        nfcReadMode.value = true
        isProcessingPayment.value = true
        nfcMessage.value = ""
        Toast.makeText(this, "Acerca tu teléfono a la etiqueta NFC del conductor", Toast.LENGTH_SHORT).show()
    }

    private fun handleNFCRead(tag: Tag) {
        val conductorData = NFCUtils.readNFCTag(tag)

        if (conductorData != null) {
            currentConductorData.value = conductorData
            procesarPago(conductorData.first, conductorData.second)
        } else {
            nfcMessage.value = "Error al leer la etiqueta NFC"
            isProcessingPayment.value = false
            nfcReadMode.value = false
        }
    }

    private fun procesarPago(conductorId: String, conductorNombre: String) {
        val pasajeroId = auth.currentUser?.uid ?: return
        val monto = currentPaymentAmount.value

        val db = FirebaseDatabase.getInstance().reference

        // Obtener datos del pasajero
        db.child("usuarios").child(pasajeroId).get()
            .addOnSuccessListener { pasajeroSnapshot ->
                val pasajeroNombre = "${pasajeroSnapshot.child("nombres").getValue(String::class.java)} ${pasajeroSnapshot.child("apellidos").getValue(String::class.java)}"
                val saldoPasajero = pasajeroSnapshot.child("saldo").getValue(Double::class.java) ?: 0.0

                if (saldoPasajero >= monto) {
                    // Obtener saldo del conductor
                    db.child("usuarios").child(conductorId).child("saldo").get()
                        .addOnSuccessListener { conductorSnapshot ->
                            val saldoConductor = conductorSnapshot.getValue(Double::class.java) ?: 0.0

                            // Realizar la transacción
                            val nuevoSaldoPasajero = saldoPasajero - monto
                            val nuevoSaldoConductor = saldoConductor + monto

                            // Actualizar saldos
                            val updates = mapOf(
                                "usuarios/$pasajeroId/saldo" to nuevoSaldoPasajero,
                                "usuarios/$conductorId/saldo" to nuevoSaldoConductor
                            )

                            db.updateChildren(updates)
                                .addOnSuccessListener {
                                    // Crear registro de pago

                                    crearRegistroPago(pasajeroId, conductorId, pasajeroNombre, conductorNombre, monto)

                                    runOnUiThread{

                                        actualizarSaldoUsuario(nuevoSaldoPasajero)

                                        // Actualizar estado
                                        nfcMessage.value = "¡Pago exitoso! Bs. $monto"
                                        isProcessingPayment.value = false
                                        nfcReadMode.value = false

                                        // Enviar notificación al conductor
                                        enviarNotificacionConductor(conductorId, pasajeroNombre, monto)

                                        Toast.makeText(this, "Pago procesado exitosamente", Toast.LENGTH_LONG).show()

                                    }
                                }
                                .addOnFailureListener {
                                    runOnUiThread{
                                        nfcMessage.value = "Error al procesar el pago"
                                        isProcessingPayment.value = false
                                        nfcReadMode.value = false
                                    }
                                }
                        }
                } else {
                    runOnUiThread{
                        nfcMessage.value = "Saldo insuficiente"
                        isProcessingPayment.value = false
                        nfcReadMode.value = false
                    }
                }
            }
            .addOnFailureListener {
                runOnUiThread{
                    nfcMessage.value = "Error al obtener datos del pasajero"
                    isProcessingPayment.value = false
                    nfcReadMode.value = false
                }
            }
    }

    private fun crearRegistroPago(pasajeroId: String, conductorId: String, pasajeroNombre: String, conductorNombre: String, monto: Double) {
        val db = FirebaseDatabase.getInstance().reference
        val pagoId = db.child("pagos").push().key ?: return

        val calendario = Calendar.getInstance()
        val formatoFecha = SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
        val formatoHora = SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())

        val pago = Pago(
            id = pagoId,
            pasajeroId = pasajeroId,
            conductorId = conductorId,
            pasajeroNombre = pasajeroNombre,
            conductorNombre = conductorNombre,
            monto = monto,
            fecha = formatoFecha.format(calendario.time),
            hora = formatoHora.format(calendario.time),
            timestamp = calendario.timeInMillis
        )

        db.child("pagos").child(pagoId).setValue(pago)
            .addOnSuccessListener {
                Log.d("PAGO", "Registro de pago creado exitosamente")
            }
            .addOnFailureListener {
                Log.e("PAGO", "Error al crear registro de pago: ${it.message}")
            }
    }

    private fun actualizarSaldoUsuario(nuevoSaldo: Double) {
        saldoActualizado.value = nuevoSaldo
        debeActualizarSaldo.value = true
    }

    private fun enviarNotificacionConductor(conductorId: String, pasajeroNombre: String, monto: Double) {
        val db = FirebaseDatabase.getInstance().reference
        val notificacionId = db.child("notificaciones").push().key ?: return

        val calendario = Calendar.getInstance()
        val formatoFecha = SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
        val formatoHora = SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())

        val notificacion = mapOf(
            "id" to notificacionId,
            "conductorId" to conductorId,
            "titulo" to "Pago recibido",
            "mensaje" to "Has recibido un pago de Bs. $monto de $pasajeroNombre",
            "fecha" to formatoFecha.format(calendario.time),
            "hora" to formatoHora.format(calendario.time),
            "timestamp" to calendario.timeInMillis,
            "leida" to false
        )

        db.child("notificaciones").child(notificacionId).setValue(notificacion)
            .addOnSuccessListener {
                Log.d("NOTIFICACION", "Notificación enviada al conductor")
            }
            .addOnFailureListener {
                Log.e("NOTIFICACION", "Error al enviar notificación: ${it.message}")
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


