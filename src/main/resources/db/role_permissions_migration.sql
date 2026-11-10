-- Migration: Dynamic role descriptions and permissions (HoaTranmaymac)
-- Target: SQL Server

IF COL_LENGTH('dbo.Roles', 'description') IS NULL
BEGIN
    ALTER TABLE dbo.Roles
    ADD description NVARCHAR(500) NULL;
END
GO

IF OBJECT_ID('dbo.Permissions', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.Permissions (
        id_permission BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        code VARCHAR(100) NOT NULL UNIQUE,
        name NVARCHAR(150) NOT NULL,
        description NVARCHAR(300) NULL
    );
END
GO

IF OBJECT_ID('dbo.Role_Permissions', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.Role_Permissions (
        id_role BIGINT NOT NULL,
        id_permission BIGINT NOT NULL,
        CONSTRAINT PK_Role_Permissions PRIMARY KEY (id_role, id_permission),
        CONSTRAINT FK_Role_Permissions_Roles FOREIGN KEY (id_role)
            REFERENCES dbo.Roles (id_role) ON DELETE CASCADE,
        CONSTRAINT FK_Role_Permissions_Permissions FOREIGN KEY (id_permission)
            REFERENCES dbo.Permissions (id_permission) ON DELETE CASCADE
    );
END
GO

-- Seed permissions catalog idempotently
DECLARE @SeedPermissions TABLE (
    code VARCHAR(100) NOT NULL,
    name NVARCHAR(150) NOT NULL,
    description NVARCHAR(300) NULL
);

INSERT INTO @SeedPermissions (code, name, description) VALUES
('SYSTEM_ADMIN', N'Quản trị hệ thống', N'Toàn quyền quản trị cấu hình và vận hành hệ thống'),
('ROLE_MANAGE', N'Quản lý vai trò', N'Tạo, chỉnh sửa, xóa và thiết lập phạm vi quyền hạn cho vai trò'),
('USER_MANAGE', N'Quản lý nhân sự & tài khoản', N'Quản lý tài khoản người dùng, phân vai trò và trạng thái hoạt động'),
('SERVICE_PRICING_MANAGE', N'Cấu hình bảng giá dịch vụ', N'Quản lý danh mục dịch vụ thiết kế may đo và bảng giá'),
('ADMIN_PORTAL_ACCESS', N'Cổng vận hành Admin', N'Truy cập khu vực quản trị và vận hành nội bộ'),
('SERVICE_ORDER_PROCESS', N'Xử lý đơn dịch vụ', N'Tiếp nhận, cập nhật tiến độ và bàn giao đơn dịch vụ'),
('DESIGN_FILE_MANAGE', N'Quản lý tệp thiết kế', N'Tải lên, kiểm duyệt và quản lý tệp mẫu thiết kế'),
('CUSTOMER_CARE', N'Chăm sóc khách hàng', N'Tư vấn, phản hồi yêu cầu và hỗ trợ khách hàng'),
('REVIEW_MANAGE', N'Quản lý đánh giá dịch vụ', N'Theo dõi và quản lý đánh giá chất lượng dịch vụ từ khách hàng'),
('CUSTOMER_PORTAL_ACCESS', N'Cổng khách hàng', N'Truy cập cổng khách hàng và quản lý thông tin cá nhân'),
('SERVICE_ORDER_CREATE', N'Đặt đơn dịch vụ', N'Khởi tạo và theo dõi đơn đặt dịch vụ may đo'),
('SERVICE_REVIEW_WRITE', N'Đánh giá dịch vụ', N'Gửi phản hồi và đánh giá sau khi hoàn thành đơn dịch vụ');

INSERT INTO dbo.Permissions (code, name, description)
SELECT s.code, s.name, s.description
FROM @SeedPermissions s
WHERE NOT EXISTS (
    SELECT 1 FROM dbo.Permissions p WHERE p.code = s.code
);
GO

-- Populate default descriptions for initial roles only when missing
UPDATE dbo.Roles
SET description = CASE id_role
    WHEN 1 THEN N'Toàn quyền quản trị hệ thống, phân quyền người dùng, vai trò và cấu hình dịch vụ.'
    WHEN 2 THEN N'Vận hành xử lý đơn dịch vụ may đo, hỗ trợ khách hàng và theo dõi tiến độ thiết kế.'
    WHEN 3 THEN N'Vai trò mặc định cho khách hàng đăng ký mới, sử dụng cổng đặt dịch vụ và quản lý hồ sơ.'
    ELSE description
END
WHERE id_role IN (1, 2, 3)
  AND (description IS NULL OR LTRIM(RTRIM(description)) = N'');
GO

-- Seed initial Role_Permissions mappings for default roles only if that role has no permissions assigned yet
IF EXISTS (SELECT 1 FROM dbo.Roles WHERE id_role = 1)
   AND NOT EXISTS (SELECT 1 FROM dbo.Role_Permissions WHERE id_role = 1)
BEGIN
    INSERT INTO dbo.Role_Permissions (id_role, id_permission)
    SELECT 1, p.id_permission
    FROM dbo.Permissions p
    WHERE p.code IN (
        'SYSTEM_ADMIN',
        'ROLE_MANAGE',
        'USER_MANAGE',
        'SERVICE_PRICING_MANAGE'
    );
END
GO

IF EXISTS (SELECT 1 FROM dbo.Roles WHERE id_role = 2)
   AND NOT EXISTS (SELECT 1 FROM dbo.Role_Permissions WHERE id_role = 2)
BEGIN
    INSERT INTO dbo.Role_Permissions (id_role, id_permission)
    SELECT 2, p.id_permission
    FROM dbo.Permissions p
    WHERE p.code IN (
        'ADMIN_PORTAL_ACCESS',
        'SERVICE_ORDER_PROCESS',
        'CUSTOMER_CARE',
        'DESIGN_FILE_MANAGE'
    );
END
GO

IF EXISTS (SELECT 1 FROM dbo.Roles WHERE id_role = 3)
   AND NOT EXISTS (SELECT 1 FROM dbo.Role_Permissions WHERE id_role = 3)
BEGIN
    INSERT INTO dbo.Role_Permissions (id_role, id_permission)
    SELECT 3, p.id_permission
    FROM dbo.Permissions p
    WHERE p.code IN (
        'CUSTOMER_PORTAL_ACCESS',
        'SERVICE_ORDER_CREATE',
        'SERVICE_REVIEW_WRITE'
    );
END
GO
