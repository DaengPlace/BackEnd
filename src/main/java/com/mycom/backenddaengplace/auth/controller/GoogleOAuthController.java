package com.mycom.backenddaengplace.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycom.backenddaengplace.auth.jwt.JWTUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/oauth2/google")
public class GoogleOAuthController {

    private final JWTUtil jwtUtil;
    private final RestTemplate restTemplate;

    // 환경 변수에서 Google Client ID와 Secret 가져오기
    @Value("${GOOGLE_CLIENT_ID}")
    private String googleClientId;

    @Value("${GOOGLE_CLIENT_SECRET}")
    private String googleClientSecret;

    public GoogleOAuthController(JWTUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
        this.restTemplate = new RestTemplate();
    }

    @GetMapping("/callback")
    public ResponseEntity<?> handleGoogleCallback(@RequestParam String code) throws IOException {
        // Google로부터 액세스 토큰 요청
        String tokenUrl = "https://oauth2.googleapis.com/token";
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("grant_type", "authorization_code");
        parameters.add("client_id", googleClientId); // 환경 변수에서 가져온 값 사용
        parameters.add("client_secret", googleClientSecret); // 환경 변수에서 가져온 값 사용
        parameters.add("redirect_uri", "https://api.daengplace.com/login/oauth2/code/google");
        parameters.add("code", code);

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(parameters, new HttpHeaders());
        ResponseEntity<String> response = restTemplate.postForEntity(tokenUrl, requestEntity, String.class);
        Map<String, Object> responseBody = new ObjectMapper().readValue(response.getBody(), Map.class);

        String accessToken = (String) responseBody.get("access_token");

        // 사용자 정보 요청
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        HttpEntity<Void> userInfoRequest = new HttpEntity<>(headers);
        ResponseEntity<Map> userInfoResponse = restTemplate.exchange("https://www.googleapis.com/oauth2/v1/userinfo?alt=json", HttpMethod.GET, userInfoRequest, Map.class);

        Map<String, Object> userInfo = userInfoResponse.getBody();
        String username = (String) userInfo.get("id");

        // JWT 토큰 생성
        String jwtToken = jwtUtil.createJwt("access", username, "ROLE_USER", 600000L);

        // 프론트엔드에 JWT 토큰 전달
        return ResponseEntity.ok(Map.of("accessToken", jwtToken));
    }
}