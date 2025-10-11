package click.dailyfeed.member.domain.member.integratedtest;

import click.dailyfeed.code.domain.member.member.type.data.CountryCode;
import click.dailyfeed.code.domain.member.member.type.data.GenderType;
import click.dailyfeed.member.domain.authentication.dto.AuthenticationDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * POST /api/authentication/signup API를 호출하는 통합 테스트
 * CSV 파일의 데이터를 사용하여 회원 가입 API를 테스트합니다.
 *
 * Note: Production 에서는 사용하지 마세요.
 * 로컬에서 테스트를 위한 회원 데이터를 넣는 테스트 코드
 */
@ActiveProfiles({"local-k8s-test"})
@SpringBootTest
@AutoConfigureMockMvc
@ComponentScan(
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = "click.dailyfeed.feign..*"
        )
)
public class SignupApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Rollback(value = false)
    @Transactional
    @ParameterizedTest
    @CsvFileSource(resources = {"/csv/authentication/signup_request_ai.csv"}, numLinesToSkip = 1)
    @DisplayName("POST /api/authentication/signup - Quick test without validation")
    public void run_test(
        String email,
        String password,
        String memberName,
        String handle,
        String displayName,
        String bio,
        String location,
        String websiteUrl,
        String birthDate,
        String gender,
        String avatarUrl
    ) throws Exception {
        // Given - CSV 파라미터를 SignupRequest로 변환
        AuthenticationDto.SignupRequest signupRequest = toRequest(
                email, password, memberName, handle, displayName,
                bio, location, websiteUrl, birthDate, gender,
                avatarUrl);

        String requestBody = objectMapper.writeValueAsString(signupRequest);
        System.out.println("Testing signup for handle: " + signupRequest.getHandle());

        // When - POST /api/authentication/signup 호출
        mockMvc.perform(post("/api/authentication/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Rollback(value = false)
    @Transactional
    @ParameterizedTest
    @CsvFileSource(resources = {"/csv/authentication/signup_request_ai.csv"}, numLinesToSkip = 1)
    @DisplayName("POST /api/authentication/signup - AI 이미지 프로필을 사용한 회원가입 API 테스트")
    public void signup_api_test_with_ai_generated_image(
            String email,
            String password,
            String memberName,
            String handle,
            String displayName,
            String bio,
            String location,
            String websiteUrl,
            String birthDate,
            String gender,
            String avatarUrl
    ) throws Exception {
        // Given - CSV 파라미터를 SignupRequest로 변환
        AuthenticationDto.SignupRequest signupRequest = toRequest(
                email, password, memberName, handle, displayName,
                bio, location, websiteUrl, birthDate, gender,
                avatarUrl);

        String requestBody = objectMapper.writeValueAsString(signupRequest);

        System.out.println("Testing signup for handle: " + signupRequest.getHandle());

        // When & Then - POST /api/authentication/signup 호출 및 검증
        mockMvc.perform(post("/api/authentication/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.handle").value(handle))
                .andExpect(jsonPath("$.data.memberName").value(memberName))
                .andExpect(jsonPath("$.data.displayName").value(displayName))
                .andExpect(jsonPath("$.result.code").exists())
                .andExpect(jsonPath("$.result.message").exists());
    }

    /**
     * CSV 파라미터를 SignupRequest DTO로 변환하는 헬퍼 메서드
     */
    private AuthenticationDto.SignupRequest toRequest(
            String email,
            String password,
            String memberName,
            String handle,
            String displayName,
            String bio,
            String location,
            String websiteUrl,
            String birthDate,
            String gender,
            String avatarUrl
    ) {
        return AuthenticationDto.SignupRequest.builder()
                .email(email)
                .password(password)
                .memberName(memberName)
                .handle(handle)
                .displayName(displayName)
                .bio(bio)
                .location(location)
                .websiteUrl(websiteUrl)
                .birthDate(parseBirthDate(birthDate))
                .gender(GenderType.valueOf(gender))
                .avatarUrl(avatarUrl)
                .countryCode(CountryCode.KR)
                .languageCode("en")
                .isActive(true)
                .build();
    }

    /**
     * 생년월일 문자열을 LocalDate로 파싱하는 헬퍼 메서드
     * 다양한 날짜 형식을 처리합니다.
     */
    private LocalDate parseBirthDate(String birthDateStr) {
        if (birthDateStr == null || birthDateStr.trim().isEmpty()) {
            return null;
        }

        try {
            // "55.1.1." 같은 형식을 "55-1-1"로 변환
            String cleaned = birthDateStr.replace(".", "-").replaceAll("-+$", "");
            String[] parts = cleaned.split("-");

            if (parts.length >= 3) {
                int year = Integer.parseInt(parts[0]);
                // 2자리 연도를 4자리로 변환 (55 -> 1955, 25 -> 2025)
                if (year < 100) {
                    year = year < 30 ? 2000 + year : 1900 + year;
                }
                int month = Integer.parseInt(parts[1]);
                int day = Integer.parseInt(parts[2]);

                return LocalDate.of(year, month, day);
            }
        } catch (Exception e) {
            System.err.println("⚠️ 생년월일 파싱 실패: " + birthDateStr);
        }
        return null;
    }
}