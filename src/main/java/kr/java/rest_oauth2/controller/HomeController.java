package kr.java.rest_oauth2.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * [설명]
 * - SSR의 진입점(Thymeleaf 템플릿 렌더링)
 * - 인증 정보는 템플릿에서 직접 주입하지 않고, /api/me를 호출하여 표시한다.
 */
@Controller
public class HomeController {

    /**
     * [설명]
     * - 홈 화면 렌더링
     */
    @GetMapping("/")
    public String home() {
        return "home";
    }
}
