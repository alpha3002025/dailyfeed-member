# authentication 테스트 명세

## 실행 Profile
`src/test/docs/reference/environment/test-profile.md` 파일을 확인하세요.<br/>
<br/>

## 주요 테스트 feature
> 자세한 내용은 하단 개별 섹션들에 구체적인 내용을 참고하세요.

- login
- refresh
- signup

<br/>

### login
테스트 구현 완료
- LoginBehaviorVerifyLocalWASTest.java

구현 예정
- 추가예정

<br/>

### refresh
테스트 구현 완료
- refresh/localwas/tokenservice/RefreshBehaviorVerifyLocalWASTest.java

구현 예정
- 추가예정

<br/>

### signup
테스트 구현 완료<br/>
local-was Profile
- signup/localwas/SignupInsertLocalWASTest.java

local-k8s Profile<br/>
- signup/localk8s/ImageInsertAndSignupInsertLocalK8sTest.java

dev-k8s Profile<br/>
- signup/devk8s/ImageInsertAndSignupInsertDevK8sTest.java

<br/>



