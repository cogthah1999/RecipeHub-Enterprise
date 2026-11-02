# RecipeHub Enterprise — Khóa / mở khóa tài khoản

## Mục đích

- Guest: chỉ xem công thức và bình luận, không cần JWT.
- USER: tạo/sửa/xóa nội dung của mình.
- ADMIN: truy cập `/admin.html`, khóa/mở khóa USER và kiểm duyệt nội dung.
- Khi tài khoản bị khóa: không thể đăng nhập; JWT đã cấp bị từ chối HTTP 401.
- Sau khi mở khóa: JWT cũ vẫn bị từ chối do `tokenVersion`; người dùng **bắt buộc đăng nhập lại**.

## Cách chạy

1. Sao lưu dự án và **giữ nguyên** file `.env` (nếu dùng Docker) cùng volume MySQL.
2. Từ thư mục chứa `pom.xml`: `mvn clean verify`.
3. Chạy `docker compose up --build -d`; không dùng `docker compose down -v`.
4. `http://localhost:8075/admin.html` → danh sách người dùng → nút **Khóa** / **Mở khóa**.
5. Cấp Admin bằng SQL thủ công với một tài khoản thử nghiệm: 
   `UPDATE users SET role = 'ADMIN' WHERE username = 'ten_admin';`
   Đăng xuất / đăng nhập lại.

### API

`PATCH /api/admin/users/{id}/lock` — cần JWT ADMIN; body:
```json
{"reason":"Đăng nội dung spam nhiều lần"}
```

`PATCH /api/admin/users/{id}/unlock` — cần JWT ADMIN, không có body.

`GET /api/admin/users` — liệt kê tối đa 100 tài khoản gần nhất, bao gồm `enabled`, `lockReason`, `lockedAt`, `lockedBy`.

Mã HTTP: 200 thành công; 400 thiếu lý do; 401 thiếu JWT/JWT bị thu hồi; 403 không phải Admin hoặc cố khóa Admin; 404 ID không tồn tại; 409 khóa trùng hoặc mở khóa khi tài khoản đang hoạt động.

### Dữ liệu cũ & token

Dự án hiện đang có `spring.jpa.hibernate.ddl-auto=update`. Khi khởi động, Hibernate sẽ thêm cột `token_version`, `locked_at`, `lock_reason`, `locked_by` vào bảng `users` nếu DB cho phép. Với dữ liệu thực tế/production, nên dùng Flyway thay cho `ddl-auto=update` và chạy migration có kiểm tra trước.

JWT cũ tạo ở phiên bản trước bản vá **không có `tokenVersion`** nên sẽ không còn hợp lệ sau khi cập nhật. Mọi người phải đăng nhập lại. JWT phiên bản mới chứa `tokenVersion`, được đối chiếu với MySQL trên từng request có Bearer token. Cách này ưu tiên khả năng thu hồi ngay, đổi lại mỗi request có JWT sẽ cần truy vấn DB (có thể tối ưu cache sau).

### Kiểm thử thủ công

1. Với ADMIN vào Dashboard, khóa User B với lý do bất kỳ.
2. User B gọi `GET /api/users/me` bằng token cũ → 401.
3. User B đăng nhập lại khi vẫn bị khóa → 401.
4. Guest gọi `GET /api/recipes` không cần token → 200.
5. ADMIN mở khóa User B.
6. Token cũ của User B vẫn trả 401; đăng nhập User B lại nhận token mới → `GET /api/users/me` trả 200.
7. USER thường thử gọi PATCH lock → 403; Admin cố khóa chính mình → 403.
8. Kiểm tra `mvn clean verify`, `target/site/jacoco/index.html` và `docker compose ps`.

**Chú ý**: Trang quản trị phục vụ qua static HTML công khai, nhưng toàn bộ endpoint dữ liệu và thao tác quản trị bắt buộc xác thực ADMIN tại backend. Không đăng token, mật khẩu hoặc `.env` lên GitHub.
