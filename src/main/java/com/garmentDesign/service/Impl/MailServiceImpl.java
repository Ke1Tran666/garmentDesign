package com.garmentDesign.service.Impl;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.garmentDesign.dto.mail.ContactRequest;
import com.garmentDesign.service.MailService;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class MailServiceImpl implements MailService {

	private final JavaMailSender mailSender;

	private static final String LANDING_PAGE_EMAIL = "hoatranmaymac@gmail.com";

	public MailServiceImpl(JavaMailSender mailSender) {
		this.mailSender = mailSender;
	}

	@Override
	public void sendSubscribeSuccessEmail(String email) {

		try {
			MimeMessage mimeMessage = mailSender.createMimeMessage();

			MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

			helper.setTo(email);
			helper.setSubject("Đăng ký nhận tin thành công - HoaTran maymac");

			String htmlContent = """
					<!DOCTYPE html>
					<html lang="vi">
					<body style="margin:0;padding:0;background:#f4f7fb;font-family:Arial,sans-serif;color:#111827;">

					    <div style="max-width:680px;margin:0 auto;padding:32px 16px;">

					        <div style="background:#ffffff;border-radius:18px;overflow:hidden;border:1px solid #e5e7eb;box-shadow:0 10px 30px rgba(15,23,42,0.08);">

					            <div style="background:#0192F5;padding:32px;color:#ffffff;text-align:center;">
					                <div style="font-size:14px;letter-spacing:2px;text-transform:uppercase;">
					                    HoaTran maymac
					                </div>

					                <h1 style="margin:12px 0 0;font-size:28px;font-weight:700;">
					                    Đăng ký thành công
					                </h1>

					                <p style="margin:12px 0 0;font-size:15px;opacity:0.95;">
					                    Cảm ơn bạn đã đăng ký nhận tin từ chúng tôi.
					                </p>
					            </div>

					            <div style="padding:40px 32px;">

					                <div style="text-align:center;margin-bottom:30px;">
					                    <div style="
					                        width:72px;
					                        height:72px;
					                        line-height:72px;
					                        border-radius:50%%;
					                        background:#e8f4fe;
					                        color:#0192F5;
					                        font-size:36px;
					                        font-weight:bold;
					                        margin:0 auto;">
					                        ✓
					                    </div>
					                </div>

					                <h2 style="
					                    margin:0;
					                    text-align:center;
					                    font-size:22px;
					                    color:#111827;">
					                    Chào mừng bạn đến với HoaTran maymac
					                </h2>

					                <p style="
					                    text-align:center;
					                    color:#6b7280;
					                    line-height:1.8;
					                    margin-top:16px;">
					                    Chúng tôi rất vui khi bạn tham gia danh sách nhận tin.
					                    Bạn sẽ nhận được những thông tin mới nhất về dịch vụ,
					                    sản phẩm và các chương trình ưu đãi đặc biệt.
					                </p>

					                <div style="
					                    margin-top:30px;
					                    background:#f9fafb;
					                    border:1px solid #e5e7eb;
					                    border-radius:14px;
					                    padding:20px;">

					                    <div style="
					                        font-weight:700;
					                        margin-bottom:12px;
					                        color:#111827;">
					                        Bạn sẽ nhận được:
					                    </div>

					                    <ul style="
					                        margin:0;
					                        padding-left:18px;
					                        color:#4b5563;
					                        line-height:1.9;">
					                        <li>Tin tức mới nhất từ HoaTran maymac</li>
					                        <li>Cập nhật dịch vụ và sản phẩm</li>
					                        <li>Ưu đãi dành riêng cho khách hàng đăng ký</li>
					                        <li>Xu hướng thiết kế và kỹ thuật may mặc</li>
					                    </ul>
					                </div>

					                <div style="
					                    text-align:center;
					                    margin-top:32px;">
					                    <a href="https://hoatranmaymac.com"
					                       style="
					                       display:inline-block;
					                       background:#0192F5;
					                       color:#ffffff;
					                       text-decoration:none;
					                       padding:14px 24px;
					                       border-radius:12px;
					                       font-weight:700;">
					                        Truy cập website
					                    </a>
					                </div>

					            </div>

					            <div style="
					                background:#f9fafb;
					                padding:18px 32px;
					                border-top:1px solid #e5e7eb;
					                text-align:center;
					                color:#6b7280;
					                font-size:12px;
					                line-height:1.6;">

					                Email này được gửi tự động từ HoaTran maymac.<br>
					                Nếu bạn không muốn nhận email nữa, vui lòng liên hệ với chúng tôi.

					            </div>

					        </div>

					    </div>

					</body>
					</html>
					""";

			helper.setText(htmlContent, true);

			mailSender.send(mimeMessage);

		} catch (MessagingException e) {
			throw new RuntimeException("Không thể gửi email đăng ký nhận tin.", e);
		}
	}

	@Override
	public void sendContactEmail(ContactRequest request) {
		try {
			MimeMessage mimeMessage = mailSender.createMimeMessage();

			MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

			helper.setTo(LANDING_PAGE_EMAIL);
			helper.setSubject("Yêu cầu liên hệ mới - HoaTran maymac");

			String fullName = safe(request.getFullName());
			String phone = safe(request.getPhone());
			String email = safe(request.getEmail());
			String serviceCode = safe(request.getServiceCode());
			String customerMessage = safe(request.getMessage());

			String htmlContent = """
					<!DOCTYPE html>
					<html lang="vi">
					<body style="margin:0;padding:0;background:#f4f7fb;font-family:Arial,sans-serif;color:#111827;">
					  <div style="max-width:680px;margin:0 auto;padding:32px 16px;">

					    <div style="background:#ffffff;border-radius:18px;overflow:hidden;border:1px solid #e5e7eb;box-shadow:0 10px 30px rgba(15,23,42,0.08);">

					      <div style="background:#0192F5;padding:28px 32px;color:#ffffff;">
					        <div style="font-size:13px;letter-spacing:1.5px;text-transform:uppercase;opacity:0.9;">
					          HoaTran maymac
					        </div>
					        <h1 style="margin:8px 0 0;font-size:24px;font-weight:700;line-height:1.3;">
					          Yêu cầu liên hệ mới
					        </h1>
					        <p style="margin:8px 0 0;font-size:14px;opacity:0.95;line-height:1.6;">
					          Có khách hàng vừa gửi thông tin liên hệ từ landing page.
					        </p>
					      </div>

					      <div style="padding:32px;">
					        <h2 style="margin:0 0 20px;font-size:20px;color:#111827;">
					          Thông tin khách hàng
					        </h2>

					        <table style="width:100%%;border-collapse:collapse;">
					          <tr>
					            <td style="padding:12px 0;color:#6b7280;width:160px;border-bottom:1px solid #f3f4f6;">Họ tên</td>
					            <td style="padding:12px 0;font-weight:600;border-bottom:1px solid #f3f4f6;">%s</td>
					          </tr>
					          <tr>
					            <td style="padding:12px 0;color:#6b7280;border-bottom:1px solid #f3f4f6;">Số điện thoại</td>
					            <td style="padding:12px 0;font-weight:600;border-bottom:1px solid #f3f4f6;">
					              <a href="tel:%s" style="color:#111827;text-decoration:none;">%s</a>
					            </td>
					          </tr>
					          <tr>
					            <td style="padding:12px 0;color:#6b7280;border-bottom:1px solid #f3f4f6;">Email</td>
					            <td style="padding:12px 0;font-weight:600;border-bottom:1px solid #f3f4f6;">
					              <a href="mailto:%s" style="color:#0192F5;text-decoration:none;">%s</a>
					            </td>
					          </tr>
					          <tr>
					            <td style="padding:12px 0;color:#6b7280;">Dịch vụ cần</td>
					            <td style="padding:12px 0;">
					              <span style="display:inline-block;background:#e8f4fe;color:#0170c2;padding:7px 13px;border-radius:999px;font-weight:700;font-size:13px;">
					                %s
					              </span>
					            </td>
					          </tr>
					        </table>

					        <div style="margin-top:28px;">
					          <div style="font-size:14px;color:#6b7280;margin-bottom:10px;font-weight:600;">
					            Nội dung yêu cầu
					          </div>
					          <div style="background:#f9fafb;border:1px solid #e5e7eb;border-radius:14px;padding:18px;line-height:1.7;color:#111827;white-space:pre-line;">
					            %s
					          </div>
					        </div>

					        <div style="margin-top:30px;">
					          <a href="mailto:%s" style="display:inline-block;background:#0192F5;color:#ffffff;text-decoration:none;padding:13px 20px;border-radius:12px;font-weight:700;">
					            Phản hồi khách hàng
					          </a>
					        </div>
					      </div>

					      <div style="background:#f9fafb;padding:18px 32px;color:#6b7280;font-size:12px;border-top:1px solid #e5e7eb;line-height:1.5;">
					        Email này được gửi tự động từ website HoaTran maymac.
					      </div>

					    </div>
					  </div>
					</body>
					</html>
					"""
					.formatted(fullName, phone, phone, email, email, serviceCode, customerMessage, email);

			helper.setText(htmlContent, true);

			mailSender.send(mimeMessage);

		} catch (MessagingException e) {
			throw new RuntimeException("Không thể gửi email liên hệ.", e);
		}
	}

	private String safe(String value) {
		if (value == null || value.isBlank()) {
			return "Chưa cung cấp";
		}

		return value.trim();
	}

	@Override
	public void sendOtpEmail(String email, String otp, String purpose) {

		try {
			MimeMessage mimeMessage = mailSender.createMimeMessage();

			MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");

			helper.setTo(email);

			boolean resetPassword = "reset-password".equals(purpose);

			String title = resetPassword ? "Đặt lại mật khẩu" : "Xác thực địa chỉ email";

			helper.setSubject(title + " - HoaTran maymac");

			String htmlContent = """
					<!DOCTYPE html>
					<html lang="vi">
					<body style="
					    margin:0;
					    padding:0;
					    background:#f4f7fb;
					    font-family:Arial,sans-serif;
					    color:#111827;
					">
					    <div style="
					        max-width:600px;
					        margin:0 auto;
					        padding:32px 16px;
					    ">
					        <div style="
					            background:#ffffff;
					            border:1px solid #e5e7eb;
					            border-radius:16px;
					            overflow:hidden;
					            box-shadow:0 10px 30px rgba(15,23,42,0.08);
					        ">
					            <div style="
					                background:#0192F5;
					                color:#ffffff;
					                padding:28px;
					                text-align:center;
					            ">
					                <div style="
					                    font-size:13px;
					                    letter-spacing:2px;
					                    text-transform:uppercase;
					                    margin-bottom:10px;
					                ">
					                    HoaTran maymac
					                </div>

					                <h1 style="
					                    margin:0;
					                    font-size:24px;
					                ">
					                    %s
					                </h1>
					            </div>

					            <div style="
					                padding:36px 28px;
					                text-align:center;
					            ">
					                <p style="
					                    color:#4b5563;
					                    line-height:1.7;
					                ">
					                    Mã OTP của bạn là:
					                </p>

					                <div style="
					                    margin:24px auto;
					                    padding:18px 24px;
					                    background:#eff6ff;
					                    color:#0192F5;
					                    border-radius:12px;
					                    font-size:32px;
					                    font-weight:700;
					                    letter-spacing:8px;
					                ">
					                    %s
					                </div>

					                <p style="
					                    color:#6b7280;
					                    line-height:1.7;
					                ">
					                    Mã có hiệu lực trong 5 phút.<br>
					                    Bạn có tối đa 5 lần nhập sai.
					                </p>

					                <p style="
					                    color:#dc2626;
					                    font-size:14px;
					                    font-weight:600;
					                ">
					                    Không cung cấp mã này cho bất kỳ ai.
					                </p>

					                <p style="
					                    margin-top:28px;
					                    color:#9ca3af;
					                    font-size:13px;
					                    line-height:1.6;
					                ">
					                    Nếu bạn không thực hiện yêu cầu này,
					                    hãy bỏ qua email.
					                </p>
					            </div>
					        </div>
					    </div>
					</body>
					</html>
					""".formatted(title, otp);

			helper.setText(htmlContent, true);

			mailSender.send(mimeMessage);

		} catch (Exception exception) {
			throw new RuntimeException("Không thể gửi mã OTP qua email", exception);
		}
	}
}