# v0.10 스타택 검토

2026-10-04 UTC

스타택 원본 공개 사진으로 스킨 재작성: 긴 덮개, 작은 가로 LCD, 촘촘한 키패드.
스타택 LCD 및 숫자 키와 6개 기능 키 좌표를 새 이미지에 맞춤.
스타택 MENU/MR/M+/전원/↑/OK를 기존 기능에 연결.
작은 LCD 전용 메뉴 한 항목 표시, 사진첩 한 장 표시, 상태 한 줄 표시.
스타택에서 사진첩 위/아래 이동은 한 장 단위. 읽기 권한 OK 처리 포함.
메뉴 → 스킨: 직접 터치 또는 2/8 이동, OK/5 선택.
SharedPreferences에 안정적인 스킨 ID 저장, 잘못된 값은 실버로 복구.
초록 LCD는 Compose 이미지 표시용 ColorFilter이며 JPEG 저장·공유 처리는 유지.
스킨 사진·폰트 출처 및 라이선스 포함.

assembleDebug / lintDebug: BUILD SUCCESSFUL.
Lint 0 errors / 16 warnings.
APK 서명 검증 통과. v0.9과 인증서 동일. versionCode 10.
이번 UI 변경에서 기능 테스트를 추가하거나 반복하지 않음.
실제 기기의 스킨 배치·터치 동작은 미검증.

APK: SKAI-Camera-2003-v0.10-StarTAC.apk
크기: 19795842 bytes
SHA256: 67726316ff5ae8c876969a38186ed48d114be07563f5194058536a8817811943

GitHub 업로드 준비 검증 (2026-10-04):
testDebugUnitTest BUILD SUCCESSFUL. 기존 기능 테스트 8개 통과, 실패·오류 0개.
GitHub Actions에는 assembleDebug / lintDebug / testDebugUnitTest 및 APK Artifact 업로드를 설정.
