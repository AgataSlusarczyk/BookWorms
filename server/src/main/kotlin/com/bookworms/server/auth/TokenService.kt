package com.bookworms.server.auth

import com.bookworms.server.users.User
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class TokenService(
	private val jwtEncoder: JwtEncoder,
	private val jwtProperties: JwtProperties,
) {

	fun createToken(user: User): String {
		val now = Instant.now()
		val claims = JwtClaimsSet.builder()
			.subject(user.id.toString())
			.issuedAt(now)
			.expiresAt(now.plus(jwtProperties.expiration))
			.build()
		val header = JwsHeader.with(MacAlgorithm.HS256).build()
		return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).tokenValue
	}
}
