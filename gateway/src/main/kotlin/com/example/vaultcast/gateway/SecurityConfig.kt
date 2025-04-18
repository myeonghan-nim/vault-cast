package com.example.vaultcast.gateway

import javax.crypto.spec.SecretKeySpec
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.oauth2.jwt.*
import org.springframework.security.web.server.SecurityWebFilterChain

@Configuration
class SecurityConfig(@Value("\${jwt.secret}") private val jwtSecret: String) {
    @Bean
    fun customJwtDecoder(): ReactiveJwtDecoder {
        val keyBytes = jwtSecret.toByteArray(Charsets.UTF_8)
        val secretKey = SecretKeySpec(keyBytes, "HmacSHA256")
        return NimbusReactiveJwtDecoder.withSecretKey(secretKey).build()
    }

    @Bean
    fun springSecurityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        http.csrf()
                .disable()
                .authorizeExchange { ex ->
                    ex.pathMatchers(HttpMethod.OPTIONS)
                            .permitAll()
                            .pathMatchers("/api/v1/auth/**")
                            .permitAll() // 인증 엔드포인트는 공개
                            .pathMatchers("/api/v1/**")
                            .authenticated()
                            .anyExchange()
                            .permitAll()
                }
                .oauth2ResourceServer { oauth2 ->
                    oauth2.jwt { jwtSpec -> jwtSpec.jwtDecoder(customJwtDecoder()) }
                }
        return http.build()
    }
}
