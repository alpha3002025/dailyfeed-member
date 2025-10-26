package click.dailyfeed.member.domain.member.integratedtest.k8s;

import click.dailyfeed.code.domain.member.member.type.data.CountryCode;
import click.dailyfeed.code.domain.member.member.type.data.GenderType;
import click.dailyfeed.member.domain.authentication.dto.AuthenticationDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Random;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 이미지를 업로드하고 해당 viewId를 사용하여 회원 가입을 수행하는 통합 테스트
 *
 * 1. CSV 파일을 @CsvFileSource로 읽어들임
 * 2. 각 row의 avatarUrl이 http://dailyfeed-image-svc:8080/api/images/view/로 시작하면
 *    sample_images 디렉토리에서 이미지를 하나씩 업로드
 * 3. 업로드 후 받은 viewId로 http://dailyfeed.local:8889/api/images/view/{viewId}를 avatarUrl로 설정
 * 4. 해당 row로 회원 가입 요청
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
public class ImageInsertAndSignupInsertLocalk8sTest {

    private static final String IMAGE_SERVICE_BASE_URL = "http://localhost:8889";
    private static final String IMAGE_UPLOAD_ENDPOINT = "/api/images/upload/profile";
    private static final String SAMPLE_IMAGES_DIR = "src/test/resources/sample_images";
    private static final String OLD_IMAGE_URL_PREFIX = "http://dailyfeed.local:8889";
    private static final String NEW_IMAGE_URL_PREFIX = "http://dailyfeed.local:8889/api/images/view/";
    private static final int MIN_IMAGE_NUMBER = 1;
    private static final int MAX_IMAGE_NUMBER = 47;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private final RestTemplate restTemplate = new RestTemplate();
    private final Random random = new Random();

    @Rollback(value = false)
    @Transactional
    @ParameterizedTest
    @CsvFileSource(resources = {"/csv/authentication/signup_request_ai_k8s.csv"}, numLinesToSkip = 1)
    @DisplayName("CSV 파일을 읽어 row마다 이미지 업로드 후 회원 가입 수행")
    public void signupWithImageUploadPerRow(
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
        // Upload image and replace avatarUrl if it matches the old image service URL pattern
        if (avatarUrl.startsWith(OLD_IMAGE_URL_PREFIX)) {
            // 1~47 범위에서 랜덤하게 이미지 번호 선택
            int randomImageNumber = MIN_IMAGE_NUMBER + random.nextInt(MAX_IMAGE_NUMBER - MIN_IMAGE_NUMBER + 1);
            String uploadedViewId = uploadRandomImage(randomImageNumber);
            if (uploadedViewId != null) {
                avatarUrl = NEW_IMAGE_URL_PREFIX + uploadedViewId;
                System.out.println("🔄 Replaced avatarUrl for " + handle + " with random image #" + randomImageNumber + " -> viewId: " + uploadedViewId);
            } else {
                System.err.println("⚠️  Failed to upload image for " + handle + ", keeping original URL");
            }
        }

        // Create signup request
        AuthenticationDto.SignupRequest signupRequest = toRequest(
                email, password, memberName, handle, displayName,
                bio, location, websiteUrl, birthDate, gender, avatarUrl
        );

        String requestBody = objectMapper.writeValueAsString(signupRequest);
        System.out.println("📝 Processing signup for: " + signupRequest.getHandle());

        // Perform signup
        mockMvc.perform(post("/api/authentication/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andDo(print())
                .andExpect(status().isOk());

        System.out.println("✅ Signup successful for: " + signupRequest.getHandle());
    }

    /**
     * 랜덤하게 선택된 이미지를 업로드하고 viewId를 반환하는 메서드
     */
    private String uploadRandomImage(int imageNumber) {
        String fileName = imageNumber + ".png";
        Path imagePath = Paths.get(SAMPLE_IMAGES_DIR, fileName);
        File imageFile = imagePath.toFile();

        if (!imageFile.exists()) {
            System.err.println("⚠️  Image file not found: " + imageFile.getAbsolutePath());
            return null;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("image", new FileSystemResource(imageFile));

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    IMAGE_SERVICE_BASE_URL + IMAGE_UPLOAD_ENDPOINT,
                    requestEntity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                // Parse the response to extract viewId
                String responseBody = response.getBody();
                JsonNode responseJson = objectMapper.readTree(responseBody);

                // Try different possible paths for viewId
                String viewId = null;

                // Try path: data (direct string)
                if (responseJson.has("data")) {
                    JsonNode dataNode = responseJson.get("data");
                    if (dataNode.isTextual()) {
                        // data is a string (the viewId itself)
                        viewId = dataNode.asText();
                    } else if (dataNode.isObject() && dataNode.has("viewId")) {
                        // data is an object with viewId field
                        viewId = dataNode.get("viewId").asText();
                    }
                }

                // Try path: viewId (direct)
                if ((viewId == null || viewId.isEmpty()) && responseJson.has("viewId")) {
                    viewId = responseJson.get("viewId").asText();
                }

                // Try path: result.viewId
                if ((viewId == null || viewId.isEmpty()) && responseJson.has("result")) {
                    JsonNode resultNode = responseJson.get("result");
                    if (resultNode.isObject() && resultNode.has("viewId")) {
                        viewId = resultNode.get("viewId").asText();
                    }
                }

                if (viewId != null && !viewId.isEmpty()) {
                    System.out.println("✅ Uploaded: " + fileName + " -> viewId: " + viewId);
                    return viewId;
                } else {
                    System.err.println("⚠️  No viewId found in response for: " + fileName);
                    System.err.println("Response: " + responseBody);
                    return null;
                }
            } else {
                System.err.println("❌ Failed to upload: " + fileName + " - Status: " + response.getStatusCode());
                return null;
            }
        } catch (Exception e) {
            System.err.println("❌ Exception while uploading: " + fileName);
            System.err.println("Error: " + e.getMessage());
            return null;
        }
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
                .gender(parseGender(gender))
                .avatarUrl(avatarUrl)
                .countryCode(CountryCode.KR)
                .languageCode("en")
                .isActive(true)
                .build();
    }

    /**
     * 생년월일 문자열을 LocalDate로 파싱하는 헬퍼 메서드
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
            System.err.println("⚠️  생년월일 파싱 실패: " + birthDateStr);
        }
        return null;
    }

    /**
     * Gender 문자열을 GenderType으로 파싱하는 헬퍼 메서드
     */
    private GenderType parseGender(String gender) {
        try {
            return GenderType.valueOf(gender);
        } catch (Exception e) {
            System.err.println("⚠️  Gender 파싱 실패: " + gender);
            return GenderType.PREFER_NOT_TO_SAY;
        }
    }
}