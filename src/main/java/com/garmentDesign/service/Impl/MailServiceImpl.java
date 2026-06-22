package com.garmentDesign.service.Impl;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.garmentDesign.service.MailService;

@Service
public class MailServiceImpl implements MailService {

    private final JavaMailSender mailSender;

    public MailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendSubscribeSuccessEmail(String email) {

        SimpleMailMessage message =
                new SimpleMailMessage();

        message.setTo(email);

        message.setSubject(
                "Đăng ký nhận tin thành công"
        );

        message.setText(
                """
                Xin chào,

                Cảm ơn bạn đã đăng ký nhận tin từ HoaTran maymac.

                Chúng tôi sẽ gửi đến bạn:
                - Tin tức mới nhất
                - Ưu đãi dịch vụ
                - Cập nhật kỹ thuật may mặc

                Trân trọng,
                HoaTran maymac
                """
        );

        mailSender.send(message);
    }
}