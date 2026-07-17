# MyApp (Android/Kotlin)

## 1. Setup lần đầu (chỉ làm 1 lần)

### a) Mở project trong Android Studio
Mở thư mục này bằng Android Studio — Android Studio sẽ tự sinh `gradlew`, `gradlew.bat`
và `gradle/wrapper/gradle-wrapper.jar` (3 file này chưa có sẵn vì môi trường tạo
project không có mạng). Sau khi mở, chờ Gradle sync xong là đủ.

Hoặc nếu có Gradle cài sẵn trên máy, chạy:
```
gradle wrapper --gradle-version 8.9
```

### b) Tạo keystore để ký APK release
```bash
keytool -genkeypair -v \
  -keystore release.keystore \
  -alias myapp \
  -keyalg RSA -keysize 2048 -validity 10000
```
Nó sẽ hỏi password kho khóa (store password), password của key, và vài thông tin
(tên, tổ chức...). **Lưu 3 giá trị: đường dẫn file, store password, key password, alias.**
Giữ file `release.keystore` an toàn — mất là không update lại app cùng chữ ký được nữa.

### c) Add secrets vào GitHub repo
Vào repo → Settings → Secrets and variables → Actions → New repository secret,
tạo 4 secrets:

| Secret name         | Giá trị                                              |
|----------------------|------------------------------------------------------|
| `KEYSTORE_BASE64`    | `base64 -i release.keystore | tr -d '\n'` (paste kết quả) |
| `KEYSTORE_PASSWORD`  | store password lúc tạo keystore                       |
| `KEY_ALIAS`          | alias (vd `myapp`)                                    |
| `KEY_PASSWORD`       | key password lúc tạo keystore                         |

## 2. Build & release APK

Chỉ cần push một tag đúng định dạng `vX.Y.Z`:
```bash
git tag v1.0.0
git push origin v1.0.0
```
GitHub Actions (`.github/workflows/release.yml`) sẽ tự:
1. Build APK release đã ký (`assembleRelease`)
2. Tạo GitHub Release ứng với tag đó
3. Đính kèm file `MyApp-v1.0.0.apk` vào Release

Cũng có thể chạy tay: tab **Actions** → chọn workflow **Build & Release APK** →
**Run workflow** (không tạo Release, chỉ ra artifact để tải).

## 3. Cấu trúc chính
```
app/
  build.gradle.kts          # config build + signing (đọc từ env var)
  src/main/java/.../MainActivity.kt
  src/main/res/...
.github/workflows/release.yml
```

## 4. Đổi tên package / app
- `applicationId` và `namespace` trong `app/build.gradle.kts`
- Di chuyển thư mục `com/thuanh/app` cho khớp package mới
- `app_name` trong `res/values/strings.xml`
