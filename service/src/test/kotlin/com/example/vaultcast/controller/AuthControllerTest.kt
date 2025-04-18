package com.example.vaultcast.controller

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest(@Autowired val mockMvc: MockMvc) {

    private val authUrl = "/v1/auth/token"

    @Test
    fun `test auth successful`() {
        mockMvc.perform(
                        post(authUrl)
                                .param("username", "user")
                                .param("password", "pass")
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.token").isNotEmpty)
    }

    @Test
    fun `test auth invalid credentials`() {
        mockMvc.perform(post(authUrl).param("username", "bad").param("password", "creds"))
                .andExpect(status().isUnauthorized)
                .andExpect(jsonPath("$.error").value("Invalid credentials"))
    }
}
