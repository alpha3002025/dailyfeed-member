package click.dailyfeed.member.domain.member.integratedtest;

import click.dailyfeed.code.domain.member.member.type.data.CountryCode;
import click.dailyfeed.code.domain.member.member.type.data.GenderType;
import click.dailyfeed.member.domain.authentication.dto.AuthenticationDto;
import click.dailyfeed.member.domain.authentication.service.AuthenticationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/// Note : Production 에서는 사용하지 마세요.
/// 로컬에서 테스트를 위한 회원 데이터를 넣는 테스트 코드
@ActiveProfiles({"local-test"})
@SpringBootTest
public class SignupInsertTest {
    @Autowired
    private AuthenticationService authenticationService;

    @Rollback(value = false)
    @Transactional
    @ParameterizedTest
    @CsvFileSource(resources = {"/csv/authentication/signup_request.csv"}, numLinesToSkip = 1)
    @DisplayName("회원가입_샘플데이터_insert(로컬테스트용 샘플데이터)")
    public void fixture__insert_sample_data(
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
    ){
        // When - CSV 파라미터를 SignupRequest로 변환
        AuthenticationDto.SignupRequest signupRequest = toRequest(
                email, password, memberName, handle, displayName,
                bio, location, websiteUrl, birthDate, gender,
                avatarUrl);

        System.out.println("handle = " + signupRequest.getHandle());

        authenticationService.signup(signupRequest);
    }


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
    ){
        AuthenticationDto.SignupRequest signupRequest = AuthenticationDto.SignupRequest.builder()
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

        return signupRequest;
    }

    private LocalDate parseBirthDate(String birthDateStr) {
        if (birthDateStr == null || birthDateStr.trim().isEmpty()) {
            return null;
        }

        try {
            String cleaned = birthDateStr.replace(".", "-").replaceAll("-+$", "");
            String[] parts = cleaned.split("-");

            if (parts.length >= 3) {
                int year = Integer.parseInt(parts[0]);
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
