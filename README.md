## Trạng thái dự án

**RecipeHub Enterprise — Portfolio Project (MVP)**

- Java 25, Spring Boot 4.1.1, MySQL 8, Spring Security và JWT.
- Hoàn thành REST API cho Recipe, Category, Ingredient, Comments và quản lý tài khoản.
- Phân quyền Guest / User / Admin.
- Admin Dashboard, kiểm duyệt nội dung và khóa/mở khóa tài khoản.
- JUnit, Mockito, MockMvc, JaCoCo, PMD, Checkstyle và SpotBugs.
- **Maven Build:** SUCCESS.
- **GitHub Actions CI:** SUCCESS, chạy tự động trên Java 25.
- **Coverage Quality Gate:** tối thiểu 80%.
- Đã kiểm thử API bằng Postman và Smoke Test trên Docker.
- Đã kiểm thử thành công 8/8 tình huống quản lý trạng thái tài khoản.

**Lưu ý:** Dự án phục vụ học tập và portfolio, chưa phải hệ thống được cấu hình để triển khai production công khai.

## Nâng cấp Comments & Admin (02/11/2026)

- Guest xem bình luận; User viết/sửa/xóa bình luận của mình; Admin kiểm duyệt.
- Admin Dashboard `/admin.html` có thống kê và quản lý nội dung; role `ADMIN` do người quản trị cấp trong DB, không đăng ký công khai.
- Mã nguồn **đã có frontend nhiều trang** (`index.html`, `recipe.html`, `editor.html`, `login.html`, `register.html`, `admin.html`).
- **Xem `COMMENTS_ADMIN_GUIDE.md`** để cấp Admin và kiểm thử những route mới.
- Postman Collection 27 requests, bổ sung Comments API và test chặn truy cập Admin đối với User thường.
- Sau khi bổ sung module cần chạy lại `mvn clean verify` và đo JaCoCo; chưa có kết quả thực tế của phiên bản nâng cấp.

## Chức năng đã viết

- Category CRUD; Ingredient CRUD (name, description).
- Recipe CRUD, công thức có Category và User tác giả, danh sách nguyên liệu (quantity, unit).
- Tìm kiếm Recipe bằng `keyword`, lọc `categoryId`, xem danh sách và chi tiết.
- Đăng ký, đăng nhập, BCrypt hash, trả JWT; `/api/users/me` (không lộ mật khẩu).
- GET API public; ghi Category/Ingredient/Recipe yêu cầu JWT; sửa/xóa Recipe chỉ chủ sở hữu.
- HTTP 400 (validation/bad JSON), 401 (auth), 403 (không phải tác giả), 404, 409 (duplicate/integrity) dạng JSON.
- Giao diện demo public tại `/` gồm tìm kiếm, đăng nhập, tạo Category/Ingredient/Recipe, xem chi tiết, sửa/xóa công thức của tác giả.
- Swagger tại `/swagger-ui.html`, OpenAPI ở `/v3/api-docs`.
- Unit tests + MockMvc/H2 integration tests, JaCoCo whole-project and Service coverage gates 80%, PMD/Checkstyle/SpotBugs gates và GitHub Actions.
- Postman Collection 27 requests và script smoke test Comments cho Windows PowerShell.

## 1. Chạy local trong VS Code (Windows)

**Yêu cầu:** JDK 25, Maven 3.9.x, Docker Desktop. Mở thư mục chứa `pom.xml`.

```powershell
java -version
mvn -version
# Lần đầu chạy, khởi động MySQL:
docker compose up -d mysqldb
# Xem container đã healthy:
docker compose ps
# Build và kiểm tra chất lượng toàn bộ:
mvn clean verify
# Chạy backend (chỉ port 8080):
mvn spring-boot:run
```

Nếu bạn đã có DB cũ, `spring.jpa.hibernate.ddl-auto=update` **chỉ dùng để phát triển**; trước khi chạy trên dữ liệu quan trọng nên sao lưu MySQL và dùng migration Flyway/Liquibase. Bản MVP không thực hiện migration dữ liệu người dùng có sẵn.

Mở các URL:

| URL | Nội dung |
| --- | --- |
| http://localhost:8080/ | Giao diện RecipeHub |
| http://localhost:8080/swagger-ui.html | Swagger |
| http://localhost:8080/v3/api-docs | OpenAPI JSON |
| http://localhost:8080/actuator/health | Health check |

**Lưu ý:** Khi chạy bằng `mvn spring-boot:run` bạn không cần bật container `app`. Docker MySQL (`mysqldb`) phải hoạt động. Không dùng đồng thời hai app để tránh nhầm cổng.

