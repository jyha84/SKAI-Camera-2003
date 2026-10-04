# SKAI Camera 2003

2000년대 디지털카메라와 피처폰의 저해상도 사진을 재현하는 Android 토이 카메라입니다.
Kotlin · Jetpack Compose · CameraX로 만들었으며 현재 버전은 **0.10.0**입니다.

## 기능

- 카메라 권한 요청 및 후면 카메라 실시간 프리뷰
- 저해상도 JPEG 촬영: VGA 480×640 / QVGA 240×320
- 사진 느낌: 피처폰 2003 / 디카 2005
- 사진 안에 날짜 인쇄, 표시 켜기·끄기
- 갤러리의 `Pictures/SKAI 2003` 폴더에 저장
- 앱 안의 사진첩, 외부 사진 열기, 원본 사진 공유
- 실버 2003 / 검정 스타택 / 남색 노키아 3310 스킨
- 갈무리 도트 폰트, 키 설명 토글, 마지막 스킨 선택 저장

스타택·노키아의 초록 LCD는 화면 표시 효과입니다. 저장·공유되는 JPEG에는 촬영 설정의 색감이 적용됩니다.
공개 휴대폰 사진을 변형한 SKAI 스킨이며 출처와 라이선스를 함께 제공합니다.

## 설치

현재 설치 파일은 `SKAI-Camera-2003-v0.10-StarTAC.apk`입니다. Android 6.0(API 23) 이상에서 설치할 수 있습니다.
기존 클라우드 빌드와 같은 서명이므로 이전 SKAI APK 위에 업데이트 설치할 수 있습니다.

[GitHub 저장소](https://github.com/jyha84/SKAI-Camera-2003) · [최신 APK Release](https://github.com/jyha84/SKAI-Camera-2003/releases/latest)

소스는 Public으로 공개하며 설치 APK와 SHA256 파일은 Release에서 제공합니다.

## 조작

| 키 | 기능 |
| --- | --- |
| 0 | 메뉴 열기 |
| 1 | 날짜 표시 변경 |
| 2 | 카메라에서 크기 변경, 메뉴·사진 화면에서 위로 이동 |
| 3 | 사진 느낌 변경 |
| 4 / 6 | 사진 이동 또는 카메라 색감 변경 |
| 5 / OK | 촬영·선택·사진 열기 |
| 7 | 사진첩 |
| 8 | 아래로 이동 |
| 9 | 외부 사진 찾기 |
| 로고 옆 ? | 키 설명 표시 켜기·끄기 |

`메뉴 → 4. 스킨`에서 스킨을 선택합니다. 선택 상태는 재실행 후에도 유지됩니다.

스타택에는 긴 덮개, 작은 가로 LCD, 촘촘한 숫자 키와 두 줄의 기능 키를 적용했습니다.
작은 LCD에서 메뉴는 한 항목씩, 사진첩은 한 장씩 표시합니다.
스타택의 MENU는 이전·메뉴, MR은 사진·공유, M+는 아래, ↑는 위, OK는 촬영·선택입니다.

## 빌드

Android Studio, JDK 17, Android SDK 35가 필요합니다. Gradle Wrapper가 포함되어 있습니다.

```sh
./gradlew assembleDebug lintDebug testDebugUnitTest
```

Windows에서는 `gradlew.bat`을 사용합니다.
APK 출력: `app/build/outputs/apk/debug/app-debug.apk`.

`.github/workflows/android.yml`에는 GitHub Actions 빌드·Lint·기능 테스트 및 APK Artifact 업로드를 준비했습니다.
[Actions](https://github.com/jyha84/SKAI-Camera-2003/actions)에서 실행 결과를 확인할 수 있습니다. 개발 환경마다 디버그 서명은 달라질 수 있습니다.

## 공개 배포 안내

이 앱은 SKY·Samsung·Motorola·Nokia와 제휴하거나 승인받은 공식 제품이 아닌 개인 토이 프로젝트입니다.
사진 사용 라이선스는 브랜드 상표나 제품 디자인에 관한 별도의 이용 허락을 의미하지 않습니다.
사진·폰트의 출처와 라이선스 고지를 유지해야 하며, 현재 앱 코드에는 별도 오픈소스 라이선스를 지정하지 않았습니다.
Public 공개가 자유로운 코드 재사용 허락을 뜻하지는 않습니다.
자세한 내용은 [공개 배포와 권리 안내](RIGHTS.md)를 참고하세요.

## 검증 및 문서

클라우드 빌드와 서명 검증 통과, Lint 오류 0개·경고 16개, 기능 테스트 8개 통과.
새 스킨 배치와 터치 동작은 실제 기기에서 추가 확인이 필요합니다.

- [전체 코드](FULL_CODE.md)
- [빌드 검토](BUILD_REVIEW.md)
- [개발 구조](DEVELOPMENT.md)
- [GitHub 업로드 안내](GITHUB_UPLOAD.md)
- [공개 배포와 권리 안내](RIGHTS.md)
- [사진 스킨 출처·라이선스](ASSET_LICENSE.txt)
- [갈무리 폰트 라이선스](FONT_LICENSE.md)

사진 스킨은 각 CC 라이선스, 갈무리 폰트는 SIL OFL 1.1을 따릅니다.
앱 코드에는 별도 오픈소스 라이선스를 지정하지 않았습니다.
`TEST_RESULTS_v0.5.xml`은 이전 버전 테스트 기록입니다.
