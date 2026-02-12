package com.servija.servija.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async
    public void sendVerificationEmail(String toEmail, String userName, String verificationCode) {
        try {
            log.info("===== INICIANDO ENVIO DE EMAIL =====");
            log.info("De: {}", fromEmail);
            log.info("Para: {}", toEmail);
            log.info("Assunto: Verificação de Email - ServiJá");
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Verificação de Email - ServiJá");

            String htmlContent = buildVerificationEmailTemplate(userName, verificationCode);
            helper.setText(htmlContent, true);

            log.info("Enviando mensagem via SMTP...");
            mailSender.send(message);
            log.info("✅ Email de verificação enviado com sucesso para: {}", toEmail);
            System.out.println("✅ Email enviado com sucesso para: " + toEmail);
        } catch (MessagingException e) {
            log.error("❌ Erro MessagingException ao enviar email para {}: {}", toEmail, e.getMessage(), e);
            System.out.println("⚠️ ERRO MessagingException: " + e.getMessage());
            System.out.println("Código de verificação para " + toEmail + ": " + verificationCode);
        } catch (MailException e) {
            log.error("❌ Erro MailException ao enviar email para {}: {}", toEmail, e.getMessage(), e);
            System.out.println("⚠️ ERRO MailException: " + e.getMessage());
            System.out.println("Código de verificação para " + toEmail + ": " + verificationCode);
        } catch (Exception e) {
            log.error("❌ Erro genérico ao enviar email para {}: {}", toEmail, e.getMessage(), e);
            System.out.println("⚠️ ERRO: " + e.getMessage());
            System.out.println("Código de verificação para " + toEmail + ": " + verificationCode);
        }
    }

    @Async
    public void sendPasswordResetEmail(String toEmail, String userName, String resetToken) {
        try {
            log.info("===== INICIANDO ENVIO DE EMAIL DE RECUPERAÇÃO =====");
            log.info("De: {}", fromEmail);
            log.info("Para: {}", toEmail);
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Recuperação de Senha - ServiJá");

            String htmlContent = buildPasswordResetEmailTemplate(userName, resetToken);
            helper.setText(htmlContent, true);

            log.info("Enviando mensagem via SMTP...");
            mailSender.send(message);
            log.info("✅ Email de recuperação de senha enviado com sucesso para: {}", toEmail);
            System.out.println("✅ Email de recuperação enviado com sucesso para: " + toEmail);
        } catch (MessagingException e) {
            log.error("❌ Erro MessagingException ao enviar email de recuperação para {}: {}", toEmail, e.getMessage(), e);
            System.out.println("⚠️ ERRO MessagingException: " + e.getMessage());
            System.out.println("Token de recuperação para " + toEmail + ": " + resetToken);
        } catch (MailException e) {
            log.error("❌ Erro MailException ao enviar email de recuperação para {}: {}", toEmail, e.getMessage(), e);
            System.out.println("⚠️ ERRO MailException: " + e.getMessage());
            System.out.println("Token de recuperação para " + toEmail + ": " + resetToken);
        } catch (Exception e) {
            log.error("❌ Erro genérico ao enviar email de recuperação para {}: {}", toEmail, e.getMessage(), e);
            System.out.println("⚠️ ERRO: " + e.getMessage());
            System.out.println("Token de recuperação para " + toEmail + ": " + resetToken);
        }
    }

    private String buildVerificationEmailTemplate(String userName, String verificationCode) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <style>
                        body {
                            font-family: Arial, sans-serif;
                            line-height: 1.6;
                            color: #333;
                            max-width: 600px;
                            margin: 0 auto;
                            padding: 20px;
                        }
                        .container {
                            background-color: #f9f9f9;
                            border-radius: 10px;
                            padding: 30px;
                            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                        }
                        .header {
                            text-align: center;
                            margin-bottom: 30px;
                        }
                        .logo {
                            font-size: 32px;
                            font-weight: bold;
                            color: #286AD8;
                        }
                        .code-box {
                            background-color: #286AD8;
                            color: white;
                            font-size: 36px;
                            font-weight: bold;
                            text-align: center;
                            padding: 20px;
                            border-radius: 8px;
                            margin: 30px 0;
                            letter-spacing: 8px;
                        }
                        .message {
                            text-align: center;
                            margin-bottom: 20px;
                        }
                        .footer {
                            text-align: center;
                            font-size: 12px;
                            color: #666;
                            margin-top: 30px;
                            padding-top: 20px;
                            border-top: 1px solid #ddd;
                        }
                        .warning {
                            background-color: #fff3cd;
                            border: 1px solid #ffc107;
                            border-radius: 5px;
                            padding: 15px;
                            margin-top: 20px;
                            text-align: center;
                            font-size: 14px;
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <div class="logo">ServiJá</div>
                        </div>
                        
                        <div class="message">
                            <h2>Olá, %s!</h2>
                            <p>Obrigado por se cadastrar no ServiJá.</p>
                            <p>Para confirmar seu endereço de e-mail, utilize o código abaixo:</p>
                        </div>
                        
                        <div class="code-box">
                            %s
                        </div>
                        
                        <div class="message">
                            <p>Este código é válido por <strong>24 horas</strong>.</p>
                            <p>Se você não solicitou este código, por favor ignore este e-mail.</p>
                        </div>
                        
                        <div class="warning">
                            ⚠️ Nunca compartilhe este código com ninguém
                        </div>
                        
                        <div class="footer">
                            <p>Este é um e-mail automático, por favor não responda.</p>
                            <p>&copy; 2025 ServiJá. Todos os direitos reservados.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(userName, verificationCode);
    }

    private String buildPasswordResetEmailTemplate(String userName, String resetCode) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <style>
                        body {
                            font-family: Arial, sans-serif;
                            line-height: 1.6;
                            color: #333;
                            max-width: 600px;
                            margin: 0 auto;
                            padding: 20px;
                        }
                        .container {
                            background-color: #f9f9f9;
                            border-radius: 10px;
                            padding: 30px;
                            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                        }
                        .header {
                            text-align: center;
                            margin-bottom: 30px;
                        }
                        .logo {
                            font-size: 32px;
                            font-weight: bold;
                            color: #286AD8;
                        }
                        .code-box {
                            background-color: #ef4444;
                            color: white;
                            font-size: 36px;
                            font-weight: bold;
                            text-align: center;
                            padding: 20px;
                            border-radius: 8px;
                            margin: 30px 0;
                            letter-spacing: 8px;
                        }
                        .message {
                            text-align: center;
                            margin-bottom: 20px;
                        }
                        .footer {
                            text-align: center;
                            font-size: 12px;
                            color: #666;
                            margin-top: 30px;
                            padding-top: 20px;
                            border-top: 1px solid #ddd;
                        }
                        .warning {
                            background-color: #fee2e2;
                            border: 1px solid #ef4444;
                            border-radius: 5px;
                            padding: 15px;
                            margin-top: 20px;
                            text-align: center;
                            font-size: 14px;
                            color: #991b1b;
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <div class="logo">ServiJá</div>
                        </div>
                        
                        <div class="message">
                            <h2>Olá, %s!</h2>
                            <p>Recebemos uma solicitação para redefinir sua senha.</p>
                            <p>Utilize o código abaixo para criar uma nova senha:</p>
                        </div>
                        
                        <div class="code-box">
                            %s
                        </div>
                        
                        <div class="message">
                            <p>Este código é válido por <strong>1 hora</strong>.</p>
                            <p>Se você não solicitou a redefinição de senha, por favor ignore este e-mail.</p>
                        </div>
                        
                        <div class="warning">
                            🔒 Por segurança, nunca compartilhe este código com ninguém.<br>
                            A equipe do ServiJá nunca solicitará seu código.
                        </div>
                        
                        <div class="footer">
                            <p>Este é um e-mail automático, por favor não responda.</p>
                            <p>&copy; 2025 ServiJá. Todos os direitos reservados.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(userName, resetCode);
    }
}

