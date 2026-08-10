package com.garmentDesign.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.session.HttpSessionEventPublisher;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder(12);
	}

	@Bean
	public SecurityContextRepository securityContextRepository() {
		return new HttpSessionSecurityContextRepository();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();

		configuration.setAllowedOrigins(List.of("http://localhost:5173"));

		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

		configuration.setAllowedHeaders(List.of("Content-Type", "X-XSRF-TOKEN"));

		configuration.setAllowCredentials(true);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

		source.registerCorsConfiguration("/**", configuration);

		return source;
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityContextRepository repository,
			SessionRegistry sessionRegistry) throws Exception {

		http.cors(Customizer.withDefaults())

				.csrf(csrf -> csrf.spa())

				.securityContext(context -> context.securityContextRepository(repository).requireExplicitSave(true))

				.sessionManagement(session -> {
					session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
							.sessionFixation(fixation -> fixation.changeSessionId());

					/*
					 * -1 nghĩa là không giới hạn số thiết bị đăng nhập. SessionRegistry vẫn theo
					 * dõi các session để có thể vô hiệu hóa sau khi đổi mật khẩu.
					 */
					session.maximumSessions(-1).sessionRegistry(sessionRegistry).expiredSessionStrategy(event -> {
						event.getResponse().setStatus(HttpStatus.UNAUTHORIZED.value());

						event.getResponse().setCharacterEncoding("UTF-8");

						event.getResponse().setContentType(MediaType.APPLICATION_JSON_VALUE);

						event.getResponse().getWriter().write("""
								{
								  "message":
								  "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại"
								}
								""");
					});
				})

				.authorizeHttpRequests(auth -> auth
						/*
						 * Endpoint xác thực công khai
						 */
						.requestMatchers("/api/auth/login", "/api/auth/send-otp", "/api/auth/verify-otp",
								"/api/auth/google-login", "/api/auth/register", "/api/auth/send-email-otp",
								"/api/auth/verify-email-otp", "/api/auth/forgot-password",
								"/api/auth/verify-forgot-otp", "/api/auth/reset-password", "/api/auth/csrf",
								"/api/auth/me", "/uploads/**")
						.permitAll()

						/*
						 * Nội dung công khai trang chủ
						 */
						.requestMatchers(HttpMethod.GET, "/api/services/**", "/api/service-reviews/public").permitAll()

						.requestMatchers(HttpMethod.POST, "/api/newsletter/subscribe", "/api/mail/contact").permitAll()

						/*
						 * API chỉ dành cho admin
						 */
						.requestMatchers("/api/roles/**", "/api/user-auth-providers/**").hasRole("ADMIN")

						/*
						 * Các endpoint còn lại phải đăng nhập
						 */
						.anyRequest().authenticated())

				.exceptionHandling(errors -> errors.authenticationEntryPoint((request, response, exception) -> {
					response.setStatus(HttpStatus.UNAUTHORIZED.value());

					response.setCharacterEncoding("UTF-8");

					response.setContentType(MediaType.APPLICATION_JSON_VALUE);

					response.getWriter().write("""
							{
							  "message":
							  "Bạn chưa đăng nhập"
							}
							""");
				})

						.accessDeniedHandler((request, response, exception) -> {
							response.setStatus(HttpStatus.FORBIDDEN.value());

							response.setCharacterEncoding("UTF-8");

							response.setContentType(MediaType.APPLICATION_JSON_VALUE);

							response.getWriter().write("""
									{
									  "message":
									  "Bạn không có quyền truy cập"
									}
									""");
						}))

				.formLogin(form -> form.disable())

				.logout(logout -> logout.logoutUrl("/api/auth/logout").invalidateHttpSession(true)
						.clearAuthentication(true).deleteCookies("JSESSIONID")

						.logoutSuccessHandler((request, response, authentication) -> {
							response.setStatus(HttpStatus.OK.value());

							response.setCharacterEncoding("UTF-8");

							response.setContentType(MediaType.APPLICATION_JSON_VALUE);

							response.getWriter().write("""
									{
									  "message":
									  "Đăng xuất thành công"
									}
									""");
						}));

		return http.build();
	}

	@Bean
	public SessionRegistry sessionRegistry() {
		return new SessionRegistryImpl();
	}

	@Bean
	public HttpSessionEventPublisher httpSessionEventPublisher() {
		return new HttpSessionEventPublisher();
	}
}