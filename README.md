# AI VISION WORKS 2 — Android 앱

AI VISION WORKS 2 서버에 연결하는 안드로이드 설치형 앱(WebView)입니다.
- 첫 실행 때 서버 주소 입력 (사내 `http://192.168.0.56:8100` / 외부 `https://works.ucube.co.kr`)
- 파일 첨부·내려받기, 당겨서 새로고침, 뒤로가기, 인쇄(PDF 저장)
- 15분마다 새 메신저·메일 확인 → 휴대폰 알림

빌드: 이 브랜치에 push 하면 GitHub Actions 가 APK 를 만들어 Releases 에 올립니다.
직접 빌드: Android Studio 로 열거나 `gradle assembleDebug` (JDK 17, Android SDK 34).
정식 서명: 환경변수 AVW_KEYSTORE, AVW_KEYSTORE_PASSWORD, AVW_KEY_ALIAS, AVW_KEY_PASSWORD 를 설정하고 `gradle assembleRelease`.
