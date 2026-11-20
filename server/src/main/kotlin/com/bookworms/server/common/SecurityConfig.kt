package com.bookworms.server.common

import com.bookworms.server.auth.JwtProperties
import com.nimbusds.jose.jwk.source.ImmutableSecret
import jakarta.servlet.DispatcherType
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
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
				// The rest of an answer that keeps going (the chat's live stream). The request itself was checked
				// when it came in – this only lets the server finish writing to it.
				it.dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()
				// Who in my groups has a book – only for group members, so it's checked before the public catalog below.
				it.requestMatchers(HttpMethod.GET, "/books/*/copies").authenticated()
				// The public catalog: people browse books before they have an account.
				it.requestMatchers(HttpMethod.GET, "/books", "/books/**", "/covers/**").permitAll()
				// Profile photos: image loaders on the phone don't send the token. The file names can't be guessed.
				it.requestMatchers(HttpMethod.GET, "/photos/**").permitAll()
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
