# OAuth2 REST Social Login Demo

Spring Boot 3 + Spring Security 6 기반의 **소셜 로그인 데모 프로젝트**입니다.  
Google, GitHub, Kakao 로그인을 REST API 형태로 제공하며, DB 없이 세션만으로 인증을 처리합니다.

<br>

## 📌 주요 기능

- ✅ **다중 소셜 로그인 지원**: Google (OIDC) / GitHub / Kakao
- ✅ **REST API 방식**: `/api/me`, `/api/logout` 엔드포인트 제공
- ✅ **프로바이더별 응답 정규화**: 각 OAuth Provider의 서로 다른 응답 구조를 통일된 모델로 변환
- ✅ **DB 없는 경량 구조**: 세션 기반 인증만으로 동작
- ✅ **Spring Security 6 표준 DSL**: SecurityFilterChain 기반 최신 보안 설정

<br>

## 🛠 기술 스택

| 분류 | 기술 |
|------|------|
| Framework | Spring Boot 3.5.9, Spring Security 6 |
| Template Engine | Thymeleaf |
| Build Tool | Gradle |
| Java Version | 17 |
| OAuth2 Providers | Google (OIDC), GitHub, Kakao |

<br>

## 🚀 시작하기

### 1️⃣ 사전 준비: OAuth 애플리케이션 등록

각 소셜 플랫폼에서 OAuth 애플리케이션을 등록하고 `Client ID`와 `Client Secret`을 발급받아야 합니다.

