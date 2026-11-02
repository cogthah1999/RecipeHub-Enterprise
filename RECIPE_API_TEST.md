# Recipe API quick test

Đã cập nhật từ bản trước: **không còn truyền `userId` trong RecipeRequest**; tác giả lấy từ JWT do `/api/auth/register` hoặc `/api/auth/login` trả về.

Để thử đầy đủ mà không cần gõ request thủ công:

```powershell
powershell -ExecutionPolicy Bypass -File .\smoke-test.ps1
```

Hoặc import `postman/RecipeHub.postman_collection.json` vào Postman và chạy từ request 01 đến 21.

Ví dụ POST `/api/recipes`:

```json
{
  "name": "Gà chiên nước mắm", "description": "Bữa cơm gia đình", "cookingTime": 30,
  "servings": 2, "instructions": "Chiên gà", "categoryId": 1,
  "ingredients": [{"ingredientId":1, "quantity": 500, "unit": "gram"}]
}
```

Header: `Authorization: Bearer <accessToken>` và `Content-Type: application/json; charset=utf-8`.
