package com.bookworms.server.common

import com.bookworms.server.auth.JwtProperties
import com.nimbusds.jose.jwk.source.ImmutableSecret
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import org.springframework.security.web.SecurityFilterChain
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

@Configuration
@EnableConfigurationProperties(JwtProperties::class)
class SecurityConfig {

	@Bean
	fun securityFilterChain(http: HttpSecurity): SecurityFilterChain =
		http
			.csrf { it.disable() }
			.sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
			.authorizeHttpRequests {
				it.requestMatchers("/auth/register", "/auth/login", "/error").permitAll()
				it.anyRequest().authenticated()
			}
			.oauth2ResourceServer { it.jwt { } }
			.build()

	@Bean
	fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

	@Bean
	fun jwtSecretKey(jwtProperties: JwtProperties): SecretKey =
		SecretKeySpec(jwtProperties.secret.toByteArray(), "HmacSHA256")

	@Bean
	fun jwtEncoder(jwtSecretKey: SecretKey): JwtEncoder =
		NimbusJwtEncoder(ImmutableSecret(jwtSecretKey))

	@Bean
	fun jwtDecoder(jwtSecretKey: SecretKey): JwtDecoder =
		NimbusJwtDecoder.withSecretKey(jwtSecretKey).build()
}
