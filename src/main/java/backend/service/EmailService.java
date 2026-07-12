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

    public void sendLockoutWarningEmail(String toEmail) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("PrepAce Account Locked Warning");
        message.setText("Your PrepAce account has been temporarily locked for 15 minutes due to 5 consecutive failed login attempts.");
        try {
            mailSender.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendPasswordChangeNotification(String toEmail) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("PrepAce Password Changed Notification");
        message.setText("Your account password was successfully changed. If you did not perform this change, please contact support immediately.");
        try {
            mailSender.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendResetConfirmationEmail(String toEmail) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("PrepAce Password Reset Successfully");
        message.setText("Your password has been successfully reset. You can now log in using your new password.");
        try {
            mailSender.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
