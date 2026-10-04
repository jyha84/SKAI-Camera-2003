# 개발 구조

패키지: `com.skai.camera2003` / 현재 버전: `0.10.0` / versionCode: `10`.

| 파일 | 역할 |
| --- | --- |
| MainActivity.kt | 권한, LCD 화면, 메뉴, 키패드 조작, 스킨 선택 저장 |
| RetroCamera.kt | CameraX 분석·촬영, 촬영 설정, JPEG 저장 |
| RetroImage.kt | 저해상도 처리, 사진 색감, 날짜 인쇄 |
| PhoneSkin.kt | 스킨 이미지, LCD 위치·색상, 물리 버튼 터치 좌표 |
| PhotoRepository.kt | 사진 목록 조회·디코딩, 공유 Intent |
| PhotoAlbum.kt | 사진첩과 사진 보기 |
| LcdTypography.kt | 갈무리 도트 폰트 |

소스 위치: `app/src/main/java/com/skai/camera2003/`.

## 사진 스킨 수정

본체 이미지는 `app/src/main/res/drawable-nodpi/phone_*.png`입니다.
화면과 버튼 좌표는 이미지 가로·세로를 각각 1로 보는 정규화 좌표를 사용합니다.
이미지를 바꾸면 `PhoneSkin.kt`의 LCD 영역과 버튼 중심·크기를 함께 확인해야 합니다.
스타택은 작은 LCD에 맞춘 전용 메뉴·사진첩 표시를 사용합니다.

스킨의 초록색 이미지 효과는 Compose의 표시용 ColorFilter이며, 저장 JPEG 처리와 분리되어 있습니다.

## 권한과 저장

카메라 권한을 실행 시 요청합니다.
Android 9 이하에서 외부 저장소 읽기·쓰기가 필요한 동작에는 해당 권한을 요청합니다.
Android 10 이상에서는 앱 사진 폴더를 MediaStore로 조회합니다.
외부 사진은 시스템 선택기를 이용하며 공유는 읽기 권한을 부여한 URI로 처리합니다.

## 검증

```sh
./gradlew assembleDebug lintDebug testDebugUnitTest
```

테스트는 RetroImageTest와 PhotoRepositoryTest에 있습니다.
클라우드 검증은 빌드·Lint·기능 테스트를 포함하며 실제 카메라와 터치 배치 확인을 대신하지 않습니다.

기기별 SDK 경로는 local.properties로 설정하고 Git에는 포함하지 않습니다.
사진과 폰트 변경 시 출처·라이선스 문서와 앱 assets의 고지도 함께 갱신합니다.
