package com.example.vaultcast.controller

import com.example.vaultcast.config.FileStorageProperties
import java.nio.file.Files
import java.nio.file.Paths
import org.springframework.core.io.FileSystemResource
import org.springframework.http.*
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody

@RestController
class StreamingController(private val storageProps: FileStorageProperties) {

    private val uploadDir = Paths.get(storageProps.uploadDir).toAbsolutePath().toString()

    @GetMapping("/v1/stream/{fileName}")
    fun streamVideo(
            @PathVariable fileName: String,
            @RequestHeader headers: HttpHeaders,
    ): ResponseEntity<StreamingResponseBody> {
        val path = Paths.get(uploadDir).resolve(fileName).toAbsolutePath()
        if (!Files.exists(path)) return ResponseEntity.notFound().build()

        val resource = FileSystemResource(path.toFile())
        // a = b ?: c는 a가 null일 경우 b를 사용하고, 그렇지 않으면 c를 사용
        val contentType = Files.probeContentType(path) ?: MediaType.APPLICATION_OCTET_STREAM_VALUE
        val contentLength = resource.contentLength()

        // Range 헤더가 없으면 전체 스트림
        val range = headers.getFirst(HttpHeaders.RANGE)

        val (start, end) =
                if (range.isNullOrBlank()) {
                    0L to contentLength - 1
                } else {
                    val parts = range.removePrefix("bytes=").split("-")
                    if (parts.size != 2) {
                        return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                                .header(HttpHeaders.CONTENT_TYPE, contentType)
                                .header(HttpHeaders.CONTENT_LENGTH, "0")
                                .build()
                    }
                    parts[0].toLong() to (parts.getOrNull(1)?.toLongOrNull() ?: contentLength - 1)
                }

        val chunkLength = end - start + 1

        // 유효성 검사
        if (start < 0 || end >= contentLength || start > end) {
            return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                    .header(HttpHeaders.CONTENT_TYPE, contentType)
                    .header(HttpHeaders.CONTENT_LENGTH, "0")
                    .build()
        }

        val status = if (range.isNullOrBlank()) HttpStatus.OK else HttpStatus.PARTIAL_CONTENT
        val responseHeaders =
                HttpHeaders().apply {
                    set(HttpHeaders.CONTENT_TYPE, contentType)
                    set(HttpHeaders.CONTENT_LENGTH, chunkLength.toString())
                    set(HttpHeaders.ACCEPT_RANGES, "bytes")
                    set(HttpHeaders.CONTENT_RANGE, "bytes $start-$end/$contentLength")
                }
        val body = StreamingResponseBody { output ->
            resource.inputStream.use { input ->
                input.skip(start)
                // limit 만큼만 복사
                var remaining = chunkLength
                val buffer = ByteArray(8 * 1024)
                while (remaining > 0) {
                    val read = input.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
                    if (read <= 0) break
                    output.write(buffer, 0, read)
                    remaining -= read
                }
            }
        }

        return ResponseEntity.status(status).headers(responseHeaders).body(body)
    }

    private fun java.io.InputStream.copyTo(
            out: java.io.OutputStream,
            bufferSize: Int,
            limit: Long
    ) {
        var remaining = limit
        val buffer = ByteArray(bufferSize)
        while (remaining > 0) {
            val read = read(buffer, 0, minOf(bufferSize.toLong(), remaining).toInt())
            if (read == -1) break
            out.write(buffer, 0, read)
            remaining -= read
        }
    }
}
