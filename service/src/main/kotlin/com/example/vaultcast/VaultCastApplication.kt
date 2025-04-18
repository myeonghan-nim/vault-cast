package com.example.vaultcast

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync

@SpringBootApplication @EnableAsync class VaultCastApplication

fun main(args: Array<String>) {
    runApplication<VaultCastApplication>(*args)
}
