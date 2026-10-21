package com.bookworms.server.auth

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties("bookworms.jwt")
data class JwtProperties(
	val secret: String,
	val expiration: Duration,
)
