# Booking Platform (Spa) — Implementation Roadmap

Nguyên tắc: đi **tuần tự từng Phase, từng Step trong Phase đó**. Không bắt đầu Step tiếp theo khi Step hiện tại chưa chạy được và test được. Không nhảy sang Phase sau khi Phase hiện tại chưa hoàn chỉnh + test end-to-end.

---

## Phase 0 — Scaffolding & môi trường local

1. Tạo monorepo structure (`services/`, `frontend/`, `terraform/`, `.github/`, `docs/`)
2. Viết `docker-compose.yml` gốc: Postgres, Redis, LocalStack (mock SQS/S3)
3. Chạy `docker compose up` — xác nhận Postgres, Redis, LocalStack lên healthy
4. Tạo database + baseline schema rỗng cho từng service trong Postgres (`auth_schema`, `venue_schema`, `booking_schema`, `payment_schema`)
5. Setup Flyway convention chung (naming migration file, cách mỗi service tự quản lý migration của schema mình)
6. Khởi tạo skeleton `gateway-service` (Spring Cloud Gateway) — chưa cần route gì, chỉ chạy lên được là đạt

**Điều kiện hoàn thành Phase 0:** `docker compose up` chạy được toàn bộ hạ tầng nền (DB, Redis, mock AWS), gateway service khởi động thành công.

---

## Phase 1 — auth-service

1. Tạo entity: `User`, `RefreshToken`, `UserShopMapping`
2. Viết Flyway migration cho `auth_schema`
3. Implement JWT util: sign/verify token, claims gồm `role` + `shopIds`
4. Endpoint `POST /auth/register` (mặc định role = USER)
5. Endpoint `POST /auth/login` — trả access token + refresh token
6. Endpoint `POST /auth/refresh`
7. Endpoint `GET /auth/me`
8. Endpoint admin: `POST /admin/users` (tạo user bất kỳ role), `POST /admin/user-shop-mapping` (gán shop cho manager)
9. Spring Security config: `hasRole()` theo `ADMIN` / `SHOP_MANAGER` / `USER`
10. Viết test (unit cho JWT util, integration cho luồng register → login → refresh)
11. Dockerize service, thêm vào `docker-compose.yml`
12. Test thủ công toàn bộ luồng qua Postman/curl: register → login → gọi `/me` bằng token → admin tạo shop manager → gán shop

**Điều kiện hoàn thành Phase 1:** toàn bộ luồng auth chạy được độc lập, chưa cần service nào khác.

---

## Phase 2 — venue-service

1. Tạo entity: `Shop`, `Resource`, `TimeSlot`
2. Flyway migration cho `venue_schema`
3. Endpoint admin: `POST /admin/shops` (tạo shop)
4. Endpoint shop manager: `POST /shops/{shopId}/resources`, `POST /shops/{shopId}/slots` — check quyền sở hữu bằng `shopIds` trong JWT
5. Endpoint public: `GET /shops`, `GET /shops/{shopId}/slots?date=...`
6. Test quyền: shop manager A không được sửa slot của shop B (dùng token thật từ auth-service)
7. Viết test (unit + integration)
8. Dockerize, thêm vào `docker-compose.yml`
9. Test thủ công: admin tạo shop → gán manager (gọi lại auth-service) → manager login → tạo resource/slot → user xem được danh sách slot trống

**Điều kiện hoàn thành Phase 2:** venue-service hoạt động độc lập, xác thực bằng JWT thật từ auth-service, chưa cần booking-service.

---

## Phase 3 — booking-service (khung cơ bản, chưa nối payment)

1. Tạo entity: `Booking` (status: PENDING, CONFIRMED, CANCELLED)
2. Flyway migration cho `booking_schema`
3. Endpoint `POST /bookings` — gọi venue-service kiểm tra slot còn trống, tạo booking status = PENDING (**chưa có lock Redis ở bước này**)
4. Endpoint tạm thời để test thủ công: `PATCH /bookings/{id}/confirm`, `PATCH /bookings/{id}/cancel` (sẽ thay bằng event-driven ở Phase 7)
5. Test luồng thủ công: tạo booking → confirm tay → xem status đổi đúng
6. Dockerize, thêm vào `docker-compose.yml`

**Điều kiện hoàn thành Phase 3:** tạo/xem/confirm/cancel booking chạy được thủ công, chưa có concurrency handling, chưa có payment thật.

---

## Phase 4 — Redis: lock chống double-booking + cache slot

1. Thêm Redis client vào `booking-service`
2. Implement lock `SETNX lock:slot:{slotId}` (TTL 10s) trước khi tạo booking ở endpoint `POST /bookings`
3. Release lock sau khi xử lý xong (thành công hoặc lỗi)
4. Viết script test đơn giản (bash/k6) bắn nhiều request cùng lúc vào 1 slot → xác nhận chỉ 1 request thành công
5. Thêm cache slot trống ở `venue-service` (TTL 30–60s), invalidate khi có booking mới
6. Test cache: gọi lại `GET /shops/{shopId}/slots` nhiều lần, xác nhận không query DB liên tục (kiểm tra qua log)

