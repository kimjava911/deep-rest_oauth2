package kr.java.rest_oauth2.domain;

import lombok.Getter;

import java.util.Map;

/**
 * [설명]
 * - 화면/REST API에서 쓰기 쉬운 형태로 OAuth2 사용자 정보를 정규화한 모델
 * - DB 저장 없이도 "provider + id + name + email" 정도를 통일해서 사용할 수 있다.
 */
@Getter
public class SocialUserProfile {

    private final String provider; // github/google/kakao
    private final String id;       // 각 provider의 사용자 식별자
    private final String name;     // 표시 이름
    private final String email;    // 이메일(없을 수도 있음)
    private final Map<String, Object> attributes; // 원본 attributes(디버깅/확장용)

    public SocialUserProfile(String provider, String id, String name, String email, Map<String, Object> attributes) {
        this.provider = provider;
        this.id = id;
        this.name = name;
        this.email = email;
        this.attributes = attributes;
    }
}
