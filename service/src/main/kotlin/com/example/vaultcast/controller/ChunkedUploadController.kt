package com.example.vaultcast.controller

import com.example.vaultcast.model.VideoMetadata
import com.example.vaultcast.service.VideoMetadataService
import com.example.vaultcast.util.FileSizeValidator
import com.example.vaultcast.util.UploadValidationUtil
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.CompletableFuture
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.scheduling.annotation.Async
import org.springframework.util.StringUtils
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
class ChunkedUploadController(private val videoMetadataService: VideoMetadataService) {

    // 임시 청크 파일 저장 디렉토리 및 최종 파일 저장 디렉토리
    private val tempDir = "uploads/temp"
    private val finalDir = "uploads"

    // 최대 파일 크기: 4GB
    private val maxFileSize: Long = 4L * 1024 * 1024 * 1024

    init {
        Files.createDirectories(Paths.get(tempDir))
        Files.createDirectories(Paths.get(finalDir))
    }

    /**
     * 청크 업로드 엔드포인트
     *
     * @param fileId: 클라이언트가 생성한 업로드 세션 아이디 (재시작 지원)
     * @param chunkIndex: 현재 청크 번호 (0부터 시작)
     * @param totalChunks: 전체 청크 수
     * @param originalFileName: 원본 파일 이름
     * @param file: 전송된 파일 청크
     * @param title, description: 메타데이터 (간단한 위험 내용 검사 수행)
     */
    @PostMapping("/upload/chunk")
    fun uploadChunk(
            @RequestParam("fileId") fileId: String,
            @RequestParam("chunkIndex") chunkIndex: Int,
            @RequestParam("totalChunks") totalChunks: Int,
            @RequestParam("originalFileName") originalFileName: String,
            @RequestParam("file") file: MultipartFile,
            @RequestParam("title", required = false) title: String?,
            @RequestParam("description", required = false) description: String?
    ): ResponseEntity<Map<String, String>> {

        // 1. 청크가 비어있는지 확인
        if (file.isEmpty) {
            return ResponseEntity.badRequest().body(mapOf("error" to "Uploaded chunk is empty"))
        }

        // 2. 청크 크기 체크
        if (FileSizeValidator.isFileSizeValid(file.size).not()) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                    .body(mapOf("error" to "File size exceeds the allowed limit of 4GB"))
        }

        // 3. 원본 파일 이름 정규화 및 확장자 검증
        val cleanedFileName = StringUtils.cleanPath(originalFileName)
        if (UploadValidationUtil.isAllowedExtension(originalFileName).not()) {
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                    .body(mapOf("error" to "Only video files with extensions are allowed"))
        }

        // 4. 메타데이터 (제목/설명) 기본 위험 요소 검사 (간단한 XSS 방지)
        if (title != null && UploadValidationUtil.containsDangerousContent(title)) {
            return ResponseEntity.badRequest()
                    .body(mapOf("error" to "Title contains disallowed or dangerous content"))
        }

        if (description != null && UploadValidationUtil.containsDangerousContent(description)) {
            return ResponseEntity.badRequest()
                    .body(mapOf("error" to "Description contains disallowed or dangerous content"))
        }

        // 5. 청크 저장: 경로 uploads/temp/{fileId}/chunk_{chunkIndex}
        val fileTempDir = Paths.get(tempDir, fileId).toFile()
        if (!fileTempDir.exists()) {
            fileTempDir.mkdirs()
        }
        val chunkFile = File(fileTempDir, "chunk_$chunkIndex")
        try {
            file.inputStream.use { input ->
                FileOutputStream(chunkFile).use { output -> input.copyTo(output) }
            }
        } catch (ex: IOException) {
            ex.printStackTrace()
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(mapOf("error" to "Could not store chunk. Please try again!"))
        }

        // 6. 업로드된 청크 수 확인: 모든 청크가 업로드되었으면 병합 시작
        val uploadedChunks = fileTempDir.listFiles()?.size ?: 0
        if (uploadedChunks == totalChunks) {
            // 비동기적으로 청크 병합 실행
            mergeChunksAsync(fileId, cleanedFileName, totalChunks, title, description)
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(mapOf("message" to "All chunks received. File merging in progress."))
        } else {
            return ResponseEntity.ok(
                    mapOf(
                            "message" to "Chunk $chunkIndex uploaded successfully",
                            "uploadedChunks" to uploadedChunks.toString(),
                    )
            )
        }
    }

    // 청크 병합을 비동기적으로 처리하는 메서드: 모든 청크 파일을 순서대로 읽어서 최종 파일로 병합 후 임시 청크 파일들을 삭제
    @Async
    fun mergeChunksAsync(
            fileId: String,
            originalFileName: String,
            totalChunks: Int,
            title: String?,
            description: String?,
    ): CompletableFuture<Void> {
        try {
            val fileTempDir = Paths.get(tempDir, fileId).toFile()
            val finalFile = Paths.get(finalDir, originalFileName).toFile()
            val fileExtension = originalFileName.substringAfterLast('.', "").lowercase()

            BufferedOutputStream(FileOutputStream(finalFile)).use { mergingStream ->
                for (i in 0 until totalChunks) {
                    val chunkFile = File(fileTempDir, "chunk_$i")
                    if (!chunkFile.exists()) {
                        println("Missing chunk: $i for fileId: $fileId")
                        throw IllegalStateException("Missing chunk index $i")
                    }
                    chunkFile.inputStream().use { chunkStream -> chunkStream.copyTo(mergingStream) }
                }
            }
            // 모든 chunk의 파일 크기 합 계산
            val totalSize = fileTempDir.listFiles()?.sumOf { it.length() } ?: 0L

            // 메타데이터 객체 생성 및 DB 저장
            val metadata =
                    VideoMetadata(
                            fileName = originalFileName,
                            title = title,
                            description = description,
                            format = fileExtension,
                            size = totalSize
                    )
            val savedMetadata = videoMetadataService.saveMetadata(metadata)

            // 청크 병합 후 임시 폴더 삭제
            fileTempDir.deleteRecursively()
            println("File merged successfully: $originalFileName")
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return CompletableFuture.completedFuture(null)
    }
}
