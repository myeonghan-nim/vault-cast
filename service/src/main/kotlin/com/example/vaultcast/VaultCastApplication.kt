package com.example.vaultcast

import com.example.vaultcast.config.FileStorageProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync

@SpringBootApplication
@EnableAsync
@EnableConfigurationProperties(FileStorageProperties::class)
class VaultCastApplication

fun main(args: Array<String>) {
    runApplication<VaultCastApplication>(*args)
}
