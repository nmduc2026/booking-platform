# Booking Platform (Spa) — Architecture Overview

## 1. Giới thiệu sản phẩm

Nền tảng đặt lịch spa theo mô hình marketplace: nhiều shop spa đăng ký lên hệ thống, khách hàng vào tìm và đặt lịch, thanh toán qua Stripe (test mode).

### Vai trò (Roles)

JWT-based, 3 role:

- **ADMIN**: quản lý toàn hệ thống — tạo shop, tạo tài khoản Shop Manager, gán quyền quản lý shop cho Shop Manager
- **SHOP_MANAGER**: quản lý 1 hoặc nhiều shop được gán — tạo resource, tạo time slot, xem/xử lý booking, xem doanh thu
- **USER**: khách hàng — tìm shop, đặt lịch, thanh toán, hủy lịch

### Luồng tạo shop (đã chốt — không có bước duyệt)

```
Admin tạo shop (POST /admin/shops)
  → Admin tạo tài khoản user cho Shop Manager (POST /admin/users)
  → Admin gán user đó quản lý shop (POST /admin/user-shop-mapping)
  → Shop Manager login, bắt đầu tạo resource + time slot cho shop của mình
```

Không có luồng tự đăng ký làm Shop Manager hay chờ admin duyệt — admin là người khởi tạo toàn bộ.

---

## 2. Kiến trúc tổng thể

```
                         ┌──────────────────────┐
                         │  Spring Cloud Gateway  │  ← API Gateway (1 service Java)
                         └───────────┬────────────┘
     ┌───────────┬───────────────────┼───────────────┬─────────────────┬────────────────┐
┌────▼────┐ ┌─────▼─────┐     ┌──────▼──────┐  ┌──────▼──────┐  ┌───────▼───────┐ ┌──────▼──────┐
│  auth-  │ │  venue-   │     │  booking-   │  │  payment-   │  │ notification- │ │  realtime-  │
│ service │ │  service  │     │  service    │  │  service    │  │   service     │ │   service   │
└────┬────┘ └─────┬─────┘     └──────┬──────┘  └──────┬──────┘  └───────────────┘ └─────────────┘
     │            │                  │                │
     └────────────┴──────┬───────────┴────────────────┘
                          │
              ┌───────────▼────────────┐        ┌────────────────┐
              │  RDS PostgreSQL (1     │        │  Redis (chạy   │
              │  instance, mỗi service │        │  chung trên    │
              │  1 schema riêng)       │        │  EC2)          │
              └────────────────────────┘        └────────────────┘

Compute: tất cả service (trừ RDS) chạy trong Docker Compose trên 1 EC2 instance.
Async messaging: SQS (booking-events, payment-events, notification-events + DLQ).
```

### Nguyên tắc kiến trúc

- **Database-per-service (dạng nhẹ)**: 1 RDS instance duy nhất, mỗi service có **schema riêng** (`auth_schema`, `venue_schema`, `booking_schema`, `payment_schema`). Không service nào query chéo schema của service khác — chỉ giao tiếp qua REST hoặc event (SQS).
- **Giao tiếp đồng bộ (REST)**: dùng khi cần phản hồi ngay (FE gọi booking-service check slot trống).
- **Giao tiếp bất đồng bộ (SQS)**: dùng cho các bước không cần chờ kết quả ngay (payment xong → báo booking-service, notification-service).
- **Saga pattern (choreography)**: không có service trung tâm điều phối; mỗi service tự lắng nghe event và cập nhật trạng thái của mình.
- **Outbox pattern**: đảm bảo ghi DB + publish event luôn đồng bộ, tránh dual-write.

---

## 3. Chi tiết từng service

| Service | Trách nhiệm | Schema (trong RDS chung) |
|---|---|---|
| **auth-service** | Đăng ký/login, phát hành JWT (claims: `role`, `shopIds`), refresh token, admin tạo user/gán shop | `auth_schema`: users, refresh_tokens, user_shop_mapping |
| **venue-service** | Quản lý shop, resource (phòng/giường/kỹ thuật viên), time slot | `venue_schema`: shops, resources, time_slots |
| **booking-service** | Tạo/hủy booking, khóa slot tạm, Saga participant, Outbox | `booking_schema`: bookings, outbox_events |
| **payment-service** | Tạo Stripe PaymentIntent, xử lý webhook, refund, Saga participant, Outbox | `payment_schema`: payments, outbox_events |
| **notification-service** | Gửi email xác nhận/hủy lịch | Không cần schema riêng (stateless) |
| **realtime-service** | Push SSE cho Shop Manager dashboard + trạng thái slot | Không cần schema riêng (stateless) |
| **Spring Cloud Gateway** | Entry point duy nhất, route request theo path, có thể thêm rate limit | Không có DB |

