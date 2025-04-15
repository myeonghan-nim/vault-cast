package com.example.vaultcast.controller

import com.example.vaultcast.model.VideoMetadata
import com.example.vaultcast.service.VideoMetadataService
import com.example.vaultcast.util.FileSizeValidator
import com.example.vaultcast.util.UploadValidationUtil
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.util.StringUtils
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
class UploadController(private val videoMetadataService: VideoMetadataService) {

    // 파일 저장 디렉토리 설정 (프로젝트 루트에 uploads 폴더 생성)
    private val uploadDir = "uploads"

    init {
        // 업로드 디렉토리가 없으면 생성
        val uploadPath = Paths.get(uploadDir)
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath)
        }
    }

    /**
     * 업로드 엔드포인트
     *
     * @param file: 전송된 파일
     * @param title, description: 메타데이터 (간단한 위험 내용 검사 수행)
     */
    @PostMapping("/upload")
    fun uploadVideo(
            @RequestParam("file") file: MultipartFile,
            @RequestParam("title", required = false) title: String?,
            @RequestParam("description", required = false) description: String?
    ): ResponseEntity<Map<String, String>> {
        // 1. 파일이 비어있는지 확인
        if (file.isEmpty) {
            return ResponseEntity.badRequest().body(mapOf("error" to "Uploaded file is empty"))
        }

        // 2. 파일 크기 체크
        if (FileSizeValidator.isFileSizeValid(file.size).not()) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                    .body(mapOf("error" to "File size exceeds the allowed limit of 4GB"))
        }

        // 3. 파일 확장자 체크
        val fileName = StringUtils.cleanPath(file.originalFilename ?: "unknown")
        if (UploadValidationUtil.isAllowedExtension(fileName).not()) {
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                    .body(mapOf("error" to "Only video files with extensions are allowed"))
        }
        val fileExtension = fileName.substringAfterLast('.', "").lowercase()

        // 4. 메타데이터 위험 요소 검증 (간단한 XSS 필터링)
        if (title != null && UploadValidationUtil.containsDangerousContent(title)) {
            return ResponseEntity.badRequest()
                    .body(mapOf("error" to "Title contains disallowed or dangerous content"))
        }
        if (description != null && UploadValidationUtil.containsDangerousContent(description)) {
            return ResponseEntity.badRequest()
                    .body(mapOf("error" to "Description contains disallowed or dangerous content"))
        }

        // 5. 파일 저장
        val targetLocation = Paths.get(uploadDir).resolve(fileName).toAbsolutePath()

        return try {
            // 파일을 지정한 경로에 복사 (같은 이름의 파일이 있으면 교체)
            Files.copy(file.inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING)

            // 메타데이터 객체 생성 및 DB 저장
            val metadata =
                    VideoMetadata(
                            fileName = fileName,
                            title = title,
                            description = description,
                            format = fileExtension,
                            size = file.size
                    )
            val savedMetadata = videoMetadataService.saveMetadata(metadata)

            ResponseEntity.status(HttpStatus.CREATED)
                    .body(mapOf("message" to "File uploaded successfully", "fileName" to fileName, "metadataId" to savedMetadata.id.toString()))
        } catch (ex: IOException) {
            ex.printStackTrace()
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                            mapOf(
                                    "error" to "Could not store file. Please try again!",
                                    "exception" to (ex.message ?: "unknown error")
                            )
                    )
        }
    }
}
