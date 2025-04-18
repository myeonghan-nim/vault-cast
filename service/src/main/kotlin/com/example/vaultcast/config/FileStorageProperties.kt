package com.example.vaultcast.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "file")
data class FileStorageProperties(var uploadDir: String = "uploads")