### JWT claims structure

```json
{
  "sub": "user-uuid",
  "email": "manager@shop.com",
  "role": "SHOP_MANAGER",
  "shopIds": ["shop-uuid-1", "shop-uuid-2"],
  "iat": 1234567890,
  "exp": 1234571490
}
```

Mỗi service tự verify JWT (không cần gọi lại auth-service) → giảm coupling, tăng tốc độ xử lý request.

---

## 4. Luồng nghiệp vụ quan trọng

### 4.1. Đặt lịch + thanh toán (Saga choreography)

```
1. User chọn slot
   → Redis: thử SETNX "lock:slot:{slotId}" (TTL 10s)
   → Nếu lock thành công: booking-service tạo booking, status = PENDING

2. FE gọi payment-service → tạo Stripe PaymentIntent → trả client_secret

3. FE dùng Stripe Elements xác nhận thanh toán

4. Stripe gửi webhook → payment-service nhận payment_intent.succeeded
   → payment-service ghi payment vào DB + ghi event vào bảng outbox
     (cùng 1 transaction Postgres)

5. Polling job (relay) đọc bảng outbox → gửi event "PAYMENT_SUCCEEDED" lên SQS
   → đánh dấu outbox row = SENT

6. booking-service nhận event từ SQS
   → cập nhật booking status = CONFIRMED
   → xoá lock Redis
   → notification-service gửi email xác nhận
   → realtime-service push SSE báo Shop Manager có booking mới

7. Nếu payment fail/timeout (quá 10 phút không thanh toán):
   → payment-service publish "PAYMENT_FAILED"
   → booking-service cập nhật status = CANCELLED, nhả slot (compensating transaction)
```

### 4.2. Outbox pattern — chi tiết

Vấn đề giải quyết: tránh dual-write (ghi DB thành công nhưng gửi SQS thất bại, hoặc ngược lại).

```java
@Transactional
public void confirmPayment(Payment payment) {
    paymentRepository.save(payment);
    outboxRepository.save(new OutboxEvent("PAYMENT_SUCCEEDED", payment.toJson()));
    // Cả 2 dòng trên cùng 1 transaction Postgres — hoặc cùng thành công, hoặc cùng rollback
}
```

```
outbox_events table:
| id | event_type        | payload | status  | created_at |
|----|--------------------|---------|---------|------------|
| 1  | PAYMENT_SUCCEEDED  | {...}   | PENDING | ...        |
```

**Relay job** (scheduled task, chạy mỗi vài giây): quét row `status = PENDING` → gửi lên SQS → cập nhật `status = SENT`. Đảm bảo *at-least-once delivery*, không mất event kể cả khi relay job crash giữa chừng.

### 4.3. Hủy lịch / hoàn tiền

```
User hoặc Shop Manager hủy booking
  → booking-service đổi status = CANCELLING
  → gọi payment-service thực hiện Stripe refund
  → payment-service refund xong → publish "REFUND_SUCCEEDED"
  → booking-service đổi status = CANCELLED, nhả slot
  → notification-service gửi email hủy lịch
```

---

## 5. Xử lý concurrency (Redis)

### 5.1. Distributed lock chống double-booking

```
Key:   lock:slot:{slotId}
Value: request-id
TTL:   10 giây
```

Request đến → `SETNX` key này:
- Thành công → giữ được lock → xử lý tạo booking
- Thất bại → trả lỗi "slot đang được giữ bởi người khác, vui lòng thử lại"

TTL 10 giây để tránh deadlock nếu service crash giữa chừng chưa kịp release lock.

### 5.2. Cache slot trống

Cache kết quả "slot trống của shop X ngày Y" vào Redis, TTL ngắn (30s–1 phút). Khi có booking mới confirm → invalidate cache key liên quan. TTL ngắn đóng vai trò "safety net" vì việc invalidate thủ công giữa nhiều service dễ sót.

---

## 6. Realtime (SSE)

- `realtime-service` subscribe event nội bộ (từ SQS hoặc gọi trực tiếp), push xuống client qua **Server-Sent Events** (`EventSource` ở FE) — chọn SSE thay vì WebSocket vì chỉ cần luồng 1 chiều server → client, đơn giản hơn, không cần thư viện riêng.
- Dùng cho: Shop Manager thấy booking mới ngay lập tức; slot chuyển trạng thái "đã đặt" realtime cho tất cả user đang xem trang đó.

---

## 7. Bảo mật

