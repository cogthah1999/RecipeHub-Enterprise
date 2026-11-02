# RecipeHub Enterprise — giao diện theo luồng ASP.NET RecipeWebsite

Bản vá tập trung vào luồng Guest / User và các trang tách biệt, không thay đổi database MySQL.

## Các trang

- `/` — danh sách công thức, tìm kiếm, lọc danh mục, khách xem được.
- `/recipe.html?id=ID` — trang chi tiết công thức, nguyên liệu, hướng dẫn; khách xem được.
- `/login.html` — chỉ tên đăng nhập và mật khẩu.
- `/register.html` — tên đăng nhập, email, họ tên, mật khẩu.
- `/editor.html` — tạo Recipe (JWT bắt buộc); thêm danh mục/nguyên liệu trong phần mở rộng ở cuối trang.
- `/editor.html?id=ID` — sửa Recipe (backend chỉ chấp nhận tác giả).

## Giới hạn

- Chưa có upload file ảnh (chỉ URL ảnh).
- Chưa chuyển module bình luận và quản trị Admin từ dự án ASP.NET.
- Danh mục và nguyên liệu do mọi tài khoản hợp lệ thêm được; nếu muốn chỉ Admin được thêm, cần role và rules khác.
- JWT nằm trong `sessionStorage` như bản cũ; sản phẩm triển khai production cần đánh giá bảo mật/HTTP-only cookie và CSRF phù hợp.

## Chạy ứng dụng (PowerShell VS Code)

1. Lưu dự phòng hoặc commit mã nguồn trước khi áp dụng patch.
2. `mvn clean verify` — bao gồm MockMvc, JUnit, JaCoCo, PMD, Checkstyle, SpotBugs.
3. `docker compose up --build -d`
4. `docker compose ps`
5. Mở `http://localhost:8075/`.
6. Nếu CSS/JS cũ còn cache: nhấn `Ctrl + Shift + R`.

Nếu chạy qua Maven thay vì Docker: `docker compose up -d mysqldb`, `mvn spring-boot:run`, truy cập `http://localhost:8080/`.

## Kiểm thử thủ công

1. Truy cập trang chủ lúc chưa đăng nhập → xem được danh sách, tìm kiếm, lọc.
2. Bấm vào một món → thấy thông tin, nguyên liệu (số lượng + đơn vị), hướng dẫn.
3. Truy cập `/editor.html` với tư cách khách → tự chuyển tới đăng nhập.
4. Đăng ký: 4 trường bắt buộc; đăng nhập: 2 trường.
5. Sau đăng nhập tạo danh mục/nguyên liệu từ khối mở rộng ở trang tạo công thức.
6. Tạo món: bỏ trống tên, hướng dẫn, nguyên liệu → thông báo lỗi trên web; API trả HTTP 400 theo JSON `status`/`message`.
7. Thêm từ 2 nguyên liệu, lưu → xem chi tiết có đủ 2 nguyên liệu.
8. Tài khoản A tạo món, tài khoản B xem được nhưng không thấy nút sửa/xóa; thử PUT/DELETE với JWT B → HTTP 403.
9. Tài khoản A tự sửa/xóa món của mình → thành công.
10. Chạy lại `smoke-test.ps1` để xác nhận CRUD vẫn hoạt động.

## Lưu ý

- Chạy lại `mvn clean verify` trên **JDK 25** ở máy của bạn. Môi trường tạo bản vá chỉ có JDK 21, chưa chạy được Maven integration tests.
- Nếu trước đây frontend có file JS cũ đang được Docker phục vụ, `docker compose up --build -d` cần build lại để nhận `static` mới.
