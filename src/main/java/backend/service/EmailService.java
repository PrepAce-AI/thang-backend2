package backend.service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    @Autowired
    private JavaMailSender mailSender;

    public void sendOtp(String toEmail, String otp){
        try {
            SimpleMailMessage mailMessage = new SimpleMailMessage();

            mailMessage.setTo(toEmail);
            mailMessage.setSubject("PrepAce Email Verification");
            mailMessage.setText("Your Verification Code Is: " + otp);

            mailSender.send(mailMessage);

            System.out.println("✅ MAIL SENT SUCCESS");

        } catch (Exception e) {
            System.out.println("❌ MAIL FAILED:");
            e.printStackTrace();
        }
    }

    public void sendVerificationEmail(String toEmail, String otp){
        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(toEmail);
        message.setSubject("PrepAce Email Verification");
        message.setText("Your OTP Code is: " + otp);

        mailSender.send(message);
    }
}
