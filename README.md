# 🧵 Garment Design Server — HoaTran

Hệ thống Backend phục vụ quản lý dịch vụ thiết kế may mặc, khách hàng, đơn hàng và xác thực người dùng.

Backend system for garment design service management, customer management, order processing, and user authentication.

---

## 📖 Giới thiệu | Introduction

### 🇻🇳 Tiếng Việt

**HoaTran Garment Design Server** là ứng dụng Backend thuộc hệ sinh thái Garment Design.

Dự án được xây dựng nhằm:

* Quản lý người dùng và phân quyền
* Hỗ trợ đăng nhập đa phương thức
* Quản lý dịch vụ ngành may mặc
* Quản lý khách hàng
* Theo dõi đơn hàng
* Hỗ trợ tích hợp Mobile App và Frontend Web
* Cung cấp RESTful API cho toàn bộ hệ thống

Hệ thống được thiết kế theo kiến trúc:

* REST API
* JWT Authentication
* Spring Security
* SQL Server Database

### 🇺🇸 English

**HoaTran Garment Design Server** is the backend application of the Garment Design ecosystem.

The project is designed to:

* Manage users and authorization
* Support multiple authentication methods
* Manage garment-related services
* Handle customer information
* Process service orders
* Integrate with Web and Mobile applications
* Provide RESTful APIs for the entire system

The system is built using:

* REST API Architecture
* JWT Authentication
* Spring Security
* SQL Server Database

---

## 🚀 Chức năng | Features

| 🇻🇳 Tiếng Việt                 | 🇺🇸 English               |
| ------------------------------- | -------------------------- |
| Đăng ký tài khoản               | User Registration          |
| Đăng nhập Email & Password      | Email & Password Login     |
| Đăng nhập Google                | Google Authentication      |
| Đăng nhập bằng số điện thoại    | Phone Authentication       |
| Quên mật khẩu bằng OTP          | OTP Password Recovery      |
| JWT Authentication              | JWT Authentication         |
| Spring Security                 | Spring Security            |
| Quản lý người dùng              | User Management            |
| Phân quyền Admin / Staff / User | Role-Based Authorization   |
| Quản lý dịch vụ may mặc         | Garment Service Management |
| Quản lý khách hàng              | Customer Management        |
| Quản lý đơn hàng                | Order Management           |
| Upload tài liệu đơn hàng        | Order File Upload          |
| Theo dõi trạng thái đơn hàng    | Order Status Tracking      |

---

## 🛠 Tech Stack

### Backend

| Technology      | Version | Description                    |
| --------------- | ------- | ------------------------------ |
| Java            | 21      | Programming Language           |
| Spring Boot     | 3.x     | Backend Framework              |
| Spring Security | Latest  | Authentication & Authorization |
| Spring Data JPA | Latest  | ORM Framework                  |
| JWT             | Latest  | Authentication Token           |
| Maven           | Latest  | Dependency Management          |

### Database

| Technology      | Description      |
| --------------- | ---------------- |
| SQL Server      | Primary Database |
| JPA / Hibernate | ORM Layer        |

---

## 🔐 Authentication System

### 🇻🇳 Tiếng Việt

Hệ thống xác thực hỗ trợ:

* Đăng ký tài khoản
* Đăng nhập Local
* Đăng nhập Google
* Đăng nhập Số điện thoại
* Quên mật khẩu
* OTP Verification
* JWT Authentication
* Refresh Token
* Route Protection

### 🇺🇸 English

Authentication features include:

* User Registration
* Local Authentication
* Google Authentication
* Phone Authentication
* Forgot Password
* OTP Verification
* JWT Authentication
* Refresh Token
* Protected APIs

---

## 🗄️ Database Design

### Core Modules

| Module              | Description                |
| ------------------- | -------------------------- |
| Users               | User Information           |
| Roles               | Authorization Roles        |
| User Auth Providers | Authentication Providers   |
| Services            | Garment Services           |
| Service Categories  | Service Categories         |
| Orders              | Customer Orders            |
| Order Files         | Uploaded Order Files       |
| Customer Addresses  | Customer Factory Addresses |

### Supported Roles

| Role  |
| ----- |
| Admin |
| Staff |
| User  |

---

## ⚙️ Bắt đầu nhanh | Getting Started

### Requirements

* Java 21+
* Maven 3.9+
* SQL Server 2019+
* Git

### Clone Repository

```bash
git clone https://github.com/Ke1Tran666/garmentDesign.git
```

```bash
cd garmentDesign
```

---

### Database Configuration

Update:

```properties
src/main/resources/application.properties
```

Example:

```properties
spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=HoaTranMayMac;encrypt=true;trustServerCertificate=true

spring.datasource.username=sa
spring.datasource.password=your_password

spring.jpa.hibernate.ddl-auto=update
```

---

### Run Application

```bash
mvn spring-boot:run
```

Application runs at:

```text
http://localhost:8080
```

---

## 📂 Cấu trúc dự án | Project Structure

```text
src/main/java
│
├── controller
│
├── service
│
├── service/impl
│
├── repository
│
├── entity
│
├── dto
│
├── config
│
├── security
│
├── exception
│
└── util
```

---

## 🌱 Git Workflow

```bash
git checkout -b feature/auth
```

```bash
git add .
git commit -m "feat(auth): complete authentication module"
git push origin feature/auth
```

Sau khi hoàn thành:

```text
Create Pull Request
feature/auth → main
```

---

## 📌 Roadmap

### ✅ Completed

* Spring Boot Setup
* SQL Server Integration
* JWT Authentication
* User Authentication
* Google Login
* Forgot Password
* Security Configuration

### 🚧 In Progress

* User Profile API
* Service Management API
* Order Management API
* File Upload System

### 🔮 Future Plans

* Email Verification
* SMS OTP Verification
* Payment Integration
* Invoice Management
* Admin Dashboard APIs
* Analytics & Reporting APIs

---

## 🤝 Đóng góp | Contributing

1. Tạo feature branch

```bash
git checkout -b feature/your-feature
```

2. Thực hiện thay đổi

3. Chạy test và kiểm tra code

```bash
mvn test
```

4. Tạo Pull Request

---

## 👨‍💻 Tác giả | Author

### Trần Hữu Hùng (Kei Tran)

* Java Backend Developer
* Frontend Developer
* UI/UX Enthusiast

GitHub:
https://github.com/Ke1Tran666

Portfolio:
https://ke1tran666.github.io/portfolio/

---

## 📄 Giấy phép | License

### 🇻🇳 Tiếng Việt

Dự án hiện đang ở chế độ Private và được phát triển cho mục đích học tập, nghiên cứu và xây dựng portfolio.

### 🇺🇸 English

This project is currently private and developed for educational, research, and portfolio purposes.

---

## 📬 Hỗ trợ | Support

Nếu bạn có câu hỏi hoặc đề xuất, vui lòng liên hệ tác giả dự án.

If you have any questions or suggestions, please feel free to contact the project author.