## 2. Chạy toàn bộ bằng Docker

Copy `.env.example` thành `.env`, thay `JWT_SECRET` bằng secret ngẫu nhiên mạnh từ PowerShell:

```powershell
$bytes = New-Object byte[] 48
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
$rng.Dispose()
[Convert]::ToBase64String($bytes)
```

Dán chuỗi nhận được vào `JWT_SECRET=...` trong `.env` (không commit `.env` lên GitHub). Sau đó:

```powershell
docker compose up --build -d
docker compose ps
docker compose logs -f app
```

- Docker frontend/backend: http://localhost:8075/
- Docker Swagger: http://localhost:8075/swagger-ui.html
- Không sửa port 8075: container chạy port 8080 nhưng publish ra host 8075.

**Cảnh báo:** `MYSQL_ROOT_PASSWORD=123456` và `JWT_SECRET` mặc định trong `application.properties` chỉ là thiết lập tiện lợi cho môi trường **phát triển**. Triển khai thực tế phải cung cấp mật khẩu mạnh, JWT secret riêng, HTTPS, database user không phải root, migration, giám sát và quy trình bảo mật bổ sung. Khi đổi JWT_SECRET, token cũ hết hiệu lực.

## 3. Quy trình Quality Gate (bắt buộc)

```powershell
mvn clean compile    # Biên dịch
mvn test             # JUnit 5/Mockito/MockMvc/H2
mvn clean verify     # Tests + JaCoCo >=80% toàn bộ Java backend và từng class Service + PMD + Checkstyle + SpotBugs
```

### Coverage JaCoCo

- HTML report: `target/site/jacoco/index.html`.
- Data report: `target/site/jacoco/jacoco.xml` để tích hợp SonarQube/CI.
- Rule Maven JaCoCo: **toàn bộ backend (BUNDLE) phải đạt LINE coverage >= 0.80**, đồng thời mỗi class trong `recipehub.*.service.*` phải >=80%. Con số thực tế chỉ biết khi chạy `mvn clean verify`.
- Khi report chỉ ra các class thiếu coverage, bổ sung unit test/MockMvc để vượt qua quality gate, không hạ ngưỡng nếu mục tiêu vẫn là 80%.
- `mvn clean verify` trả BUILD FAILURE khi rule coverage, PMD hoặc Checkstyle không đạt. Không dùng `-DskipTests` để báo cáo coverage.

### Static Code Analysis

- **Checkstyle**: `checkstyle.xml`, kiểm tra tab, empty statement, finalizer, import.
- **PMD**: `pmd-ruleset.xml`, các quy tắc error-prone; chạy tự động trong `verify`.
- **SpotBugs**: quét lỗi bytecode có mức độ nghiêm trọng cao.
- **GitHub Actions**: `.github/workflows/ci.yml` chạy `mvn clean verify` khi push/PR.
- Các công cụ hiện là **quality gates được thiết lập**, chưa có kết quả chạy thành công từ máy này.

### Unit Test và Integration Test

| File test | Phạm vi |
| --- | --- |
| `CategoryServiceTest` | find/list/create/update/delete, 404 và trùng tên |
| `IngredientServiceTest` | đầy đủ CRUD và các nhánh lỗi |
| `AuthServiceTest` | hash password, đăng ký, trùng username/email, login lỗi |
| `RecipeServiceTest` | recipe CRUD, tìm/lọc, trùng/mất nguyên liệu, quyền tác giả |
| `TokenServiceTest` | JWT ký HS256, subject, hết hạn |
| `RecipeHubApiIntegrationTest` | MockMvc + H2, JWT thật, flow category → ingredient → recipe |
| `RecipeHubApiErrorIntegrationTest` | bad JSON, 404, 409, 401 và login |
| `RecipeHubOwnershipIntegrationTest` | MockMvc: 403 khi sửa/xóa Recipe người khác, invalid Bearer token |
| `GlobalExceptionHandlerTest` và `EntityLifecycleTest` | Chuẩn lỗi và JPA entity lifecycle |

**Integration test tự động dùng H2 in-memory** với cấu hình `src/test/resources/application.properties`, không sửa MySQL thật.

## 4. Kiểm thử API trên máy thật

```powershell
# Khi ứng dụng đang chạy tại 8080:
.\smoke-test.ps1
# Khi chạy Docker:
.\smoke-test.ps1 -BaseUrl http://localhost:8075
```

Smoke test tạo User mới, dùng JWT, tạo Category → Ingredient → Recipe, GET, PUT, DELETE rồi dọn dữ liệu do script tạo. Thất bại sẽ in HTTP response để bạn gửi lỗi.

