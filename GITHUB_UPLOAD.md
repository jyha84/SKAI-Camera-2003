# GitHub 업로드 준비

최신 소스, 사진 스킨, 폰트와 라이선스, Gradle Wrapper, GitHub Actions 설정을 준비했습니다.
대상 계정은 `jyha84`, 저장소 이름은 `SKAI-Camera-2003`입니다.

## 현재 막힌 단계

연결된 GitHub 앱으로 저장소 생성을 시도했으나 GitHub가 HTTP 403
`Resource not accessible by integration`으로 거절했습니다.
현재 연결 권한으로는 새 저장소를 생성할 수 없습니다.

## 저장소 만들기

1. [GitHub 새 저장소](https://github.com/new?name=SKAI-Camera-2003)를 엽니다.
2. 이름을 `SKAI-Camera-2003`으로 설정합니다. 공개하려면 Public, 비공개로 보관하려면 Private를 선택합니다. Public 공개 시 README와 RIGHTS.md의 권리 안내를 함께 포함합니다.
3. README·.gitignore·라이선스 자동 추가는 체크하지 않습니다.
4. Create repository를 누르고 생성된 주소를 이 대화에 전달합니다.

저장소가 만들어진 뒤 연결 앱의 접근 권한이 있으면 소스 업로드를 이어갈 수 있습니다.

## 업로드 파일

- 프로젝트 소스와 Gradle 설정
- 실버·스타택·노키아 스킨, 갈무리 폰트
- README.md / DEVELOPMENT.md / FULL_CODE.md / BUILD_REVIEW.md / RIGHTS.md
- 사진·폰트 출처와 라이선스
- GitHub Actions 빌드 설정

빌드 캐시, 로컬 SDK 경로, 서명 키, 인증 정보는 소스 업로드에서 제외합니다.
APK는 소스 Git 커밋 대신 Release의 설치 파일로 첨부할 예정입니다.

## APK Release

버전 태그: `v0.10.0`.
설치 파일: `SKAI-Camera-2003-v0.10-StarTAC.apk`.
검증용 파일: `SKAI-Camera-2003-v0.10-StarTAC.apk.sha256`.
현재 Release 생성과 APK 업로드는 아직 완료되지 않았습니다.