#### 🔹 Google OAuth 설정
1. [Google Cloud Console](https://console.cloud.google.com/) 접속
2. **프로젝트 생성** → **API 및 서비스** → **사용자 인증 정보**
3. **OAuth 2.0 클라이언트 ID 만들기**
    - 애플리케이션 유형: `웹 애플리케이션`
    - 승인된 리디렉션 URI: `http://localhost:8080/login/oauth2/code/google`
4. 발급받은 **클라이언트 ID**, **클라이언트 보안 비밀** 복사

#### 🔹 GitHub OAuth 설정
1. [GitHub Developer Settings](https://github.com/settings/developers) 접속
2. **OAuth Apps** → **New OAuth App**
3. 설정값 입력:
    - Homepage URL: `http://localhost:8080`
    - Authorization callback URL: `http://localhost:8080/login/oauth2/code/github`
4. 발급받은 **Client ID**, **Client Secret** 복사

#### 🔹 Kakao OAuth 설정
1. [Kakao Developers](https://developers.kakao.com/) 접속 → 애플리케이션 추가
2. **내 애플리케이션** → **앱 설정** → **앱 키**에서 `REST API 키` 확인
3. **플랫폼** → **Web 플랫폼 등록**
    - 사이트 도메인: `http://localhost:8080`
4. **제품 설정** → **Kakao Login**
    - Redirect URI: `http://localhost:8080/login/oauth2/code/kakao` 등록
    - 동의 항목: `프로필 정보(닉네임)` 필수 동의로 설정
5. **보안** → **Client Secret** 발급 (코드 생성 활성화)

<br>

### 2️⃣ 환경 설정

#### application-dev.yml 생성

프로젝트 루트의 `src/main/resources/` 폴더에 `application-dev.yml` 파일을 생성합니다.

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          github:
            client-id: YOUR_GITHUB_CLIENT_ID
            client-secret: YOUR_GITHUB_CLIENT_SECRET
            scope: [ read:user, user:email ]

          google:
            client-id: YOUR_GOOGLE_CLIENT_ID
            client-secret: YOUR_GOOGLE_CLIENT_SECRET
            scope: [ openid, profile, email ]

          kakao:
            client-id: YOUR_KAKAO_CLIENT_ID
            client-secret: YOUR_KAKAO_CLIENT_SECRET
            client-authentication-method: client_secret_post
            authorization-grant-type: authorization_code
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
            scope: [ profile_nickname ]

        provider:
          kakao:
            authorization-uri: https://kauth.kakao.com/oauth/authorize
            token-uri: https://kauth.kakao.com/oauth/token
            user-info-uri: https://kapi.kakao.com/v2/user/me
            user-name-attribute: id
```

> ⚠️ **주의**: `application-dev.yml`은 `.gitignore`에 등록되어 있어 Git에 커밋되지 않습니다.  
> 실제 인증 정보는 환경 변수나 시크릿 관리 도구를 사용하세요.

<br>

### 3️⃣ 애플리케이션 실행

```bash
# Gradle 빌드 및 실행
./gradlew bootRun

# 또는 IDE에서 RestOauth2Application.java 실행
```

서버가 정상 실행되면 브라우저에서 접속:
```
http://localhost:8080
```

<br>

## 🎬 시연 플로우

### 📺 화면 구성

`home.html` 하나로 모든 기능을 제공합니다:

```
┌─────────────────────────────────────────────┐
│  소셜 로그인 (Google / GitHub / Kakao)      │
│                                             │
│  [Google 로그인] [GitHub 로그인] [Kakao 로그인] │
│                                             │
│  [REST 로그아웃]                             │
│                                             │
│  /api/me 응답                                │
│  ┌─────────────────────────────────────┐   │
│  │ {                                   │   │
│  │   "authenticated": false,           │   │
│  │   "provider": null,                 │   │
│  │   "id": null,                       │   │
│  │   "name": null,                     │   │
│  │   "email": null                     │   │
│  │ }                                   │   │
│  └─────────────────────────────────────┘   │
└─────────────────────────────────────────────┘
```

<br>

### 🔄 로그인 플로우

#### Step 1: 초기 화면 (비로그인 상태)

1. 브라우저에서 `http://localhost:8080` 접속
2. 페이지 로드와 동시에 JavaScript가 `/api/me`를 자동 호출
3. 응답 예시:
```json
{
  "authenticated": false,
  "provider": null,
  "id": null,
  "name": null,
  "email": null
}
```

#### Step 2: 소셜 로그인 버튼 클릭

원하는 플랫폼의 로그인 버튼을 클릭합니다.

**Google 로그인 예시:**
```
클릭 → /oauth2/authorization/google 리다이렉트
→ Google 로그인 페이지 이동
→ 계정 선택 및 권한 동의
→ /login/oauth2/code/google?code=... 콜백
→ Spring Security가 자동으로 토큰 교환 및 사용자 정보 로드
→ / (홈) 리다이렉트
```

#### Step 3: 로그인 성공 후 화면

페이지가 자동으로 `/api/me`를 다시 호출하여 사용자 정보를 표시합니다.

**Google 로그인 성공 응답 예시:**
```json
{
  "authenticated": true,
  "provider": "google",
  "id": "112233445566778899",
  "name": "홍길동",
  "email": "hong@gmail.com"
}
```

**GitHub 로그인 성공 응답 예시:**
```json
{
  "authenticated": true,
  "provider": "github",
  "id": "12345678",
  "name": "hong-gildong",
  "email": "hong@users.noreply.github.com"
}
```

**Kakao 로그인 성공 응답 예시:**
```json
{
  "authenticated": true,
  "provider": "kakao",
  "id": "9876543210",
  "name": "홍길동",
  "email": null
}
```

> 💡 **참고**: Kakao는 이메일 동의 항목을 추가로 설정하지 않으면 `email`이 `null`로 반환됩니다.

<br>

### 🚪 로그아웃 플로우

#### Step 1: REST 로그아웃 버튼 클릭

```javascript
// home.html의 JavaScript 코드
async function logout() {
    const res = await fetch('/api/logout', {
        method: 'POST',
        credentials: 'same-origin'
    });
    
    if (res.status === 204) {
        await loadMe();  // /api/me 재호출
        alert('로그아웃 완료');
    }
}
```

#### Step 2: 서버 처리

```java
// MeApiController.java
@PostMapping("/api/logout")
public void logout(HttpServletRequest request, HttpServletResponse response) {
    SecurityContextHolder.clearContext();  // 인증 정보 제거
    HttpSession session = request.getSession(false);
    if (session != null) {
        session.invalidate();  // 세션 무효화
    }
    response.setStatus(204);  // No Content
}
```

#### Step 3: 로그아웃 후 화면

`/api/me` 응답이 다시 비로그인 상태로 변경됩니다:
```json
{
  "authenticated": false,
  "provider": null,
  "id": null,
  "name": null,
  "email": null
}
```

<br>

## 📡 API 명세

### GET /api/me

**현재 로그인 사용자 정보 조회**

- **인증 요구**: ❌ (비로그인도 접근 가능, `authenticated: false` 반환)
- **응답 형식**: JSON

#### 응답 예시 (비로그인)
```json
{
  "authenticated": false,
  "provider": null,
  "id": null,
  "name": null,
  "email": null
}
```

#### 응답 예시 (로그인)
```json
{
  "authenticated": true,
  "provider": "google",
  "id": "112233445566778899",
  "name": "홍길동",
  "email": "hong@gmail.com"
}
```

<br>

### POST /api/logout

**REST 스타일 로그아웃**

- **인증 요구**: ✅ (로그인 상태에서만 의미 있음)
- **응답 코드**: `204 No Content`

#### 처리 과정
1. SecurityContext 제거
2. HTTP 세션 무효화
3. 204 응답 반환

<br>

### GET /oauth2/authorization/{provider}

**소셜 로그인 시작 엔드포인트 (Spring Security 자동 생성)**

- **provider**: `google`, `github`, `kakao`
- **동작**: 해당 Provider의 OAuth 인증 페이지로 리다이렉트

<br>

## 🏗 주요 구현 포인트

### 1️⃣ Provider별 사용자 정보 정규화

각 OAuth Provider는 서로 다른 응답 구조를 갖습니다:

| Provider | ID 필드 | 이름 필드 | 이메일 필드 | 특징 |
|----------|---------|-----------|-------------|------|
| Google | `sub` | `name` | `email` | OIDC 표준 클레임 |
| GitHub | `id` | `name` 또는 `login` | `email` | 평탄한 구조 |
| Kakao | `id` | `properties.nickname` | `kakao_account.email` | 중첩 구조 |

**해결 방법**: `CustomOAuth2UserService`에서 통일된 `SocialUserProfile` 모델로 변환

```java
private SocialUserProfile mapToProfile(String provider, Map<String, Object> attributes) {
    return switch (provider) {
        case "github" -> { /* GitHub 파싱 로직 */ }
        case "google" -> { /* Google 파싱 로직 */ }
        case "kakao" -> { /* Kakao 중첩 구조 파싱 */ }
        default -> /* 기본 처리 */;
    };
}
```

<br>

### 2️⃣ AnonymousAuthenticationToken 처리

Spring Security는 비로그인 사용자에게도 `AnonymousAuthenticationToken`을 부여하며, 이는 `isAuthenticated()`가 `true`를 반환합니다.

```java
// ❌ 잘못된 처리
if (authentication.isAuthenticated()) {
    return MeResponse.of(...);  // 비로그인도 여기로 들어옴!
}

// ✅ 올바른 처리
if (authentication == null 
    || authentication instanceof AnonymousAuthenticationToken
    || !authentication.isAuthenticated()) {
    return MeResponse.anonymous();
}
```

<br>

### 3️⃣ CSRF 예외 처리

REST API는 CSRF 토큰 검증이 불필요하므로 `/api/**` 경로를 예외 처리합니다.

```java
http.csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
```

> ⚠️ **주의**: 운영 환경에서는 CORS 설정, API Key 인증 등 추가 보안 전략을 검토하세요.

<br>

### 4️⃣ Google OIDC vs OAuth2 구분

Google은 OIDC를 지원하므로 `principal`이 `OidcUser` 타입입니다.

```java
if (principal instanceof OidcUser oidcUser) {
    String id = oidcUser.getSubject();  // OIDC 표준 클레임
    String name = oidcUser.getFullName();
    String email = oidcUser.getEmail();
    // ...
}
```

GitHub/Kakao는 일반 `OAuth2User`이므로 `attributes`를 직접 파싱합니다.

<br>

## 🔧 트러블슈팅

### 문제 1: `redirect_uri_mismatch` 오류

**원인**: OAuth 애플리케이션 설정의 Redirect URI와 실제 콜백 URI가 불일치

**해결**:
```
각 Provider의 개발자 콘솔에서 Redirect URI 확인:
- Google: http://localhost:8080/login/oauth2/code/google
- GitHub: http://localhost:8080/login/oauth2/code/github
- Kakao: http://localhost:8080/login/oauth2/code/kakao

⚠️ 포트 번호, 프로토콜(http/https), 경로 모두 정확히 일치해야 함
```

<br>

### 문제 2: Kakao 이메일이 null로 반환됨

**원인**: 이메일 동의 항목이 설정되지 않음

**해결**:
1. Kakao Developers → 내 애플리케이션 → 제품 설정 → Kakao Login → 동의 항목
2. `카카오계정(이메일)` 항목을 **필수 동의** 또는 **선택 동의**로 설정
3. `application-dev.yml`의 `scope`에 `account_email` 추가:
```yaml
kakao:
  scope: [ profile_nickname, account_email ]
```

<br>

### 문제 3: 로컬에서는 되는데 배포 환경에서 실패

**원인**: 배포 환경의 도메인/포트가 다름

**해결**:
1. 각 Provider에 배포 환경의 Redirect URI 추가 등록
    - 예: `https://your-domain.com/login/oauth2/code/google`
2. `application-prod.yml`에서 `redirect-uri` 명시적 설정:
```yaml
kakao:
  redirect-uri: "https://your-domain.com/login/oauth2/code/kakao"
```

<br>

## 📚 참고 자료

- [Spring Security OAuth2 Client 공식 문서](https://docs.spring.io/spring-security/reference/servlet/oauth2/login/core.html)
- [Google OAuth2 설정 가이드](https://developers.google.com/identity/protocols/oauth2)
- [GitHub OAuth Apps 가이드](https://docs.github.com/en/developers/apps/building-oauth-apps)
- [Kakao Login REST API](https://developers.kakao.com/docs/latest/ko/kakaologin/rest-api)