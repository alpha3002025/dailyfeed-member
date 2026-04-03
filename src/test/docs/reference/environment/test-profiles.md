## 테스트 실행환경
- `local-test` Profile : 테스트 클래스 명에 `LocalWAS` 단어가 포함된 경우 `local-test` 프로필을 따릅니다.
    - local PC에 Jar 파일을 이용해 tomcat 을 띄우는 방식입니다.
    - MySQL, MongoDB, Kafka 모두 docker container 기반으로 개발환경에 띄우는 방식입니다.
- `local-k8s` Profile : 테스트 클래스 명에 `LocalK8s` 단어가 포함된 경우 `local-k8s-test` 프로필을 따릅니다.
    - 로컬 개발 PC 에서 MySQL, MongoDB, Kafka 를 docker container 로 띄운 환경에 대한 프로필입니다.
- `dev-k8s` Profile : 테스트 클래스 명에 `DevK8s` 단어가 포함된 경우 `dev-k8s-test` 프로필을 따릅니다.
    - MySQL, MongoDB 를 클라우드 환경에 띄운 경우에 대한 프로필입니다.
    - 이 외의 모든 infra 는 현재 docker container 로 띄운 테스트 환경입니다.
    - 추후 금전적인 여건이 된다면 Kafka, Redis 역시 클라우드 환경에 test 를 위한 인스턴스를 구비 후 해당 내용을 구비할 예정입니다.

```java
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles({"local-test"}) // local-test 프로필
@ActiveProfiles({"local-k8s-test"}) // local-k8s 프로필
@ActiveProfiles({"dev-k8s-test"}) // dev-k8s-test 프로필
```