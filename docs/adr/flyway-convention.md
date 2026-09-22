# Flyway Convention

Áp dụng cho mọi backend service trong monorepo. Mỗi service tự quản lý migration của **schema riêng** — không đụng schema của service khác dù chung 1 Postgres instance.

---

## 1. Mapping service → schema

| Service | Schema |
|---|---|
| `auth-service` | `auth_schema` |
| `venue-service` | `venue_schema` |
| `booking-service` | `booking_schema` |
| `payment-service` | `payment_schema` |

`notification-service`, `realtime-service`, `gateway-service` không có schema / không dùng Flyway.

Schema rỗng (`CREATE SCHEMA ...`) đã tạo ở Phase 0. Flyway chỉ tạo/sửa **bảng bên trong** schema đó.

---

## 2. Vị trí file migration

```
services/<service-name>/src/main/resources/db/migration/
```

Ví dụ:

```
services/auth-service/src/main/resources/db/migration/
  V1__create_users_table.sql
  V2__create_refresh_tokens_table.sql
  V3__create_user_shop_mapping_table.sql
```

---

## 3. Naming file

```
V{version}__{mo_ta}.sql
```

| Thành phần | Quy tắc |
|---|---|
| `V` | Prefix bắt buộc (versioned migration) |
| `{version}` | Số nguyên tăng dần: `1`, `2`, `3`… (không zero-pad: dùng `V1` không dùng `V01`) |
| `__` | Hai dấu gạch dưới (bắt buộc) |
| `{mo_ta}` | snake_case, tiếng Anh ngắn gọn, mô tả thay đổi |

Ví dụ hợp lệ:

- `V1__create_users_table.sql`
- `V2__create_refresh_tokens_table.sql`
- `V3__add_status_to_bookings.sql`

Không dùng:

- `V1_create_users.sql` (thiếu `__`)
- `v1__create_users.sql` (sai chữ `V` hoa)
- `V1__CreateUsers.sql` (không dùng PascalCase)

---

## 4. Nội dung migration

- Mỗi file là SQL thuần, idempotent trong phạm vi version (Flyway không chạy lại version đã apply).
- Luôn qualify schema rõ ràng hoặc dựa vào `default-schema` của service — **không** tạo object ở schema khác.
- Không tạo `FOREIGN KEY` sang bảng thuộc schema của service khác (chỉ logical FK, validate ở application).
- FK **trong cùng schema** thì được (ví dụ `refresh_tokens.user_id` → `users.id`).

Ví dụ:

```sql
-- V1__create_users_table.sql  (auth-service)
CREATE TABLE users (
    id            UUID PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name     VARCHAR(255),
    phone         VARCHAR(20),
    role          VARCHAR(20)  NOT NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP
);

CREATE INDEX idx_users_role ON users (role);
```

---

## 5. Config Spring Boot (`application.yml`)

Mỗi service cấu hình Flyway + JPA trỏ đúng schema của mình.

```yaml
# Ví dụ: auth-service
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/booking_platform
    username: booking
    password: booking

  flyway:
    enabled: true
    schemas: auth_schema
    default-schema: auth_schema
    locations: classpath:db/migration

  jpa:
    hibernate:
      ddl-auto: validate   # schema do Flyway quản lý, không để Hibernate tự tạo
    properties:
      hibernate:
        default_schema: auth_schema
```

Đổi `auth_schema` thành schema tương ứng khi copy sang service khác.

---

## 6. Luật bắt buộc

1. **Một service = một schema.** Migration của `booking-service` không được `CREATE`/`ALTER` gì trong `auth_schema`, `venue_schema`, …
2. **Không cross-schema FK.** Tham chiếu `user_id`, `shop_id`, `slot_id`, … giữa service là logical FK — kiểm tra qua REST gọi service sở hữu.
3. **Không sửa file migration đã apply** (local đã chạy hoặc đã merge). Muốn đổi → thêm version mới (`V4__...`).
4. **Không dùng `flyway baseline` / repair** trừ khi có lý do rõ và ghi chú trong PR.
5. **Hibernate `ddl-auto` = `validate`** (hoặc `none`) sau khi có Flyway — không dùng `update` / `create`.

---

## 7. Checklist khi thêm service mới có DB

- [ ] Schema tương ứng đã tồn tại trên Postgres
- [ ] Thư mục `src/main/resources/db/migration/` đã tạo
- [ ] `application.yml` set đúng `flyway.schemas` + `hibernate.default_schema`
- [ ] Migration đầu (`V1__...`) khớp thiết kế trong `docs/booking-platform-db-design.md`
- [ ] Không có FK / `CREATE SCHEMA` / object ngoài schema của service
