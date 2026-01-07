package kr.java.rest_oauth2;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * [설명]
 * - OAuth2 인증 유무에 따라 /api/me가 기대 형태로 응답하는지 스모크 테스트 수준으로 검증한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class MeApiControllerTest {

    @Autowired
    MockMvc mockMvc;

    /**
     * [설명]
     * - 비로그인 상태에서도 200으로 내려주며 authenticated=false를 기대한다.
     */
    @Test
    void me_anonymous_returns_authenticated_false() throws Exception {
        mockMvc.perform(get("/api/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(false));
    }


    @Test
    void me_authenticated_provider_github() throws Exception {
        Map<String, Object> attrs = Map.of(
                // [중요] DefaultOAuth2User의 nameAttributeKey로 사용할 키는 최상위에 존재 + non-null 이어야 한다.
                "id", "1",
                "name", "tester",
                "socialProfile", Map.of(
                        "provider", "github",
                        "id", "1",
                        "name", "tester",
                        "email", "t@test.com"
                )
        );

        DefaultOAuth2User principal = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                attrs,
                "id"
        );

        OAuth2AuthenticationToken oauth2Auth =
                new OAuth2AuthenticationToken(principal, principal.getAuthorities(), "github");

        mockMvc.perform(get("/api/me")
                        // [핵심] SecurityContextHolder에 인증 주입
                        .with(authentication(oauth2Auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.provider").value("github"))
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.name").value("tester"));
    }
}
