package univ.example.fincue.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import univ.example.fincue.service.AuthService;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final AuthService authService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.setAllowedOrigins(List.of(
                "http://172.19.3.233:5173",
                "http://localhost:3000",
                "http://127.0.0.1:5173",
                "http://127.0.0.1:3000"
        ));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization", "Content-Type"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/", "/error", "/login/**", "/index.html", "/demo.html", "/auth/token", "/auth/callback", "/auth/kakao/token").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        // 챗봇은 개인 데이터 기반이므로 인증 필요 (JwtAuthenticationFilter 가 처리)
                        .requestMatchers("/api/chatbot/**").authenticated()
                        .requestMatchers("/api/**").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService())
                        )
                        .successHandler(oAuth2LoginSuccessHandler)
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public OAuth2UserService<OAuth2UserRequest, OAuth2User> customOAuth2UserService() {
        return userRequest -> {
            DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
            OAuth2User oAuth2User = delegate.loadUser(userRequest);

            String provider = userRequest.getClientRegistration().getRegistrationId();
            if ("naver".equals(provider)) {
                oAuth2User = flattenNaverUser(oAuth2User, userRequest);
            }

            authService.processOAuthPostLogin(provider, oAuth2User);

            return oAuth2User;
        };
    }

    /**
     * 네이버 /v1/nid/me 응답은 { resultcode, message, response: { id, email, name, ... } } 형태라
     * Spring 기본 user-name-attribute=id 와 맞지 않음. 최상위에 id/name/email 등을 올려 DefaultOAuth2User로 재구성.
     */
    private static OAuth2User flattenNaverUser(OAuth2User loaded, OAuth2UserRequest userRequest) {
        Map<String, Object> source = loaded.getAttributes();
        @SuppressWarnings("unchecked")
        Map<String, Object> response = (Map<String, Object>) source.get("response");
        Map<String, Object> attrs = new LinkedHashMap<>(source);
        if (response != null) {
            Object id = response.get("id");
            if (id != null) attrs.put("id", id.toString());
            for (String key : new String[]{"name", "email", "nickname", "profile_image"}) {
                if (response.get(key) != null) attrs.put(key, response.get(key));
            }
        }
        String userNameAttribute = userRequest.getClientRegistration()
                .getProviderDetails()
                .getUserInfoEndpoint()
                .getUserNameAttributeName();
        return new DefaultOAuth2User(loaded.getAuthorities(), attrs, userNameAttribute);
    }
}

