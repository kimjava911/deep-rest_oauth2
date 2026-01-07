package kr.java.rest_oauth2.config;

import jakarta.servlet.http.HttpServletResponse;
import kr.java.rest_oauth2.service.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * [설명]
 * - Spring Security 6(부트 3.5.x) 표준 DSL 기반 보안 설정
 * - OAuth2 Login을 활성화하여 Google/GitHub/Kakao 로그인 흐름을 처리한다.
 * - 데모 목적상 REST 엔드포인트(/api/**)는 CSRF 예외로 처리한다(운영에서는 별도 전략 권장).
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;

    /**
     * [설명]
     * - SecurityFilterChain은 WebSecurityConfigurerAdapter 대체 방식
     * - OAuth2 로그인 성공 후 홈(/)으로 이동
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // [중요] 데모 단순화를 위해 /api/**는 CSRF 제외
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))

                .authorizeHttpRequests(auth -> auth
                        // 정적 리소스/홈 화면 허용
                        .requestMatchers("/", "/css/**", "/js/**", "/images/**").permitAll()
                        // 인증 정보 조회 API는 비로그인도 접근 가능(응답에서 authenticated=false 처리)
                        .requestMatchers("/api/me").permitAll()
                        // 로그아웃 API는 인증 사용자만 의미가 있으므로 authenticated로 제한
                        .requestMatchers("/api/logout").authenticated()
                        // 그 외는 인증 필요
                        .anyRequest().authenticated()
                )

                .oauth2Login(oauth2 -> oauth2
                        // [설명] 사용자 정보 로딩(프로바이더별 attribute 정규화)을 커스텀 서비스에서 처리
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        // [설명] 성공 시 홈으로 이동
                        .defaultSuccessUrl("/", true)
                )

                // [설명] 기본 로그아웃은 /logout(POST) 이지만, 본 예제는 REST 형태로 /api/logout 제공
//                .logout(logout -> logout.disable())
                .logout(AbstractHttpConfigurer::disable)

                // [설명] 인증 실패 시 API 호출에서 401을 명확히 내리기 위한 기본 설정(브라우저 리다이렉트 최소화)
                .exceptionHandling(eh -> eh
                        .authenticationEntryPoint((req, res, ex) -> {
                            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        })
                )

                .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}