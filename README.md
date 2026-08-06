# Via-bank
스트레스 DSR 기반 자산관리 플랫폼

## 실행 방법

1. DB 컨테이너 기동 (최초 기동 시 `db/init/*.sql`로 `via_sys` 스키마 자동 생성)
   ```bash
   docker compose up -d shinvia-mysql
   ```
2. 목서버(`Shinvia-mock`)를 `localhost:9090`에서 별도로 기동 (선택 - 카드 API 연동 테스트 시 필요)
3. 애플리케이션 실행
   ```bash
   ./gradlew bootRun
   ```
   기본 프로파일은 `local`이며, `mydata.mock.base-url=http://localhost:9090`, DB는 `jdbc:mysql://localhost:3309/via_sys`를 사용한다(`src/main/resources/application.yml`).

자세한 카드 동기화 구현/DB 스키마 설명은 [`docs/mydata-sync-implementation.md`](docs/mydata-sync-implementation.md) 참고.
