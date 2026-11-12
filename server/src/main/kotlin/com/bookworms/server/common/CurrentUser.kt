package com.bookworms.server.common

import org.springframework.security.oauth2.jwt.Jwt
import java.util.UUID

// The logged-in user's id. The token keeps it as its subject (see TokenService).
fun Jwt.userId(): UUID = UUID.fromString(subject)
