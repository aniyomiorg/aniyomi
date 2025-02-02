package eu.kanade.tachiyomi.util

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.net.Uri
import fi.iki.elonen.NanoHTTPD
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import java.net.URLConnection

class LocalHttpServer(
    port: Int,
    private val contentResolver: ContentResolver,
) : NanoHTTPD(port) {

    @SuppressLint("Recycle")
    override fun serve(session: IHTTPSession): Response {
        val params = session.parameters
        val uriParam = params["uri"]?.get(0) ?: return newFixedLengthResponse(
            Response.Status.BAD_REQUEST,
            "text/plain",
            "Missing uri parameter",
        )

        val uri = try {
            Uri.parse(uriParam)
        } catch (e: Exception) {
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "Invalid URI")
        }

        val mimeType = URLConnection.guessContentTypeFromName(uri.toString()) ?: "application/octet-stream"

        // Abrir el archivo como un InputStream y obtener su tamaño
        val assetFileDescriptor = try {
            contentResolver.openAssetFileDescriptor(uri, "r")
        } catch (e: Exception) {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "File not found")
        }

        val fileLength = assetFileDescriptor?.length ?: -1L

        // Verificar si se incluye el header "Range"
        val rangeHeader = session.headers["range"]
        if (rangeHeader != null && fileLength > 0) {
            try {
                // Se espera el formato "bytes=start-end"
                val range = rangeHeader.replace("bytes=", "").split("-")
                val start = range.getOrNull(0)?.toLongOrNull() ?: 0L
                // Si no se especifica el final, usamos el tamaño del archivo - 1
                val end = range.getOrNull(1)?.toLongOrNull() ?: (fileLength - 1)
                val length = end - start + 1

                val inputStream = contentResolver.openInputStream(uri)
                inputStream?.skip(start)

                // Responder con Partial Content
                val response = newFixedLengthResponse(Response.Status.PARTIAL_CONTENT, mimeType, inputStream, length)
                response.addHeader("Content-Range", "bytes $start-$end/$fileLength")
                response.addHeader("Accept-Ranges", "bytes")
                return response
            } catch (e: Exception) {
                // En caso de error, se envía el archivo completo
                logcat(LogPriority.ERROR, e) { "Error processing Range header" }
            }
        }

        // Sin Range header, enviar el archivo completo
        val inputStream = contentResolver.openInputStream(uri)
        return if (inputStream != null) {
            val response = newChunkedResponse(Response.Status.OK, mimeType, inputStream)
            response.addHeader("Accept-Ranges", "bytes")
            response
        } else {
            newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "File not found")
        }
    }
}

object LocalHttpServerHolder {
    const val PORT = 8181
}
