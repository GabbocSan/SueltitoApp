package com.example.suletitoapp

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.util.Log
import java.io.IOException
import java.nio.charset.Charset

object NFCUtils {
    private const val TAG = "NFCUtils"

    /**
     * Escribe datos del conductor en una etiqueta NFC - VERSIÓN MEJORADA
     */
    fun writeNFCTag(tag: Tag, conductorId: String, conductorNombre: String): Boolean {
        Log.d(TAG, "Intentando escribir en etiqueta NFC...")

        return try {
            val message = "$conductorId|$conductorNombre"
            Log.d(TAG, "Mensaje a escribir: $message")

            val ndefMessage = createNdefMessage(message)
            var success = false

            // Intentar primero con Ndef (etiqueta ya formateada)
            val ndef = Ndef.get(tag)
            if (ndef != null) {
                Log.d(TAG, "Usando tecnología Ndef")
                try {
                    ndef.connect()

                    // Verificar si la etiqueta es escribible
                    if (!ndef.isWritable) {
                        Log.e(TAG, "La etiqueta NFC no es escribible")
                        return false
                    }

                    // Verificar si hay suficiente espacio
                    val size = ndefMessage.toByteArray().size
                    if (size > ndef.maxSize) {
                        Log.e(TAG, "Mensaje demasiado grande para la etiqueta")
                        return false
                    }

                    ndef.writeNdefMessage(ndefMessage)
                    success = true
                    Log.d(TAG, "Etiqueta NFC escrita exitosamente con Ndef")

                } catch (e: Exception) {
                    Log.e(TAG, "Error al escribir con Ndef: ${e.message}")
                } finally {
                    try {
                        ndef.close()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error al cerrar Ndef: ${e.message}")
                    }
                }
            }

            // Si Ndef falló, intentar con NdefFormatable
            if (!success) {
                Log.d(TAG, "Intentando con NdefFormatable")
                val ndefFormatable = NdefFormatable.get(tag)
                if (ndefFormatable != null) {
                    try {
                        ndefFormatable.connect()
                        ndefFormatable.format(ndefMessage)
                        success = true
                        Log.d(TAG, "Etiqueta NFC formateada y escrita exitosamente")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error al formatear y escribir: ${e.message}")
                    } finally {
                        try {
                            ndefFormatable.close()
                        } catch (e: Exception) {
                            Log.e(TAG, "Error al cerrar NdefFormatable: ${e.message}")
                        }
                    }
                } else {
                    Log.e(TAG, "Etiqueta NFC no soporta Ndef ni NdefFormatable")
                }
            }

            success

        } catch (e: IOException) {
            Log.e(TAG, "Error de E/S al escribir etiqueta NFC: ${e.message}")
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error inesperado al escribir etiqueta NFC: ${e.message}")
            false
        }
    }

    /**
     * Lee datos de una etiqueta NFC - VERSIÓN MEJORADA
     */
    fun readNFCTag(tag: Tag): Pair<String, String>? {
        Log.d(TAG, "Intentando leer etiqueta NFC...")

        return try {
            val ndef = Ndef.get(tag)
            if (ndef != null) {
                Log.d(TAG, "Usando tecnología Ndef para lectura")

                try {
                    ndef.connect()
                    val ndefMessage = ndef.ndefMessage

                    if (ndefMessage != null && ndefMessage.records.isNotEmpty()) {
                        val record = ndefMessage.records[0]

                        // Verificar que sea un registro de texto
                        if (record.tnf == NdefRecord.TNF_WELL_KNOWN &&
                            record.type.contentEquals(NdefRecord.RTD_TEXT)) {

                            val payload = record.payload

                            // El primer byte indica la longitud del código de idioma
                            val langCodeLength = payload[0].toInt() and 0x3F

                            // Extraer el texto (saltar el byte de longitud y el código de idioma)
                            val message = String(
                                payload,
                                langCodeLength + 1,
                                payload.size - langCodeLength - 1,
                                Charset.forName("UTF-8")
                            )

                            Log.d(TAG, "Mensaje leído: $message")

                            val parts = message.split("|")
                            if (parts.size == 2) {
                                Log.d(TAG, "Etiqueta NFC leída exitosamente: ${parts[0]}, ${parts[1]}")
                                return Pair(parts[0], parts[1]) // conductorId, conductorNombre
                            } else {
                                Log.e(TAG, "Formato de mensaje inválido: $message")
                            }
                        } else {
                            Log.e(TAG, "La etiqueta no contiene un registro de texto válido")
                        }
                    } else {
                        Log.e(TAG, "La etiqueta no contiene mensajes NDEF")
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "Error al leer con Ndef: ${e.message}")
                } finally {
                    try {
                        ndef.close()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error al cerrar Ndef: ${e.message}")
                    }
                }
            } else {
                Log.e(TAG, "La etiqueta no soporta tecnología Ndef")
            }

            null

        } catch (e: IOException) {
            Log.e(TAG, "Error de E/S al leer etiqueta NFC: ${e.message}")
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error inesperado al leer etiqueta NFC: ${e.message}")
            null
        }
    }

    /**
     * Crea un mensaje NDEF con texto - VERSIÓN MEJORADA
     */
    private fun createNdefMessage(text: String): NdefMessage {
        Log.d(TAG, "Creando mensaje NDEF para: $text")

        val lang = "es"
        val textBytes = text.toByteArray(Charset.forName("UTF-8"))
        val langBytes = lang.toByteArray(Charset.forName("US-ASCII"))
        val langLength = langBytes.size
        val textLength = textBytes.size

        // Crear payload: [longitud_idioma][código_idioma][texto]
        val payload = ByteArray(1 + langLength + textLength)

        // Primer byte: longitud del código de idioma
        payload[0] = langLength.toByte()

        // Copiar código de idioma
        System.arraycopy(langBytes, 0, payload, 1, langLength)

        // Copiar texto
        System.arraycopy(textBytes, 0, payload, 1 + langLength, textLength)

        val record = NdefRecord(
            NdefRecord.TNF_WELL_KNOWN,
            NdefRecord.RTD_TEXT,
            ByteArray(0),
            payload
        )

        Log.d(TAG, "Mensaje NDEF creado exitosamente, tamaño: ${payload.size} bytes")
        return NdefMessage(arrayOf(record))
    }

    /**
     * Método auxiliar para obtener información de la etiqueta
     */
    fun getTagInfo(tag: Tag): String {
        val sb = StringBuilder()
        sb.append("ID: ${tag.id.joinToString("") { "%02x".format(it) }}\n")
        sb.append("Tecnologías: ${tag.techList.joinToString(", ")}\n")

        val ndef = Ndef.get(tag)
        if (ndef != null) {
            sb.append("Tipo: ${ndef.type}\n")
            sb.append("Tamaño máximo: ${ndef.maxSize} bytes\n")
            sb.append("Escribible: ${ndef.isWritable}\n")
        }

        return sb.toString()
    }
}