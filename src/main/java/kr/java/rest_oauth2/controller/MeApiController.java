package kr.java.rest_oauth2.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.java.rest_oauth2.domain.MeResponse;
import kr.java.rest_oauth2.domain.SocialUserProfile;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * [설명]
 * - Thymeleaf(SSR) 화면에서 fetch로 호출할 REST API 제공
 * - DB 없이도 "현재 로그인 사용자" 정보를 JSON으로 내려준다.

 * - 인증 여부에 관계없이 200 OK로 응답한다.
 * - AnonymousAuthenticationToken을 확실히 걸러서 authenticated=false를 정확히 만든다.
 * - Google(OIDC) / GitHub(OAuth2) / Kakao(OAuth2) 모두 principal 타입이 달라도 처리 가능하도록 한다.
 */
@RestController
@RequestMapping("/api")
public class MeApiController {

    /**
     * [설명]
     * - 인증 여부에 상관없이 200 OK로 내려준다(화면에서 처리 단순화)
     * - 로그인 상태면 CustomOAuth2UserService에서 넣어둔 socialProfile을 꺼내 반환한다.
     */
    @GetMapping("/me")
    public MeResponse me() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // [중요] AnonymousAuthenticationToken은 isAuthenticated()가 true이므로 반드시 제외해야 한다.
        if (authentication == null
                || authentication instanceof AnonymousAuthenticationToken
                || !authentication.isAuthenticated()) {
            return MeResponse.anonymous();
        }

        // [설명] OAuth2 로그인(google/github/kakao)인 경우 registrationId를 얻을 수 있다.
        String provider = null;
        if (authentication instanceof OAuth2AuthenticationToken oauth2Token) {
            provider = oauth2Token.getAuthorizedClientRegistrationId(); // "google" / "github" / "kakao"
        }

        Object principal = authentication.getPrincipal();

        // 1) Google(OIDC): principal이 OidcUser(= DefaultOidcUser)일 수 있다.
        if (principal instanceof OidcUser oidcUser) {
            // [설명] OIDC 표준 클레임에서 최소 정보 구성
            String id = oidcUser.getSubject(); // sub
            String name = oidcUser.getFullName();
            String email = oidcUser.getEmail();

            SocialUserProfile profile = new SocialUserProfile(
                    provider != null ? provider : "google",
                    id != null ? id : "",
                    name != null ? name : "unknown",
                    email,
                    oidcUser.getClaims()
            );
            return MeResponse.of(profile);
        }

        // 2) GitHub/Kakao: principal이 OAuth2User(= DefaultOAuth2User 포함)일 수 있다.
        if (principal instanceof OAuth2User oauth2User) {
            Object profileObj = oauth2User.getAttributes().get("socialProfile");

            // [설명] CustomOAuth2UserService에서 넣어둔 socialProfile이 있으면 그걸 사용
            if (profileObj instanceof SocialUserProfile profile) {
                return MeResponse.of(profile);
            }

            // [대안] socialProfile이 없더라도 최소 정보만이라도 내려주고 싶다면 여기서 provider별 파싱 가능
            SocialUserProfile fallback = new SocialUserProfile(
                    provider != null ? provider : "unknown",
                    String.valueOf(oauth2User.getAttributes().getOrDefault("id", "")),
                    String.valueOf(oauth2User.getAttributes().getOrDefault("name", "unknown")),
                    (String) oauth2User.getAttributes().get("email"),
                    oauth2User.getAttributes()
            );
            return MeResponse.of(fallback);
        }

        // [설명] 예상치 못한 principal 구조인 경우 안전하게 anonymous 처리
        return MeResponse.anonymous();
    }

    /**
     * [설명]
     * - REST 스타일 로그아웃(세션 무효화)
     * - SecurityConfig에서 logout을 disable 했으므로 직접 처리한다.
     */
    @PostMapping("/logout")
    public void logout(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.getSession(false); // 세션이 있으면 접근(없어도 무관)

        // [설명] SecurityContext 제거
        SecurityContextHolder.clearContext();

        // [설명] 세션 무효화
        var session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }
}