package com.packtpub.springsecurity.configuration;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer.FrameOptionsConfig;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.core.userdetails.UserDetailsByNameServiceWrapper;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;
import org.springframework.security.web.authentication.preauth.x509.X509AuthenticationFilter;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;

import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;

/**
 * Spring Security Config Class
 *
 * @since chapter08.00
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

	
	@Bean
	public AuthenticationManager authManager(HttpSecurity http, 
			PreAuthenticatedAuthenticationProvider preAuthAuthenticationProvider) throws Exception {
		AuthenticationManagerBuilder authenticationManagerBuilder = http.getSharedObject(AuthenticationManagerBuilder.class);
		http.authenticationProvider(preAuthAuthenticationProvider);
		return authenticationManagerBuilder.build();
	}

	/**
	 * Filter chain security filter chain.
	 *
	 * @param http the http
	 * @return the security filter chain
	 * @throws Exception the exception
	 */
	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http,
			PersistentTokenRepository persistentTokenRepository,
			X509AuthenticationFilter x509Filter, UserDetailsService userDetailsService) throws Exception {
		http.authorizeRequests( authz -> authz
						.requestMatchers(antMatcher("/webjars/**")).permitAll()
						.requestMatchers(antMatcher("/css/**")).permitAll()
						.requestMatchers(antMatcher("/favicon.ico")).permitAll()
						// H2 console:
						.requestMatchers(antMatcher("/admin/h2/**")).access("isFullyAuthenticated() and hasRole('ADMIN')")
						.requestMatchers(antMatcher("/")).permitAll()
						.requestMatchers(antMatcher("/login/*")).permitAll()
						.requestMatchers(antMatcher("/logout")).permitAll()
						.requestMatchers(antMatcher("/signup/*")).permitAll()
						.requestMatchers(antMatcher("/errors/**")).permitAll()
						.requestMatchers(antMatcher("/events/")).hasRole("ADMIN")
						.requestMatchers(antMatcher("/**")).hasRole("USER"))

				.exceptionHandling(exceptions -> exceptions
						.accessDeniedPage("/errors/403"))

				.formLogin(form -> form
						.loginPage("/login/form")
						.loginProcessingUrl("/login")
						.failureUrl("/login/form?error")
						.usernameParameter("username")
						.passwordParameter("password")
						.defaultSuccessUrl("/default", true)
						.permitAll())
				.logout(form -> form
						.logoutUrl("/logout")
						.logoutSuccessUrl("/login/form?logout")
						.permitAll())
				// CSRF is enabled by default, with Java Config
				.csrf(AbstractHttpConfigurer::disable);
		// For H2 Console
		http.headers(headers -> headers.frameOptions(FrameOptionsConfig::disable));
		
		// Remember Me
		http.rememberMe(httpSecurityRememberMeConfigurer -> httpSecurityRememberMeConfigurer
				.key("jbcpCalendar").tokenRepository(persistentTokenRepository));
		// SSL / TLS x509 support
		http.x509(httpSecurityX509Configurer -> httpSecurityX509Configurer
				.x509AuthenticationFilter(x509Filter));

		return http.build();
	}


	/**
	 * Standard SHA-256 Password Encoder
	 *
	 * @return ShaPasswordEncoder
	 */
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder(4);
	}

	@Bean
	public PersistentTokenRepository persistentTokenRepository(DataSource dataSource) {
		JdbcTokenRepositoryImpl db = new JdbcTokenRepositoryImpl();
		db.setDataSource(dataSource);
		return db;
	}

	@Bean
	public X509AuthenticationFilter x509Filter(AuthenticationManager authenticationManager){
		return new X509AuthenticationFilter(){{
			setAuthenticationManager(authenticationManager);
		}};
	}

	@Bean
	public PreAuthenticatedAuthenticationProvider preAuthAuthenticationProvider(final AuthenticationUserDetailsService authenticationUserDetailsService){
		return new PreAuthenticatedAuthenticationProvider(){{
			setPreAuthenticatedUserDetailsService(authenticationUserDetailsService);
		}};
	}

	@Bean
	public UserDetailsByNameServiceWrapper authenticationUserDetailsService(final UserDetailsService userDetailsService){
		return new UserDetailsByNameServiceWrapper(userDetailsService);
	}

}