**Điều kiện hoàn thành Phase 4:** chứng minh được bằng test thực tế là không xảy ra double-booking khi có nhiều request đồng thời.

---

## Phase 5 — Outbox pattern trong booking-service

1. Tạo bảng `outbox_events` trong `booking_schema`
2. Refactor: khi tạo/đổi trạng thái booking, ghi thêm 1 row vào `outbox_events` trong cùng transaction
3. Viết relay job (scheduled task, polling mỗi vài giây) đọc row `status = PENDING`, gửi lên SQS (LocalStack lúc dev), đánh dấu `SENT`
4. Test: tạo booking → kiểm tra message thực sự xuất hiện trong queue (qua LocalStack CLI hoặc console)
5. Test trường hợp lỗi: giả lập relay job crash giữa chừng → xác nhận row vẫn `PENDING` và được gửi lại ở lần chạy sau (không mất, không cần chưa trùng lặp xử lý ở bước này — sẽ xử lý idempotency ở Phase 7)

**Điều kiện hoàn thành Phase 5:** booking-service publish event lên SQS một cách đáng tin cậy, có bằng chứng qua test.

---

## Phase 6 — payment-service + Stripe

1. Tạo entity `Payment`, bảng `outbox_events` riêng trong `payment_schema`
2. Endpoint `POST /payments/intent` — tạo Stripe PaymentIntent (test mode), trả `client_secret`
3. Endpoint webhook `POST /payments/webhook` — verify chữ ký Stripe, xử lý `payment_intent.succeeded` / `payment_intent.payment_failed`
4. Khi nhận webhook thành công: ghi `Payment` + ghi outbox event trong cùng transaction (dùng lại pattern ở Phase 5)
5. Relay job riêng cho payment-service, publish `PAYMENT_SUCCEEDED` / `PAYMENT_FAILED` lên SQS
6. Test bằng Stripe CLI (`stripe listen --forward-to`) + test card `4242 4242 4242 4242`
7. Dockerize, thêm vào `docker-compose.yml`

**Điều kiện hoàn thành Phase 6:** payment-service độc lập tạo PaymentIntent, nhận webhook, publish event — chưa nối với booking-service.

---

## Phase 7 — Nối Saga: booking-service lắng nghe payment events

1. Thêm SQS consumer trong `booking-service`, subscribe queue `payment-events`
2. Xử lý `PAYMENT_SUCCEEDED` → cập nhật booking `CONFIRMED`, release lock Redis (nếu còn giữ)
3. Xử lý `PAYMENT_FAILED` → cập nhật booking `CANCELLED`, nhả slot
4. Xoá 2 endpoint tạm thời `PATCH /bookings/{id}/confirm|cancel` đã tạo ở Phase 3 (không cần nữa vì đã event-driven)
5. Thêm xử lý idempotency: kiểm tra event đã xử lý chưa trước khi update (tránh xử lý trùng nếu SQS gửi lại message)
6. Thêm scheduled job: tự động cancel booking `PENDING` quá X phút chưa thanh toán, nhả slot
7. Test end-to-end đầy đủ: tạo booking → tạo payment intent → thanh toán qua Stripe test → xác nhận booking tự chuyển `CONFIRMED` mà không cần gọi tay

**Điều kiện hoàn thành Phase 7:** toàn bộ luồng Saga booking + payment chạy tự động end-to-end, không còn thao tác tay nào.

---

## Phase 8 — notification-service

1. Tạo SQS consumer subscribe `booking-events` (booking CONFIRMED/CANCELLED)
2. Viết email template (xác nhận đặt lịch, hủy lịch)
3. Tích hợp gửi mail qua Mailtrap (SMTP)
4. Test: thực hiện lại luồng Phase 7, xác nhận email xuất hiện trong Mailtrap inbox

**Điều kiện hoàn thành Phase 8:** email tự động gửi đúng lúc, đúng nội dung theo trạng thái booking.

---

## Phase 9 — realtime-service (SSE)

1. Tạo endpoint SSE `GET /realtime/shops/{shopId}/stream`
2. Subscribe event booking CONFIRMED/CANCELLED (qua SQS hoặc gọi nội bộ), push xuống client đang mở kết nối SSE của đúng shop đó
3. Test bằng 1 trang HTML đơn giản dùng `EventSource` (chưa cần FE thật) — xác nhận nhận được event realtime khi có booking mới

**Điều kiện hoàn thành Phase 9:** xác nhận SSE hoạt động đúng bằng test thủ công trước khi build FE.

---

## Phase 10 — Gateway: route toàn bộ qua Spring Cloud Gateway