- Access token: sống ngắn (15 phút), chứa `role` + `shopIds`.
- Refresh token: sống dài (7 ngày), lưu trong DB (`refresh_tokens`) để có thể revoke khi logout hoặc khi admin thay đổi quyền.
- Chỉ Admin được tạo Shop Manager và gán shop — không có luồng tự leo thang quyền.
- Method-level authorization (`@PreAuthorize`) kết hợp check `shopIds` trong token để đảm bảo Shop Manager chỉ thao tác được trên shop mình quản lý.

---

## 8. Hạ tầng AWS (bản hiện tại — tối ưu cho ~$100 credit còn lại, 6 tháng)

| Thành phần | Lựa chọn | Lý do |
|---|---|---|
| Compute | **1 EC2 (t3.micro/t3.small) chạy Docker Compose** cho toàn bộ service | Rẻ hơn ECS Fargate nhiều lần khi tính theo giờ cố định thay vì theo tổng vCPU từng service |
| Database | **1 RDS PostgreSQL instance**, mỗi service 1 schema | Tiết kiệm chi phí so với nhiều instance, vẫn giữ nguyên tắc tách biệt logic |
| Cache/Lock | **Redis chạy container ngay trên EC2** | Tránh chi phí cố định của ElastiCache (~10-12$/tháng) |
| Networking | **Public subnet, không dùng NAT Gateway** | NAT Gateway tốn ~33$/tháng chỉ để đứng đó — không đáng với ngân sách hiện tại |
| API Gateway | **Spring Cloud Gateway** (chạy như 1 service trong Docker Compose) | Thay ALB để tránh chi phí cố định (~16-25$/tháng), đồng thời custom được logic (rate limit, transform) |
| Domain | Dùng domain mặc định của CloudFront (không mua domain riêng) | Không có domain sẵn, Route 53 hosted zone tốn phí định kỳ |
| Static hosting (FE) | S3 + CloudFront | Chi phí rất thấp, gần như free ở quy mô nhỏ |
| Message queue | SQS + DLQ | Gần như free (1 triệu request/tháng free vĩnh viễn) |
| Email | **Mailtrap khi dev**, chuyển sang SES khi cần demo thật | Tránh vướng SES sandbox mode lúc code |
| Secrets | AWS Secrets Manager | Lưu DB password, Stripe key, JWT secret — không hardcode |
| Giám sát chi phí | **AWS Budget Alert ở mức 50$ và 80$** | Vì tài khoản chỉ còn ~$100 credit trong 6 tháng, cần cảnh báo sớm để tránh tài khoản bị đóng đột ngột |

---

## 9. Terraform (Infrastructure as Code)

Chỉ 1 environment (`dev`) — không cần `prod` cho project cá nhân.

**Modules chính:**
- `network`: VPC, public subnet, security groups
- `ec2`: instance chạy Docker Compose, IAM role, key pair
- `rds`: 1 instance Postgres, subnet group
- `sqs`: queues + DLQ
- `s3-cloudfront`: hosting FE static
- `ecr`: registry cho từng service image
- `iam`: least-privilege roles cho EC2 (đọc secrets, gửi/nhận SQS, pull ECR)
- `monitoring`: CloudWatch log groups, Budget alert

**State management:**
```hcl
terraform {
  backend "s3" {
    bucket         = "your-tf-state-bucket"
    key            = "booking-platform/dev/terraform.tfstate"
    region         = "ap-southeast-1"
    dynamodb_table = "terraform-locks"
    encrypt        = true
  }
}
```
State lưu trên S3, lock bằng DynamoDB table — tránh 2 lần `apply` cùng lúc gây conflict.

---

## 10. CI/CD (GitHub Actions)

```
Push code → ci-backend.yml / ci-frontend.yml (build + test)
Merge vào main → deploy.yml:
  1. Build Docker image từng service đổi
  2. Push image lên ECR
  3. SSH/SSM vào EC2, pull image mới, docker compose up -d
```

---

## 11. Folder Structure (monorepo)

