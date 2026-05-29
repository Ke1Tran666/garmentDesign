# 🧵 Garment Design System

> Garment Service Management Platform built with ReactJS, Spring Boot and SQL Server.

---

## 📖 Giới thiệu | Introduction

| 🇻🇳 Tiếng Việt                                                                                                                             | 🇺🇸 English                                                                                                                     |
| ----------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------ |
| Hệ thống quản lý dịch vụ ngành may mặc, hỗ trợ khách hàng đặt dịch vụ, quản lý đơn hàng, quản lý người dùng và theo dõi tiến độ sản xuất. | A garment service management platform that allows customers to order services, manage orders, users, and production workflows. |

---

## 🚀 Tính năng | Features

| 🇻🇳 Tiếng Việt                   | 🇺🇸 English                       |
| ------------------------------- | -------------------------------- |
| Đăng nhập bằng Email & Mật khẩu | Login with Email & Password      |
| Đăng nhập bằng Google           | Google Authentication            |
| Quên mật khẩu bằng OTP          | OTP Password Recovery            |
| Phân quyền Admin / Staff / User | Role-based Authorization         |
| Quản lý hồ sơ người dùng        | User Profile Management          |
| Quản lý khách hàng              | Customer Management              |
| Quản lý dịch vụ may mặc         | Garment Service Management       |
| Quản lý đơn hàng                | Order Management                 |
| Theo dõi trạng thái đơn hàng    | Order Status Tracking            |
| Quản lý giá dịch vụ             | Service Pricing Management       |
| Hỗ trợ nhiều địa chỉ xưởng      | Multiple Factory Address Support |

---

## 🛠 Công nghệ sử dụng | Technology Stack

### Frontend

| Công nghệ        | Technology       |
| ---------------- | ---------------- |
| ReactJS          | ReactJS          |
| React Router DOM | React Router DOM |
| Tailwind CSS     | Tailwind CSS     |
| Axios            | Axios            |
| Lucide React     | Lucide React     |
| Google OAuth     | Google OAuth     |

### Backend

| Công nghệ          | Technology         |
| ------------------ | ------------------ |
| Java 21            | Java 21            |
| Spring Boot        | Spring Boot        |
| Spring Security    | Spring Security    |
| JWT Authentication | JWT Authentication |
| Spring Data JPA    | Spring Data JPA    |
| Maven              | Maven              |

### Database

| Công nghệ  | Technology |
| ---------- | ---------- |
| SQL Server | SQL Server |

---

## 📂 Cấu trúc dự án | Project Structure

```text
garmentDesign
│
├── frontend
│   ├── components
│   ├── layouts
│   ├── pages
│   │   └── Auth
│   ├── services
│   └── routes
│
├── backend
│   ├── controller
│   ├── service
│   ├── repository
│   ├── entity
│   ├── dto
│   ├── security
│   └── config
│
└── database
```

---

## 🔐 Chức năng xác thực | Authentication Features

| 🇻🇳 Tiếng Việt         | 🇺🇸 English         |
| --------------------- | ------------------ |
| Đăng ký tài khoản     | User Registration  |
| Đăng nhập             | User Login         |
| Đăng nhập Google      | Google Login       |
| Quên mật khẩu         | Forgot Password    |
| OTP xác thực          | OTP Verification   |
| JWT Authentication    | JWT Authentication |
| Spring Security       | Spring Security    |
| Phân quyền người dùng | User Authorization |

---

## 🌱 Quy trình Git | Git Workflow

```bash
git checkout -b feature/auth

git add .

git commit -m "feat(auth): complete authentication module"

git push origin feature/auth
```

Sau khi hoàn thành:

```bash
Create Pull Request
feature/auth → main
```

---

## 📌 Lộ trình phát triển | Roadmap

### ✅ Hoàn thành | Completed

- Authentication Module
- Google Login
- Forgot Password
- JWT Security
- Responsive Authentication UI

### 🚧 Đang phát triển | In Progress

- User Profile
- Service Management
- Order Management

### 🔮 Kế hoạch tương lai | Future Plans

- Phone OTP Authentication
- Email Verification
- Payment Integration
- Admin Dashboard
- Invoice Management
- Production Tracking
- Analytics & Reports

---

## 👨‍💻 Tác giả | Author

**Kei Tran (Trần Hữu Hùng)**

Frontend Developer • Java Backend Developer • UI/UX Enthusiast

GitHub:
https://github.com/Ke1Tran666

Portfolio:
https://ke1tran666.github.io/portfolio/

---

## 📄 License

This project is developed for educational, portfolio and learning purposes.
