# Hướng dẫn kiểm thử RecipeHub Enterprise — Windows + PowerShell

**Bắt buộc đọc:** README.md giải thích 8080 (Maven local) / 8075 (Docker app) và MySQL health check.

## Bước 1 — kiểm tra môi trường

```powershell
java -version
mvn -version
docker compose ps
```

Java phải là JDK 25. Nếu chạy local, bật MySQL bằng `docker compose up -d mysqldb`, nhưng không cần bật app Docker.

## Bước 2 — build và quality gates

```powershell
mvn clean verify
```

- **BUILD SUCCESS**: các unit/integration test, JaCoCo >=80% toàn bộ backend và từng Service class, PMD/Checkstyle/SpotBugs đều vượt qua.
- **BUILD FAILURE**: đừng bỏ qua. Gửi đoạn `[ERROR]`, `Failures:` hoặc `Caused by`.
- Kiểm tra `target/surefire-reports` cho kết quả JUnit/Mockito/MockMvc.
- Mở `target/site/jacoco/index.html` để xem coverage chính xác; nếu số % toàn bộ thấp hơn 80%, quality gate sẽ báo thất bại (cần bổ sung test).

## Bước 3 — khởi động backend

```powershell
mvn spring-boot:run
```

Mở: http://localhost:8080/ ; http://localhost:8080/swagger-ui.html ; http://localhost:8080/actuator/health

## Bước 4 — smoke test tự động

Mở cửa sổ PowerShell **khác**, đứng ở thư mục có `smoke-test.ps1`:

```powershell
powershell -ExecutionPolicy Bypass -File .\smoke-test.ps1
```

Khi Docker thay bằng `-BaseUrl http://localhost:8075`.

Nếu script báo `PASS`, bạn đã kiểm thử toàn bộ flow cơ bản trên MySQL thật.

## Bước 5 — Postman

1. Import collection `postman/RecipeHub.postman_collection.json`.
2. Import environment `postman/RecipeHub_Local.postman_environment.json` (hoặc Docker).
3. Chọn environment, chạy Collection Runner tuần tự các request 01–21.
4. Script Register tự lưu JWT ở collection variable `accessToken`; ID các bản ghi tự lưu để dùng sau.
5. Khi chạy lại, xóa collection variable `suffix` hoặc thay bằng một giá trị số mới.

## Các lỗi cần kiểm thử thủ công

| Test | Expected |
| --- | --- |
| POST category tên rỗng | 400 + thông báo Validation |
| POST category trùng | 409 |
| GET `/api/recipes/999999999` | 404 |
| POST recipe không có Bearer | 401 |
| POST recipe `categoryId` không tồn tại | 404 |
| POST recipe trùng `ingredientId` | 400 |
| PUT/DELETE recipe của user khác | 403 |
| Xóa Category hoặc Ingredient đang được Recipe dùng | 409 |
| POST Login sai mật khẩu | 401 |
| Truy cập `GET /api/users/me` với Bearer hợp lệ | 200, không có password |

## Cách gửi lỗi để sửa

Copy lệnh + console output chính xác (đặc biệt dòng `Caused by:`). Nếu test fail gửi `target/surefire-reports/*.txt` tương ứng. Nếu coverage fail gửi `target/site/jacoco/index.html` hoặc ảnh báo cáo và dòng JaCoCo check. Không gửi token hoặc mật khẩu.


## Comments + Admin (bản mới)

Sau khi `mvn clean verify` và khởi động lại Spring Boot, kiểm tra:

1. `GET /api/recipes/{id}/comments` không cần JWT -> 200.
2. `POST /api/recipes/{id}/comments` không JWT -> 401; có JWT, content rỗng -> 400.
3. User A tạo comment -> 201; User B PUT/DELETE comment đó -> 403; A PUT -> 200, DELETE -> 204.
4. User thường gọi `GET /api/admin/summary` -> 403; Admin được cấp DB role gọi -> 200.
5. Admin xóa comment hoặc Recipe khác tác giả -> 204; sau đó kiểm tra GET 404.
6. Chạy Postman Collection 27 requests (17a-17f là các request mới; chỉ dành cho User thường).

Cách cấp Admin an toàn cho demo nằm ở `COMMENTS_ADMIN_GUIDE.md`.
