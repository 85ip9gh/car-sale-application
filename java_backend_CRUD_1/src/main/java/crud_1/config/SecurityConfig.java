package crud_1.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

import crud_1.carSale.service.UserService;


//Security Configuration for for the Car Sale Application
@Configuration
@EnableWebSecurity
public class SecurityConfig{
	
	private final RSAKeyProperties rsaKeys;
	
	public SecurityConfig(RSAKeyProperties rsaKeys) {
		this.rsaKeys = rsaKeys;
	}
	
	@Autowired
	private UserService userService;

	// Exact public origin(s), comma separated. Set per environment; the
	// localhost default only applies when nothing is configured.
	@Value("${app.cors.allowed-origins:http://localhost:3000}")
	private String allowedOrigins;

	@Bean
	PasswordEncoder passwordEncoder() {
		 return new BCryptPasswordEncoder();
		  //return NoOpPasswordEncoder.getInstance();
	  }
	
	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
  	
		return http
				.csrf(csrf -> csrf.disable())
				.cors(Customizer.withDefaults())
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/addUser").permitAll()
						// Container health check only; nginx never proxies /actuator.
						.requestMatchers("/actuator/health").permitAll()
						.anyRequest().authenticated())
				.userDetailsService(userService)
				.oauth2ResourceServer((oauth2) -> oauth2
					    .jwt(Customizer.withDefaults()))
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.httpBasic(Customizer.withDefaults())
				.build();
	}
  
  @Bean
  JwtDecoder jwtDecoder() {
	  return NimbusJwtDecoder.withPublicKey(rsaKeys.publicKey()).build();
  }
  
  @Bean
  JwtEncoder jwtEncoder() {
	  JWK jwk = new RSAKey.Builder(rsaKeys.publicKey()).privateKey(rsaKeys.privateKey()).build();
	  JWKSource<SecurityContext> jwks = new ImmutableJWKSet<>(new JWKSet(jwk));
	  
	  return new NimbusJwtEncoder(jwks);
  }	
  
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(Arrays.stream(allowedOrigins.split(","))
        		.map(String::trim)
        		.filter(origin -> !origin.isEmpty())
        		.toList());

        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT","OPTIONS","PATCH", "DELETE"));
        configuration.setAllowedHeaders(List.of("Access-Control-Allow-Headers", "Authorization", "Content-Type"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
    
    
	
}
