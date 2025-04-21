package com.example.vaultcast.gateway

import java.time.Instant
import org.slf4j.LoggerFactory
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.core.Ordered
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@Component
class AuditLoggingFilter : GlobalFilter, Ordered {
    private val log = LoggerFactory.getLogger("AUDIT")

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        val startTime = System.currentTimeMillis()
        val request = exchange.request

        return chain.filter(exchange)
                .then(
                        Mono.defer {
                            // 응답이 완료된 시점에 실행
                            val duration = System.currentTimeMillis() - startTime
                            val status =
                                    exchange.response.statusCode ?: HttpStatus.INTERNAL_SERVER_ERROR
                            val clientIp = request.remoteAddress?.hostString ?: "unknown"
                            val method = request.methodValue
                            val uri = request.uri

                            // 로그를 남기기 전에 principal 획득
                            // principal은 Mono로 감싸져 있으므로 flatMap을 사용
                            // principal이 없을 경우 "anonymous"로 대체
                            exchange.getSession()
                                    .flatMap { session ->
                                        session.attributes["principal"] as? Mono<*> ?: Mono.empty()
                                    }
                                    .cast(
                                            org.springframework.security.core.Authentication::class
                                                    .java
                                    )
                                    .map { it.principal }
                                    .defaultIfEmpty("anonymous")
                                    .switchIfEmpty(Mono.just("anonymous"))
                                    .doOnNext { principal ->
                                        log.info(
                                                "AUDIT | {} | {} | {} {} | {} | {}ms | principal={}",
                                                Instant.now(),
                                                clientIp,
                                                method,
                                                uri,
                                                status,
                                                duration,
                                                principal
                                        )
                                    }
                                    .then()
                        }
                )
    }

    override fun getOrder(): Int = Ordered.LOWEST_PRECEDENCE
}
