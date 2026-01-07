package kr.java.rest_oauth2.domain;

/**
 * [설명]
 * - 화면에서 쓰기 쉬운 형태로 /api/me 응답을 정의한다.
 */
public record MeResponse(
        boolean authenticated,
        String provider,
        String id,
        String name,
        String email
) {
    public static MeResponse anonymous() {
        return new MeResponse(false, null, null, null, null);
    }

    public static MeResponse of(SocialUserProfile profile) {
        return new MeResponse(true, profile.getProvider(), profile.getId(), profile.getName(), profile.getEmail());
    }
}