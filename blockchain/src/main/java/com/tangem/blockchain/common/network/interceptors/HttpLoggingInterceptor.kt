package com.tangem.blockchain.common.network.interceptors

import com.tangem.blockchain.common.logging.Logger
import com.tangem.blockchain.common.logging.SensitiveKeys
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.internal.http.promisesBody
import okio.Buffer
import okio.GzipSource
import org.json.JSONArray
import org.json.JSONObject
import java.io.EOFException
import java.io.IOException
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

private const val JSON_INDENT_SPACES = 4

/**
 * Interceptor for logging network requests and responses
 *
[REDACTED_AUTHOR]
 */
internal object HttpLoggingInterceptor : Interceptor {

    private const val WRITE_LOG_THRESHOLD_BYTES_SIZE = 2_048_000

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        logRequestMessage(chain, request)

        val startNs = System.nanoTime()
        val response: Response
        try {
            response = chain.proceed(request)
        } catch (e: Exception) {
            Logger.logNetwork("<-- HTTP FAILED: $e")
            throw e
        }

        logResponseMessage(response, startNs)

        return response
    }

    private fun logRequestMessage(chain: Interceptor.Chain, request: Request) {
        val connection = chain.connection()
        val connectionProtocol = if (connection != null) " ${connection.protocol()}" else ""

        Logger.logNetwork(
            "--> ${request.method} ${request.getUrlWithoutSensitiveInfo()}$connectionProtocol\n" +
                createRequestEndMessage(request),
        )
    }

    private fun createRequestEndMessage(request: Request): String {
        val requestBody = request.body
        val method = request.method

        return if (requestBody == null) {
            "--> END $method"
        } else if (bodyHasUnknownEncoding(request.headers)) {
            "--> END $method (encoded body omitted)"
        } else if (requestBody.isDuplex()) {
            "--> END $method (duplex request body omitted)"
        } else if (requestBody.isOneShot()) {
            "--> END $method (one-shot body omitted)"
        } else {
            val buffer = Buffer()
            requestBody.writeTo(buffer)

            val contentType = requestBody.contentType()
            val charset: Charset = contentType?.charset(StandardCharsets.UTF_8) ?: StandardCharsets.UTF_8

            if (buffer.isProbablyUtf8()) {
                val json = buffer.readString(charset).beautifyJson()
                "$json\n--> END $method (${requestBody.contentLength()}-byte body)"
            } else {
                "--> END $method (binary ${requestBody.contentLength()}-byte body omitted)"
            }
        }
    }

    private fun logResponseMessage(response: Response, startNs: Long) {
        val tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs)

        val responseMessage = if (response.message.isEmpty()) "" else ' ' + response.message
        val startMessage = "<-- ${response.code}$responseMessage ${response.request.getUrlWithoutSensitiveInfo()} " +
            "(${tookMs}ms)"

        val responseHeaders = response.headers
        val responseBody = response.body!!
        val contentLength = responseBody.contentLength()

        val message = if (!response.promisesBody()) {
            "<-- END HTTP"
        } else if (bodyHasUnknownEncoding(response.headers)) {
            "<-- END HTTP (encoded body omitted)"
        } else if (contentLength > WRITE_LOG_THRESHOLD_BYTES_SIZE) {
            "Response size to large: $contentLength bytes \n<-- END HTTP"
        } else {
            val source = responseBody.source()
            source.request(Long.MAX_VALUE)
            var buffer = source.buffer
            if (buffer.size > WRITE_LOG_THRESHOLD_BYTES_SIZE) {
                "Response size to large: $contentLength bytes \n<-- END HTTP"
            } else {
                var gzippedLength: Long? = null
                if ("gzip".equals(responseHeaders["Content-Encoding"], ignoreCase = true)) {
                    gzippedLength = buffer.size
                    GzipSource(buffer.clone()).use { gzippedResponseBody ->
                        buffer = Buffer()
                        buffer.writeAll(gzippedResponseBody)
                    }
                }

                val contentType = responseBody.contentType()
                val charset: Charset = contentType?.charset(StandardCharsets.UTF_8) ?: StandardCharsets.UTF_8

                if (!buffer.isProbablyUtf8()) {
                    "<-- END HTTP (binary ${buffer.size}-byte body omitted)"
                } else {
                    val json = if (contentLength != 0L) {
                        buffer.clone().readString(charset).beautifyJson()
                    } else {
                        ""
                    }

                    val end = if (gzippedLength != null) {
                        "<-- END HTTP (${buffer.size}-byte, $gzippedLength-gzipped-byte body)"
                    } else {
                        "<-- END HTTP (${buffer.size}-byte body)"
                    }

                    "$json\n$end"
                }
            }
        }

        Logger.logNetwork(startMessage + "\n" + message)
    }

    private fun bodyHasUnknownEncoding(headers: Headers): Boolean {
        val contentEncoding = headers["Content-Encoding"] ?: return false
        return !contentEncoding.equals("identity", ignoreCase = true) &&
            !contentEncoding.equals("gzip", ignoreCase = true)
    }

    private fun Buffer.isProbablyUtf8(): Boolean {
        try {
            val prefix = Buffer()
            val byteCount = size.coerceAtMost(maximumValue = 64)
            copyTo(out = prefix, offset = 0, byteCount = byteCount)

            @Suppress("MagicNumber", "UnusedPrivateMember")
            for (i in 0 until 16) {
                if (prefix.exhausted()) break

                val codePoint = prefix.readUtf8CodePoint()
                if (Character.isISOControl(codePoint) && !Character.isWhitespace(codePoint)) return false
            }

            return true
        } catch (_: EOFException) {
            return false
        }
    }

    private fun Request.getUrlWithoutSensitiveInfo(): String {
        val key = SensitiveKeys.data.firstOrNull { url.toString().contains(other = it, ignoreCase = true) }

        return if (key == null) {
            url.toString()
        } else {
            url.toString().replace(oldValue = key, newValue = "******")
        }
    }

    private fun String.beautifyJson(): String {
        beautifyIfObject(json = this)?.let {
            return it
        }

        beautifyIfArray(json = this)?.let {
            return it
        }

        return this
    }

    private fun beautifyIfObject(json: String): String? {
        return try {
            JSONObject(json).toString(JSON_INDENT_SPACES)
        } catch (e: Exception) {
            null
        }
    }

    private fun beautifyIfArray(json: String): String? {
        return try {
            JSONArray(json).toString(JSON_INDENT_SPACES)
        } catch (_: Exception) {
            null
        }
    }
}