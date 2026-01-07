package kr.java.rest_oauth2.service;

import kr.java.rest_oauth2.domain.SocialUserProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * [설명]
 * - OAuth2 로그인 시, provider에서 내려주는 attributes를 표준화한다.
 * - Kakao는 user-info 응답이 중첩 구조이므로, name/email을 꺼내어 공통 필드로 만든다.
 */
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();

    /**
     * [설명]
     * - OAuth2UserRequest(클라이언트 등록 정보 + 토큰 등)를 바탕으로 사용자 정보를 로드한다.
     * - provider별 attribute 구조를 파싱하여 SocialUserProfile로 정규화한다.
     */
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User rawUser = delegate.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId(); // github/google/kakao
        Map<String, Object> attributes = rawUser.getAttributes();

        SocialUserProfile profile = mapToProfile(registrationId, attributes);

        // [설명] 간단 데모이므로 ROLE_USER 권한 부여
        Set<GrantedAuthority> authorities = new HashSet<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

        // [설명]
        // - principal nameKey는 "id"처럼 고유 식별자를 쓰는 것이 일반적
        // - DefaultOAuth2User는 attributes + authorities만 들고 있으므로,
        //   우리가 만든 profile을 attributes 안에 함께 넣어 API에서 쉽게 꺼내 쓰도록 한다.
        Map<String, Object> merged = new LinkedHashMap<>(attributes);
        merged.put("socialProfile", profile);

        String nameKey = switch (registrationId) {
            case "google" -> "sub";  // OIDC의 subject
            case "github" -> "id";
            case "kakao" -> "id";
            default -> "id";
        };

        return new DefaultOAuth2User(authorities, merged, nameKey);
    }

    /**
     * [설명]
     * - provider별로 다른 응답 구조를 공통 모델로 변환한다.
     */
    private SocialUserProfile mapToProfile(String provider, Map<String, Object> attributes) {
        return switch (provider) {
            case "github" -> {
                // GitHub: id, login, name, email(없을 수 있음)
                String id = String.valueOf(attributes.getOrDefault("id", ""));
                String name = Optional.ofNullable((String) attributes.get("name"))
                        .orElseGet(() -> (String) attributes.getOrDefault("login", "unknown"));
                String email = (String) attributes.get("email");
                yield new SocialUserProfile(provider, id, name, email, attributes);
            }
            case "google" -> {
                // Google(OIDC 포함): sub, name, email
                String id = String.valueOf(attributes.getOrDefault("sub", ""));
                String name = (String) attributes.getOrDefault("name", "unknown");
                String email = (String) attributes.get("email");
                yield new SocialUserProfile(provider, id, name, email, attributes);
            }
            case "kakao" -> {
                // Kakao: id, properties.nickname, kakao_account.email 등 중첩 구조
                // Kakao user-info: https://kapi.kakao.com/v2/user/me :contentReference[oaicite:2]{index=2}
                String id = String.valueOf(attributes.getOrDefault("id", ""));

                String name = "unknown";
                String email = null;

                Object propertiesObj = attributes.get("properties");
                if (propertiesObj instanceof Map<?, ?> props) {
                    Object nickname = props.get("nickname");
                    if (nickname != null) {
                        name = String.valueOf(nickname);
                    }
                }

                Object accountObj = attributes.get("kakao_account");
                if (accountObj instanceof Map<?, ?> account) {
                    Object emailObj = account.get("email");
                    if (emailObj != null) {
                        email = String.valueOf(emailObj);
                    }
                }

                yield new SocialUserProfile(provider, id, name, email, attributes);
            }
            default -> new SocialUserProfile(provider, "unknown", "unknown", null, attributes);
        };
    }
}