Postman: Import `postman/RecipeHub.postman_collection.json` và `postman/RecipeHub_Local.postman_environment.json` (hoặc `Docker`). Vào Collection Runner, chạy **theo thứ tự, bao gồm các request 17a–17f cho Comments**. Script tự lưu token/IDs. Mỗi lượt chạy mới, xóa giá trị biến collection `suffix` để tránh duplicate dữ liệu lượt trước. Xem thêm `TESTING_GUIDE.md`.

## 5. API chính

| Method | Route | Access |
| --- | --- | --- |
| POST | `/api/auth/register` | Public |
| POST | `/api/auth/login` | Public |
| GET | `/api/users/me` | JWT |
| GET | `/api/categories`, `/api/categories/{id}` | Public |
| POST/PUT/DELETE | `/api/categories`, `/api/categories/{id}` | JWT |
| GET | `/api/ingredients`, `/api/ingredients/{id}` | Public |
| POST/PUT/DELETE | `/api/ingredients`, `/api/ingredients/{id}` | JWT |
| GET | `/api/recipes`, `/api/recipes/{id}` | Public |
| GET | `/api/recipes?keyword=ga&categoryId=1` | Public |
| POST/PUT/DELETE | `/api/recipes`, `/api/recipes/{id}` | JWT, owner for PUT/DELETE |

Ví dụ POST Recipe (khi đã đăng nhập):

```json
{
  "name": "Gà chiên nước mắm",
  "description": "Món ăn gia đình",
  "cookingTime": 30,
  "servings": 2,
  "instructions": "Ướp và chiên gà",
  "imageUrl": "",
  "categoryId": 1,
  "ingredients": [{"ingredientId": 1, "quantity": 500, "unit": "gram"}]
}
```

**Không truyền `userId`**. Server lấy tác giả từ JWT, ngăn người dùng giả mạo ID tác giả. Với dữ liệu cũ trong MySQL, user mật khẩu plaintext không dùng được cho đăng nhập BCrypt; tạo tài khoản mới thông qua `/api/auth/register`.

## 6. Hạn chế và việc cần làm trước khi đưa sản phẩm lên production

- Giao diện demo chỉ chọn **một nguyên liệu** khi tạo/sửa; REST API và Postman hỗ trợ danh sách nhiều nguyên liệu.
- Không hỗ trợ tải ảnh lên storage: sử dụng `imageUrl` của ảnh đã lưu ở dịch vụ khác.
- Chưa có refresh token, email verification, forgot password, account lockout/rate limiting, roles/admin, audit log và session revocation. JWT ở `sessionStorage` chỉ phù hợp demo; production nên đánh giá mô hình cookie HttpOnly/SameSite + CSRF, CSP và bảo vệ XSS.
- Người dùng đăng nhập có thể sửa/xóa Category/Ingredient của hệ thống trong MVP; cần RBAC để hạn chế khi triển khai đa người dùng.
- `DDL_AUTO=update` để thuận tiện demo, không thay thế migration DB.
- App test bằng H2 và kiểm thử trên MySQL runtime; CI chưa cấu hình Testcontainers MySQL.
- `mvn clean verify` là tiêu chí xác nhận chính thức; không có BUILD SUCCESS từ môi trường phát triển hiện tại thì không tuyên bố các phần đã chạy hoàn chỉnh.

## 7. Quy trình gửi lỗi khi kiểm thử

Gửi lại (1) lệnh vừa chạy, (2) 30–50 dòng log lỗi đầu tiên hoặc `Caused by`, (3) Java/Maven versions, (4) bạn dùng local `8080` hay Docker `8075`, (5) nếu cần thì `target/site/jacoco/index.html` hoặc report class chưa đạt 80%. **Không gửi mật khẩu/JWT secret thật**.

### JUnit version note

Các bài test dùng JUnit Jupiter (`@Test`, `@ExtendWith(MockitoExtension.class)`), theo phong cách viết JUnit 5. Với Spring Boot 4.1.1, Spring Boot BOM tự quản lý JUnit Jupiter **6.x**. Đây không phải việc ép dependency về artifact JUnit 5.x; lựa chọn này ưu tiên tương thích với Boot 4 và sẽ được kiểm chứng khi Maven verify.


## Khóa/mở khóa tài khoản (Admin)

API `PATCH /api/admin/users/{id}/lock` và `PATCH /api/admin/users/{id}/unlock`.
JWT cũ bị thu hồi bằng `tokenVersion`; xem [ACCOUNT_LOCK_GUIDE.md](ACCOUNT_LOCK_GUIDE.md) để chạy và kiểm thử.
