# PrepAce — Tính năng của Nguyễn Văn Hải (hoàn thiện FE + BE)

Các use case được giao cho **Nguyễn Văn Hải** trong ProjectTracking, đã hoàn thiện
end-to-end (frontend ↔ backend ↔ SQL) và kiểm thử thực tế.

| # | Tính năng | Actor | Endpoint chính | Trang FE |
|---|-----------|-------|----------------|----------|
| 13 | Attempt Entry Test | Student | `GET /api/entry-test/quizzes`, `POST /api/entry-test/submit`, `GET /api/entry-test/assessment/{id}` | `/entry-test`, `/entry-test/result/:id` |
| 14 | Purchase Packages | Student | `POST /api/payments/create` → `POST /api/payments/confirm` → auto-enroll | `/checkout/:courseId`, `/payment/return` |
| 26 | Consult AI Chatbot | Student | `POST /api/ai/chat` | `/ai/chat` |
| 27 | Adaptive Path Generation | Student | `GET /api/ai/adaptive-path` | `/adaptive-path` |
| 28 | AI Gap Diagnosis | Student | `GET /api/ai/gap-diagnosis` | `/ai/gap-diagnosis` |
| 29 | AI Score Forecasting | Student | `GET /api/ai/score-forecast` | `/ai/score-forecast` |
| 30 | AI University Advising | Student | `GET /api/ai/university-advising?block=A01` | `/ai/university-advising` |

## Kiến trúc & quyết định

- **Xác thực studentId**: các API trên dùng header `X-Student-Id`. Frontend tự đính kèm
  header này từ `localStorage.user.id` qua interceptor `src/api/axiosClient.js`.
- **AI analytics tính từ dữ liệu thật**: điểm năng lực theo môn được tính từ bảng
  `QuizAttempts` (join `Quizzes → Courses → Subjects`). Gemini chỉ làm "giàu" phần nhận
  xét (summary); nếu Gemini lỗi/không có key hợp lệ, hệ thống tự dùng **fallback
  rule-based** (`source = FALLBACK`) nên không bao giờ treo.
- **Gemini đã chạy LIVE**: gọi qua header `X-goog-api-key` (model `gemini-flash-latest`).
  Chatbot trả `source = LIVE` khi Gemini phản hồi, `FALLBACK` khi gặp sự cố.
- **Thanh toán**: cổng giả lập (sandbox) — `create` (PENDING) → trang xác nhận →
  `confirm` (SUCCESS/FAILED) → tự động enroll. Dễ thay bằng VNPAY/MOMO thật sau.

## Cấu hình

`src/main/resources/application.properties`:
- `gemini.api.key` — đặt API key Google AI Studio (bắt đầu bằng `AIza`) để bật AI thật.
  Hiện key chưa hợp lệ nên AI chạy ở chế độ fallback.
- `gemini.api.timeout` — timeout (ms) trước khi fallback.

## Chạy dự án

```bash
# 1. Database (SQL Server) — chạy migration cho các tính năng của Hải
#    LƯU Ý: phải có -f 65001 để tiếng Việt không bị lỗi font
sqlcmd -S 127.0.0.1,1433 -U sa -P 123456 -C -f 65001 -i sql/prepace_hai_migration.sql

# 2. Backend (Spring Boot, cổng 8080)
./mvnw spring-boot:run

# 3. Frontend (Vite, cổng 5173)
cd FrontEnd/thang-frontend && npm install && npm run dev
```

Tài khoản demo (đã có sẵn dữ liệu năng lực để xem AI): `student1@gmail.com` / `123456`.

## Migration đã thêm (vì schema gốc thiếu cột entity yêu cầu)

- `Quizzes.quiz_type`, `Questions.cognitive_level`
- `QuizAttempts.total_questions`, `QuizAttempts.correct_count`
- `AIChatHistory.request_type`
- Seed `QuizAttempts` + `Payments` cho học sinh demo (user_id = 5)

## Thanh toán chuyển khoản ngân hàng — VietQR (KHUYẾN NGHỊ, tiền về thẳng TK của bạn)

Người dùng bấm thanh toán → hiện **mã VietQR đã điền sẵn số tiền + nội dung CK** →
quét bằng app ngân hàng → tiền vào **thẳng tài khoản ngân hàng của bạn** → hệ thống
tự mở khóa khóa học.

