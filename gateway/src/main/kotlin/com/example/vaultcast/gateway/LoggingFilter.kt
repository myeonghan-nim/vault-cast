package com.example.vaultcast.gateway

import org.slf4j.LoggerFactory
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.core.Ordered
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@Component
class LoggingFilter : GlobalFilter, Ordered {
    private val log = LoggerFactory.getLogger(LoggingFilter::class.java)

    override fun filter(
            exchange: ServerWebExchange,
            chain: org.springframework.cloud.gateway.filter.GatewayFilterChain
    ): Mono<Void> {
        val request: ServerHttpRequest = exchange.request
        log.info("→ Request: {} {} headers={}", request.method, request.uri, request.headers)

        return chain.filter(exchange)
                .then(
                        Mono.fromRunnable {
                            val response = exchange.response
                            log.info(
                                    "← Response: {} for {} {}",
                                    response.statusCode,
                                    request.method,
                                    request.uri
                            )
                        }
                )
    }

    // 필터 우선순위 (낮을수록 먼저 실행)
    override fun getOrder(): Int = Ordered.LOWEST_PRECEDENCE
}
