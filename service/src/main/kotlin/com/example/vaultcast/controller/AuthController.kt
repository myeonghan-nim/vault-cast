package com.example.vaultcast.controller

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/v1/auth")
class AuthController(@Value("\${jwt.secret}") private val jwtSecret: String) {

    @PostMapping("/token")
    fun token(
            @RequestParam username: String,
            @RequestParam password: String
    ): ResponseEntity<Map<String, String>> {
        // 실제론 DB 조회 등으로 사용자 검증 필요
        if (username != "user" || password != "pass") {
            return ResponseEntity.status(401).body(mapOf("error" to "Invalid credentials"))
        }

        val now = Instant.now()
        val key = Keys.hmacShaKeyFor(jwtSecret.toByteArray(Charsets.UTF_8))
        val jwt =
                Jwts.builder()
                        .setSubject(username)
                        .setIssuedAt(Date.from(now))
                        .setExpiration(Date.from(now.plus(1, ChronoUnit.HOURS)))
                        // 필요시 역할, 권한 등 클레임 추가
                        .signWith(key, SignatureAlgorithm.HS256)
                        .compact()

        return ResponseEntity.ok(mapOf("token" to jwt))
    }
}