**Cấu hình tài khoản của bạn** trong `application.properties`:
```properties
bank.bin=970436           # mã NH (Vietcombank=970436, Techcombank=970407, MBBank=970422, BIDV=970418...)
bank.account-no=SỐ_TK_CỦA_BẠN
bank.account-name=TÊN CHỦ TÀI KHOẢN (IN HOA, không dấu)
```

**Tự động xác nhận khi tiền vào** (để app tự mở khóa học): dùng dịch vụ đọc biến động
số dư **SePay** (sepay.vn — miễn phí cho cá nhân):
1. Đăng ký SePay, liên kết tài khoản ngân hàng của bạn.
2. Tạo Webhook trỏ tới `https://<ngrok>/api/payments/bank/webhook`, thêm header
   `X-Webhook-Key: PREPACE_SECRET` (khớp `bank.webhook-key`).
3. Khi có tiền vào khớp nội dung `PREPACE...` và đúng số tiền → app tự enroll.
Khi chưa gắn SePay, vẫn có nút **"Tôi đã chuyển khoản"** để xác nhận thủ công (demo).

**Endpoint:** `POST /api/payments/bank/create`, `GET /api/payments/bank/status/{ref}`,
`POST /api/payments/bank/confirm/{ref}`, `POST /api/payments/bank/webhook`.
Trang FE: `/pay/bank/:courseId` (`BankTransferPage.jsx`).

## Thanh toán qua ZaloPay (tùy chọn)

Hệ thống đã tích hợp sẵn cổng **ZaloPay** (đang chạy ở **sandbox** với credential demo công khai).

**Luồng:** Checkout hiện giá đơn → bấm "Thanh toán qua ZaloPay" → BE gọi `POST /v2/create`
→ redirect sang trang ZaloPay thật → thanh toán → ZaloPay gọi callback (hoặc trang return
tự `query` trạng thái) → tự động enroll khóa học.

**Endpoint BE:** `POST /api/payments/zalopay/create`, `POST /api/payments/zalopay/callback`,
`GET /api/payments/zalopay/status/{ref}`. Code: `service/ZaloPayService.java` + `PaymentService`.

### Để tiền chuyển trực tiếp về TÀI KHOẢN CỦA BẠN (3 bước)

1. **Đăng ký Merchant**: vào https://merchant.zalopay.vn, tạo ứng dụng, lấy **App ID, Key1,
   Key2**. Liên kết tài khoản nhận tiền (ngân hàng/ví ZaloPay của bạn) trong phần cài đặt merchant.
2. **Thay credential** trong `src/main/resources/application.properties`:
   ```properties
   zalopay.app-id=<APP_ID_CỦA_BẠN>
   zalopay.key1=<KEY1_CỦA_BẠN>
   zalopay.key2=<KEY2_CỦA_BẠN>
   # Khi go-live, đổi sang endpoint production:
   zalopay.create-url=https://openapi.zalopay.vn/v2/create
   zalopay.query-url=https://openapi.zalopay.vn/v2/query
   ```
   Sau bước này, mỗi giao dịch thành công tiền sẽ vào tài khoản merchant của bạn (ZaloPay đối
   soát và chuyển về ngân hàng đã liên kết).
3. **Callback URL công khai**: ZaloPay gọi server bạn để xác nhận. Khi chạy localhost, dùng
   ngrok: `ngrok http 8080`, rồi đặt
   `zalopay.callback-url=https://<id>.ngrok.io/api/payments/zalopay/callback`.
   *Không có callback vẫn chạy được* nhờ trang return tự gọi `query status` để xác nhận.

> Lưu ý: ZaloPay chỉ chuyển tiền vào **tài khoản MERCHANT doanh nghiệp**, không hỗ trợ API
> đẩy tiền thẳng vào ZaloPay **cá nhân**. Nếu chỉ cần nhận vào ví cá nhân để demo, có thể
> dùng mã **QR ZaloPay cá nhân** (tĩnh) hiển thị ở bước thanh toán — nhưng khi đó hệ thống
> không tự xác nhận được đơn (phải xác nhận thủ công).

## Kho đề Kiểm tra đầu vào (mở rộng)

Chạy `sql/prepace_question_bank.sql` (`-f 65001`) để nạp **6 đề / 48 câu** trên 5 môn:
Toán (Cơ bản + Nâng cao), Vật lý, Tiếng Anh, Hóa học, Ngữ văn. Script tự tạo thêm
2 khóa học (Hóa, Văn) nếu chưa có. File được sinh từ `sql/gen_question_bank.py`
(sửa dữ liệu câu hỏi trong file Python rồi chạy lại để tái sinh SQL).