```
booking-platform/
├── services/
│   ├── auth-service/
│   │   ├── src/main/java/com/bookingplatform/auth/
│   │   │   ├── config/          # SecurityConfig, JwtConfig
│   │   │   ├── controller/
│   │   │   ├── service/
│   │   │   ├── repository/
│   │   │   ├── entity/
│   │   │   ├── dto/
│   │   │   └── security/        # JwtAuthFilter, CustomUserDetails
│   │   ├── src/main/resources/
│   │   │   ├── application.yml
│   │   │   └── db/migration/    # Flyway scripts
│   │   ├── src/test/
│   │   ├── Dockerfile
│   │   └── pom.xml
│   │
│   ├── venue-service/            # cấu trúc tương tự auth-service
│   ├── booking-service/
│   │   └── .../outbox/           # OutboxEvent entity, relay job (polling)
│   ├── payment-service/
│   │   └── .../stripe/           # StripeClient, WebhookController
│   ├── notification-service/
│   ├── realtime-service/         # SSE endpoint, event subscriber
│   └── gateway-service/          # Spring Cloud Gateway
│
├── frontend/
│   ├── src/
│   │   ├── components/
│   │   │   └── ui/               # Shadcn components
│   │   ├── features/
│   │   │   ├── auth/
│   │   │   ├── booking/
│   │   │   ├── shop-manager/
│   │   │   └── admin/
│   │   ├── hooks/
│   │   ├── lib/                  # api client, utils
│   │   ├── pages/
│   │   └── routes/
│   ├── public/
│   ├── package.json
│   └── vite.config.ts
│
├── terraform/
│   ├── environments/
│   │   └── dev/
│   │       ├── main.tf
│   │       ├── variables.tf
│   │       └── terraform.tfvars
│   ├── modules/
│   │   ├── network/
│   │   ├── ec2/
│   │   ├── rds/
│   │   ├── sqs/
│   │   ├── s3-cloudfront/
│   │   ├── ecr/
│   │   ├── iam/
│   │   └── monitoring/
│   └── backend.tf
│
├── .github/
│   └── workflows/
│       ├── ci-backend.yml
│       ├── ci-frontend.yml
│       └── deploy.yml
│
├── docker-compose.yml             # chạy local: postgres, redis, localstack (mock SQS/S3)
├── docs/
│   ├── architecture.md            # file này
│   ├── api-contracts/             # OpenAPI spec từng service
│   └── adr/                       # Architecture Decision Records
└── README.md
```

---

## 12. Quyết định kỹ thuật & lý do (ADR-style)

| Quyết định | Lý do |
|---|---|
| Saga choreography (không orchestration) | Số bước trong luồng booking/payment còn ít, choreography đủ đơn giản, không cần thêm 1 service điều phối trung tâm |
| Outbox pattern cho booking-service và payment-service | Đảm bảo ghi DB và publish event luôn đồng bộ, tránh dual-write problem |
| Outbox relay = polling job (không CDC/Debezium) | Quy mô event thấp, polling đơn giản là đủ, tránh thêm hạ tầng nặng |
| Redis SETNX lock (không dùng Postgres advisory lock) | Tách biệt hoàn toàn khỏi tầng DB, dễ scale ngang, TTL tự động tránh deadlock |
| SSE (không WebSocket) | Chỉ cần luồng 1 chiều server → client, đơn giản hơn, không cần quản lý connection 2 chiều |
| Spring Cloud Gateway (không ALB) | Custom được logic gateway bằng Java, tránh chi phí cố định của ALB |
| 1 RDS instance, nhiều schema (không nhiều instance/database) | Tiết kiệm chi phí, vẫn giữ tách biệt logic giữa các service |
| EC2 + Docker Compose (không ECS Fargate) | Rẻ hơn nhiều khi tính theo giờ cố định, phù hợp ngân sách credit còn lại |
| Không dùng NAT Gateway, chạy public subnet | NAT Gateway tốn phí cố định cao (~33$/tháng), không cần thiết ở quy mô này |
| Không mua domain riêng | Không có domain sẵn, tránh phí Route 53 hosted zone định kỳ |
| Chỉ 1 Terraform environment (dev) | Project cá nhân, không cần tách dev/prod phức tạp |
| Admin tạo shop + user trực tiếp (không có luồng duyệt) | Đơn giản hóa nghiệp vụ, phù hợp mô hình admin kiểm soát toàn bộ onboarding |

---

## 13. Hướng mở rộng tương lai (Scale-up path)

Khi có ngân sách/traffic thật, có thể nâng cấp theo đúng hướng đã cân nhắc ban đầu nhưng tạm gác vì giới hạn credit:

| Hiện tại | Khi scale-up |
|---|---|
| EC2 + Docker Compose | ECS Fargate (hoặc EKS nếu cần orchestration phức tạp hơn) |
| Redis tự host trên EC2 | ElastiCache (managed, có failover) |
| Spring Cloud Gateway đơn | Thêm ALB phía trước + NAT Gateway, multi-AZ cho high availability |
| Domain mặc định CloudFront | Custom domain qua Route 53 + ACM |
| Mailtrap | SES production access (bỏ giới hạn sandbox) |
| Terraform chỉ dev | Thêm environment `prod`, tách state riêng |
| RDS 1 instance, nhiều schema | Tách hẳn ra nhiều RDS instance riêng khi 1 service cần scale độc lập, hoặc thêm read replica cho service đọc nhiều |

Ngoài ra, có thể cân nhắc thêm khi hệ thống lớn hơn: WAF chống bot, blue/green deployment, AI booking assistant, contract testing giữa các service — những hướng đã bàn nhưng nằm ngoài scope hiện tại.
