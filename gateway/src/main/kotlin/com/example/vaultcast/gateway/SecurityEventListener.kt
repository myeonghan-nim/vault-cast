package com.example.vaultcast.gateway

import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationListener
import org.springframework.security.authentication.event.*
import org.springframework.stereotype.Component

@Component
class SecurityEventListener : ApplicationListener<AbstractAuthenticationEvent> {

    private val log = LoggerFactory.getLogger(SecurityEventListener::class.java)

    override fun onApplicationEvent(event: AbstractAuthenticationEvent) {
        when (event) {
            is AuthenticationSuccessEvent -> {
                val user = event.authentication.name
                log.info("AUTH SUCCESS user={} details={}", user, event.authentication.details)
            }
            is AbstractAuthenticationFailureEvent -> {
                val user = event.authentication.name
                log.warn("AUTH FAILURE user={} exception={}", user, event.exception.message)
            }
            else -> {
                // 필요시 더 많은 이벤트 처리
            }
        }
    }
}
