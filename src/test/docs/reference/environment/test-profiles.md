## 주요 테스트 Profile 들
```java
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles({"local-test"}) // local-test 프로필
@ActiveProfiles({"local-k8s-test"}) // local-k8s 프로필
@ActiveProfiles({"dev-k8s-test"}) // dev-k8s-test 프로필
```
<br/>

## 테스트 실행환경
- `local-test` : 주요 Infra (MySQL, MongoDB, Kafka, Redis) 를 docker-compose 기반으로 Docker container 로 실행, WAS 는 Local PC 에 Jar 파일로 구동
  - 인프라 실행 방식은 아래의 'infra - local-test' 섹션에 정리해두었습니다.
  - 테스트 클래스 명에 'LocalWAS' 라는 단어가 포함된 경우 `local-test` 프로필을 따르는 테스트 케이스로 동작하도록 합니다.
- `local-k8s` : 주요 Infra (MySQL, MongoDB, Kafka, Redis) 는 docker-compose 기반으로 구동하고 애플리케이션은 컨테이너 기반으로 k8s 내에서 helm 을 통해 구동하며 k8s는 kind k8s 로 구동한 환경에 대한 환경들의 접속정보를 정의한 profile 입니다.
  - 인프라 실행 방식은 아래의 'infra - local-k8s' 섹션에 정리해두었습니다.
  - 테스트 클래스 명에 `LocalK8s` 단어가 포함된 경우 `local-k8s-test` 프로필을 따르는 테스트 케이스로 동작하도록 합니다.
- `dev-k8s` : 주요 Infra (MySQL, MongoDB) 는 클라우드 플랫폼을 사용하고, Kafka, Redis 는 docker-compose 기반의 환경으로 구성하고 애플리케이션은 helm 기반으로 동작하며, k8s 는 kind k8s 로 구동한 환경에 대한 환경들의 접속정보를 정의한 profile 입니다. 
  - 인프라 실행방식은 아래의 'infra - dev-k8s' 섹션에 정리해두었습니다.
  - 테스트 클래스 명에 `DevK8s` 단어가 포함된 경우 `dev-k8s-test` 프로필을 따르는 테스트 케이스로 동작하도록 합니다.

<br/>

## 환경 setup
### infra - local-test
dailyfeed-installer 프로젝트가 없다면 clone 
```bash
git clone --recurse-submodules http://github.com/alpha3002025/dailyfeed-installer
cd dailyfeed-installer
git submodule foreach 'git checkout main && git pull origin main'
```
<br/>

docker-compose 기반의 infra (MySQL, Kafka, Redis 등)를 설치
```bash
## dailyfeed-installer 프로젝트 디렉터리로 이동
cd dailyfeed-installer/dailyfeed-infrastructure
## local-was 프로필 디렉터리로 이동
cd docker/local-was
## docker compose
docker-compose up -d
```
<br/>

### infra - local-k8s
dailyfeed-installer 프로젝트가 없다면 clone
```bash
git clone --recurse-submodules http://github.com/alpha3002025/dailyfeed-installer
cd dailyfeed-installer
git submodule foreach 'git checkout main && git pull origin main'
```
<br/>

docker-compose 기반의 infra (MySQL, Kafka, Redis 등)를 설치
```bash
## dailyfeed-installer 프로젝트 디렉터리로 이동
cd dailyfeed-installer/dailyfeed-infrastructure
## local-hybrid 프로필 디렉터리로 이동
cd docker/local-hybrid
## docker compose
docker-compose up -d
```
<br/>

### infra - dev-k8s
dailyfeed-installer 프로젝트가 없다면 clone
```bash
git clone --recurse-submodules http://github.com/alpha3002025/dailyfeed-installer
cd dailyfeed-installer
git submodule foreach 'git checkout main && git pull origin main'
```
<br/>

docker-compose 기반의 infra (MySQL, Kafka, Redis 등)를 설치
```bash
## dailyfeed-installer 프로젝트 디렉터리로 이동
cd dailyfeed-installer/dailyfeed-infrastructure
## dev 프로필 디렉터리로 이동
cd docker/dev
## docker compose
docker-compose up -d
```
<br/>
