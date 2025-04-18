package com.example.vaultcast.util

object UploadValidationUtil {

    // 허용된 비디오 파일 확장자 (소문자로 비교)
    private val allowedExtensions = setOf("mp4", "avi", "mkv", "mov", "wmv", "flv", "webm")

    /**
     * 파일 이름에서 확장자를 추출하여 허용된 비디오 형식 목록과 비교
     *
     * @param fileName 원본 파일 이름
     * @param allowedExtensions 허용된 확장자 집합 (예: "mp4", "avi", "mkv", ...)
     * @return 허용된 형식이면 true, 아니면 false
     */
    fun isAllowedExtension(fileName: String): Boolean {
        val cleanedFileName = fileName.substringAfterLast('/')
        val extension = cleanedFileName.substringAfterLast('.', "").lowercase()
        return extension.isNotEmpty() && extension in allowedExtensions
    }

    /**
     * 입력 문자열에 위험한 콘텐츠가 포함되어 있는지 검사 (예: "<script" 문자열 존재 여부)
     *
     * @param input 메타데이터 제목 또는 설명 문자열
     * @return 위험한 콘텐츠가 있으면 true, 없으면 false
     */
    fun containsDangerousContent(input: String): Boolean {
        return input.lowercase().contains("<script")
    }
}
