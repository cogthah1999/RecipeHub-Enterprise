# RecipeHub Enterprise — Comments & Admin Dashboard (November 2026)

Bản này **đã bao gồm nâng cấp giao diện trước đó** (khách xem Recipe, trang đăng nhập/đăng ký/tạo/chi tiết tách riêng). Giải nén thành thư mục mới hoặc backup trước khi chép đè. Không sao chép thư mục `target`.

## Tính năng thêm

- `GET /api/recipes/{recipeId}/comments` — public (Guest xem được).
- `POST /api/recipes/{recipeId}/comments` — đăng nhập, yêu cầu `{ "content": "Món rất ngon" }`, trả 201.
- `PUT /api/recipes/{recipeId}/comments/{commentId}` — chỉ tác giả bình luận hoặc ADMIN, trả 200.
- `DELETE /api/recipes/{recipeId}/comments/{commentId}` — tác giả hoặc ADMIN, trả 204.
- `GET /api/admin/summary` — thống kê Users / Recipes / Categories / Ingredients / Comments.
- `GET /api/admin/users` — tối đa 100 tài khoản gần nhất, **không trả password**.
- `GET /api/admin/comments` — tối đa 8 bình luận gần đây.
- `DELETE /api/admin/comments/{id}` — kiểm duyệt/xóa bình luận.
- `DELETE /api/admin/recipes/{id}` — Admin xóa công thức vi phạm (kèm nguyên liệu liên kết và bình luận).
- `GET /api/users/me` — có thêm trường `role`.
- Giao diện `recipe.html?id=...` có khu vực bình luận cho Guest/User; `admin.html` là dashboard.

## Bảo mật và phân quyền

- Đăng ký công khai luôn tạo **USER**. **Không có API đăng ký ADMIN** hoặc gửi role từ client.
- Admin REST API xác thực JWT và kiểm tra **role trong database** mỗi lần truy cập. Sửa `sessionStorage` hay sửa HTML không cấp quyền.
- User chỉ chỉnh sửa/xóa Recipe của chính họ; bình luận cho phép chủ bình luận hoặc Admin. Guest chỉ GET.
- Người dùng đã đăng nhập có thể tạo danh mục/nguyên liệu như trước.

## Cấp quyền Admin ở môi trường phát triển (thực hiện thủ công)

1. Đăng ký một tài khoản thường bằng giao diện `/register.html`.
2. **Sao lưu DB** trước nếu đã có dữ liệu; chạy database migration tự động chỉ phù hợp môi trường dev (`ddl-auto=update`).
3. Dùng Docker Desktop / MySQL Workbench kết nối `recipehub` hoặc chạy:

   ```powershell
   docker compose exec mysqldb mysql -uroot -p recipehub
   ```

   Nhập mật khẩu MySQL khi được yêu cầu, rồi chạy SQL (thay `your_admin_username` bằng tên bạn vừa đăng ký):

   ```sql
   SELECT id, username, role FROM users;
   UPDATE users SET role = 'ADMIN' WHERE username = 'your_admin_username';
   SELECT username, role FROM users WHERE username = 'your_admin_username';
   ```

4. Đăng xuất rồi đăng nhập lại, mở `http://localhost:8075/admin.html` (Docker) hoặc `http://localhost:8080/admin.html` (Maven).
5. Trên MySQL database cũ, nếu Hibernate không tự thêm `role`, kiểm tra `SHOW COLUMNS FROM users LIKE 'role';` và dùng migration thủ công có kiểm soát. Không chạy SQL cấp Admin trên production tùy tiện.

## Kiểm thử

```powershell
mvn clean verify
# Maven local: start MySQL first, then mvn spring-boot:run
# Docker:
docker compose up --build -d
docker compose ps
powershell -ExecutionPolicy Bypass -File .\smoke-test.ps1 -BaseUrl http://localhost:8075
```

- Postman Collection tăng từ **21 lên 27 requests**, có kiểm tra comments và từ chối dashboard đối với User thường (403).
- Bổ sung integration tests: Guest xem comments, JWT bắt buộc để ghi, Validation 400, chủ comment sửa/xóa, User khác 403, Admin xem báo cáo và kiểm duyệt, xóa Recipe có Comments.
- Báo cáo JaCoCo: `target/site/jacoco/index.html`. Ngưỡng >=80% trong POM không được hạ; sau khi thêm code cần đo lại bằng `mvn clean verify`.
- Không tự nhận `BUILD SUCCESS`/80% cho phiên bản này trước khi chạy xác nhận trên máy bạn (môi trường tạo ZIP không có JDK 25 và Maven dependencies).

## Test UI nhanh

1. Guest mở trang chi tiết Recipe → thấy danh sách comments nhưng không có form viết.
2. User A login → đăng comment; refresh vẫn còn, sửa/xóa comment của A.
3. User B login → xem comment của A nhưng không có nút sửa/xóa và API trả 403 nếu cố thực hiện.
4. Admin login → menu Quản trị xuất hiện → dashboard và nút kiểm duyệt hoạt động.
5. User thường truy cập `admin.html` → điều hướng về trang chủ; gọi `/api/admin/summary` bằng JWT USER → 403.
6. Xóa Recipe có comment, xác nhận xóa được và dữ liệu comment không mồ côi.

## Giới hạn MVP

- Chưa có upload ảnh, email/notification, phân trang Recipe; bảng Admin đang liệt kê tất cả Recipe nên chỉ phù hợp demo dữ liệu nhỏ.
- DB schema dev dùng Hibernate `ddl-auto=update`, nên với dữ liệu thật cần migration Flyway/Liquibase và backup.
- JWT lưu trong sessionStorage nhằm thuận tiện demo; để triển khai production cần threat model và hardening bảo mật bổ sung.
- Admin chưa có giao diện khóa người dùng/đổi role (tránh cấp quyền nhầm); việc cấp Admin hiện chủ động làm ở DB.