1. Định nghĩa route cho từng service (`/api/auth/**`, `/api/venue/**`, `/api/booking/**`, `/api/payment/**`, `/api/realtime/**`)
2. Cập nhật toàn bộ test/Postman collection để gọi qua gateway thay vì gọi thẳng từng service
3. (Tuỳ chọn) thêm rate limiting cơ bản ở gateway
4. Test lại toàn bộ luồng Phase 1–9 nhưng đi qua gateway để xác nhận không có gì vỡ

**Điều kiện hoàn thành Phase 10:** toàn bộ backend hoạt động thông qua 1 entry point duy nhất.

---

## Phase 11 — Frontend: Auth & layout

1. Khởi tạo Vite + React + Shadcn/ui
2. Setup TanStack Query, React Hook Form + Zod
3. Trang đăng ký/đăng nhập, lưu token (memory hoặc httpOnly cookie nếu BE hỗ trợ), gọi qua gateway
4. Layout chính + routing phân theo role (redirect đúng dashboard sau khi login)

**Điều kiện hoàn thành Phase 11:** login được, vào đúng layout theo role, chưa cần chức năng nghiệp vụ.

---

## Phase 12 — Frontend: luồng đặt lịch công khai

1. Trang tìm shop, xem slot trống (gọi venue-service)
2. Trang đặt lịch: chọn slot → tạo booking → checkout Stripe Elements
3. Kết nối SSE để tự cập nhật slot khi có người khác vừa đặt
4. Test thủ công toàn bộ luồng bằng UI thật, không qua Postman nữa

**Điều kiện hoàn thành Phase 12:** user thật có thể đặt lịch + thanh toán hoàn chỉnh qua giao diện.

---

## Phase 13 — Frontend: Shop Manager dashboard

1. Trang quản lý resource/slot của shop
2. Trang danh sách booking, cập nhật realtime khi có booking mới (SSE)
3. Test bằng tài khoản Shop Manager thật đã tạo ở Phase 1–2

---

## Phase 14 — Frontend: Admin dashboard

1. Trang tạo shop
2. Trang tạo user + gán quản lý shop
3. (Tuỳ chọn) trang thống kê tổng quan

**Điều kiện hoàn thành Phase 11–14:** toàn bộ hệ thống dùng được hoàn chỉnh qua giao diện, không cần Postman/curl nữa.

---

## Phase 15 — Terraform (môi trường `dev`)

1. Module `network` (VPC, public subnet, security group)
2. Module `ec2` (instance chạy Docker Compose, IAM role, key pair)
3. Module `rds` (1 instance Postgres, subnet group)
4. Module `sqs` (queues + DLQ)
5. Module `s3-cloudfront` (hosting FE)
6. Module `ecr` (registry từng service)
7. Module `iam` (least-privilege roles)
8. Module `monitoring` (CloudWatch log group, Budget alert 50$/80$)
9. `terraform init` → `terraform plan` → review kỹ trước khi `apply`
10. `terraform apply`, xác nhận resource lên đúng trên AWS Console

**Điều kiện hoàn thành Phase 15:** hạ tầng thật trên AWS được tạo hoàn toàn bằng Terraform, không có resource nào tạo tay qua Console.

---

## Phase 16 — CI/CD (GitHub Actions)

1. `ci-backend.yml`: build + test mỗi service khi có PR
2. `ci-frontend.yml`: build + lint FE khi có PR
3. `deploy.yml`: build Docker image → push ECR → SSH/SSM vào EC2 → `docker compose pull && up -d`
4. Test bằng cách merge 1 thay đổi nhỏ, xác nhận pipeline chạy tự động và deploy thành công

---

## Phase 17 — Deploy & verify trên môi trường thật

1. Deploy toàn bộ service lên EC2 qua pipeline
2. Trỏ FE build lên S3 + CloudFront
3. Chuyển email từ Mailtrap sang SES (nếu cần demo cho người ngoài xem)
4. Xác nhận Budget Alert đã bật, kiểm tra credit còn lại
5. Test lại toàn bộ luồng chính (đặt lịch, thanh toán, hủy, dashboard admin/manager) trên môi trường thật

---

## Phase 18 — Hoàn thiện & tài liệu

1. Viết `README.md` (giới thiệu, cách chạy local, cách deploy)
2. Hoàn thiện `docs/adr/` — mỗi quyết định lớn 1 file ngắn
3. Viết script load test (k6) chứng minh chống double-booking, lưu kết quả vào `docs/`
4. Chụp ảnh/quay demo ngắn để đưa vào portfolio/CV

---

**Lưu ý xuyên suốt:** mỗi Phase kết thúc bằng 1 bước test cụ thể — không coi là "xong" nếu chưa test được bước đó. Không bắt đầu Phase N+1 khi Phase N chưa qua được điều kiện hoàn thành.
