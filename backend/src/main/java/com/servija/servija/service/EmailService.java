package com.servija.servija.service;

import com.servija.servija.model.SupportRequest;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async
    public void sendVerificationEmail(String toEmail, String userName, String verificationCode) {
        sendHtmlEmail(
                toEmail,
                "Verificação de E-mail - ServiJa",
                buildVerificationEmailTemplate(userName, verificationCode),
                "email de verificação");
    }

    @Async
    public void sendPasswordResetCodeEmail(String toEmail, String userName, String resetCode) {
        sendHtmlEmail(
                toEmail,
                "Recuperação de Senha - ServiJa",
                buildPasswordResetCodeEmailTemplate(userName, resetCode),
                "email de recuperação de senha");
    }

    @Async
    public void sendAdminPasswordResetLinkEmail(String toEmail, String userName, String resetLink) {
        sendHtmlEmail(
                toEmail,
                "Redefinição de Senha - ServiJa",
                buildAdminPasswordResetLinkEmailTemplate(userName, resetLink),
                "email de redefinição de senha por link");
    }

    @Async
    public void sendSupportRequestCreatedEmail(
            String toEmail,
            String userName,
            Long protocolId,
            SupportRequest.Category category) {
        sendHtmlEmail(
                toEmail,
                "Solicitação de suporte criada - Protocolo #" + protocolId,
                buildSupportRequestCreatedTemplate(userName, protocolId, category),
                "email de suporte");
    }

    @Async
    public void sendSupportRequestDecisionEmail(
            String toEmail,
            String userName,
            Long protocolId,
            SupportRequest.Status status,
            String adminMessage) {
        String statusLabel = status == SupportRequest.Status.APPROVED ? "Aprovada" : "Recusada";
        sendHtmlEmail(
                toEmail,
                "Atualização do chamado #" + protocolId + " - " + statusLabel,
                buildSupportRequestDecisionTemplate(userName, protocolId, status, adminMessage),
                "email de retorno do suporte");
    }

    private void sendHtmlEmail(
            String toEmail,
            String subject,
            String htmlContent,
            String fallbackLabel) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Email enviado com sucesso para {} com assunto {}", toEmail, subject);
        } catch (MessagingException e) {
            log.error("Erro de formatacao ao enviar email para {}: {}", toEmail, e.getMessage(), e);
            logFallback(toEmail, fallbackLabel);
        } catch (MailException e) {
            log.error("Erro de SMTP ao enviar email para {}: {}", toEmail, e.getMessage(), e);
            logFallback(toEmail, fallbackLabel);
        } catch (Exception e) {
            log.error("Erro generico ao enviar email para {}: {}", toEmail, e.getMessage(), e);
            logFallback(toEmail, fallbackLabel);
        }
    }

    private void logFallback(String toEmail, String fallbackLabel) {
        log.warn("Fallback de email para {} com {}", toEmail, fallbackLabel);
    }

    private String buildVerificationEmailTemplate(String userName, String verificationCode) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px; }
                        .container { background-color: #f9f9f9; border-radius: 10px; padding: 30px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
                        .header { text-align: center; margin-bottom: 30px; }
                        .logo { font-size: 32px; font-weight: bold; color: #286AD8; }
                        .code-box { background-color: #286AD8; color: white; font-size: 36px; font-weight: bold; text-align: center; padding: 20px; border-radius: 8px; margin: 30px 0; letter-spacing: 8px; }
                        .message { text-align: center; margin-bottom: 20px; }
                        .footer { text-align: center; font-size: 12px; color: #666; margin-top: 30px; padding-top: 20px; border-top: 1px solid #ddd; }
                        .warning { background-color: #fff3cd; border: 1px solid #ffc107; border-radius: 5px; padding: 15px; margin-top: 20px; text-align: center; font-size: 14px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <div class="logo">ServiJa</div>
                        </div>
                        <div class="message">
                            <h2>Olá, %s!</h2>
                            <p>Obrigado por se cadastrar no ServiJa.</p>
                            <p>Para confirmar seu endereço de e-mail, utilize o código abaixo:</p>
                        </div>
                        <div class="code-box">%s</div>
                        <div class="message">
                            <p>Este código é válido por <strong>24 horas</strong>.</p>
                            <p>Se você não solicitou este código, ignore este e-mail.</p>
                        </div>
                        <div class="warning">Nunca compartilhe este código com ninguém.</div>
                        <div class="footer">
                            <p>Este é um e-mail automático, por favor não responda.</p>
                            <p>&copy; 2026 ServiJa. Todos os direitos reservados.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(userName, verificationCode);
    }

    private String buildPasswordResetCodeEmailTemplate(String userName, String resetCode) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px; }
                        .container { background-color: #f9f9f9; border-radius: 10px; padding: 30px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
                        .header { text-align: center; margin-bottom: 30px; }
                        .logo { font-size: 32px; font-weight: bold; color: #286AD8; }
                        .code-box { background-color: #ef4444; color: white; font-size: 36px; font-weight: bold; text-align: center; padding: 20px; border-radius: 8px; margin: 30px 0; letter-spacing: 8px; }
                        .message { text-align: center; margin-bottom: 20px; }
                        .footer { text-align: center; font-size: 12px; color: #666; margin-top: 30px; padding-top: 20px; border-top: 1px solid #ddd; }
                        .warning { background-color: #fee2e2; border: 1px solid #ef4444; border-radius: 5px; padding: 15px; margin-top: 20px; text-align: center; font-size: 14px; color: #991b1b; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <div class="logo">ServiJa</div>
                        </div>
                        <div class="message">
                            <h2>Olá, %s!</h2>
                            <p>Recebemos uma solicitação para redefinir sua senha.</p>
                            <p>Utilize o código abaixo para criar uma nova senha:</p>
                        </div>
                        <div class="code-box">%s</div>
                        <div class="message">
                            <p>Este código é válido por <strong>30 minutos</strong>.</p>
                            <p>Se você não solicitou a redefinição de senha, ignore este e-mail.</p>
                        </div>
                        <div class="warning">Por segurança, nunca compartilhe este código com ninguém.</div>
                        <div class="footer">
                            <p>Este é um e-mail automático, por favor não responda.</p>
                            <p>&copy; 2026 ServiJa. Todos os direitos reservados.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(userName, resetCode);
    }

    private String buildAdminPasswordResetLinkEmailTemplate(String userName, String resetLink) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px; }
                        .container { background-color: #f9f9f9; border-radius: 10px; padding: 30px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
                        .header { text-align: center; margin-bottom: 30px; }
                        .logo { font-size: 32px; font-weight: bold; color: #286AD8; }
                        .message { text-align: center; margin-bottom: 20px; }
                        .cta-wrapper { text-align: center; margin: 30px 0; }
                        .cta-button { display: inline-block; background-color: #2563eb; color: white !important; text-decoration: none; font-size: 16px; font-weight: bold; padding: 14px 28px; border-radius: 8px; }
                        .helper-box { background-color: white; border: 1px solid #dbeafe; border-radius: 8px; padding: 16px; margin-top: 24px; font-size: 14px; color: #1e3a8a; }
                        .footer { text-align: center; font-size: 12px; color: #666; margin-top: 30px; padding-top: 20px; border-top: 1px solid #ddd; }
                        .warning { background-color: #fee2e2; border: 1px solid #ef4444; border-radius: 5px; padding: 15px; margin-top: 20px; text-align: center; font-size: 14px; color: #991b1b; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <div class="logo">ServiJa</div>
                        </div>
                        <div class="message">
                            <h2>Olá, %s!</h2>
                            <p>Um administrador solicitou a redefinição da sua senha.</p>
                            <p>Clique no botão abaixo para criar uma nova senha.</p>
                        </div>
                        <div class="cta-wrapper">
                            <a class="cta-button" href="%s">Redefinir senha</a>
                        </div>
                        <div class="helper-box">
                            <p>Este link é de <strong>uso único</strong> e expira em <strong>30 minutos</strong>.</p>
                            <p>Se o botão não funcionar, copie e cole este endereço no navegador:</p>
                            <p style="word-break: break-word; margin-top: 12px;">%s</p>
                        </div>
                        <div class="warning">Se você não esperava este e-mail, ignore-o. O link se tornará inválido após o uso.</div>
                        <div class="footer">
                            <p>Este é um e-mail automático, por favor não responda.</p>
                            <p>&copy; 2026 ServiJa. Todos os direitos reservados.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(userName, resetLink, resetLink);
    }

    private String buildSupportRequestCreatedTemplate(
            String userName,
            Long protocolId,
            SupportRequest.Category category) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px; }
                        .container { background-color: #f9f9f9; border-radius: 10px; padding: 30px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
                        .header { text-align: center; margin-bottom: 30px; }
                        .logo { font-size: 32px; font-weight: bold; color: #286AD8; }
                        .protocol-box { background-color: #10243d; color: white; font-size: 28px; font-weight: bold; text-align: center; padding: 18px; border-radius: 8px; margin: 24px 0; }
                        .card { background-color: white; border: 1px solid #e5e7eb; border-radius: 8px; padding: 18px; margin-top: 24px; }
                        .footer { text-align: center; font-size: 12px; color: #666; margin-top: 30px; padding-top: 20px; border-top: 1px solid #ddd; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <div class="logo">ServiJa</div>
                        </div>
                        <h2>Olá, %s!</h2>
                        <p>Sua solicitação de suporte foi criada com sucesso.</p>
                        <p>Nossa equipe tem até <strong>7 dias úteis</strong> para analisar e retornar sua solicitação.</p>
                        <div class="protocol-box">Protocolo #%s</div>
                        <div class="card">
                            <p><strong>Categoria:</strong> %s</p>
                            <p>Guarde este protocolo para acompanhar o chamado quando precisar.</p>
                        </div>
                        <div class="footer">
                            <p>Este é um e-mail automático, por favor não responda.</p>
                            <p>&copy; 2026 ServiJa. Todos os direitos reservados.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(userName, protocolId, getSupportCategoryLabel(category));
    }

    private String buildSupportRequestDecisionTemplate(
            String userName,
            Long protocolId,
            SupportRequest.Status status,
            String adminMessage) {
        String statusLabel = status == SupportRequest.Status.APPROVED ? "Aprovada" : "Recusada";
        String statusColor = status == SupportRequest.Status.APPROVED ? "#047857" : "#b91c1c";
        String statusBackground = status == SupportRequest.Status.APPROVED ? "#ecfdf5" : "#fef2f2";

        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px; }
                        .container { background-color: #f9f9f9; border-radius: 10px; padding: 30px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
                        .header { text-align: center; margin-bottom: 30px; }
                        .logo { font-size: 32px; font-weight: bold; color: #286AD8; }
                        .status-box { background-color: %s; color: %s; font-size: 24px; font-weight: bold; text-align: center; padding: 18px; border-radius: 8px; margin: 24px 0; }
                        .response-box { background-color: white; border: 1px solid #e5e7eb; border-radius: 8px; padding: 18px; margin-top: 24px; white-space: pre-line; }
                        .footer { text-align: center; font-size: 12px; color: #666; margin-top: 30px; padding-top: 20px; border-top: 1px solid #ddd; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <div class="logo">ServiJa</div>
                        </div>
                        <h2>Olá, %s!</h2>
                        <p>Seu chamado de suporte com protocolo <strong>#%s</strong> recebeu uma atualização.</p>
                        <div class="status-box">%s</div>
                        <div class="response-box">
                            <strong>Mensagem da equipe:</strong>
                            <p>%s</p>
                        </div>
                        <div class="footer">
                            <p>Este é um e-mail automático, por favor não responda.</p>
                            <p>&copy; 2026 ServiJa. Todos os direitos reservados.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(
                statusBackground,
                statusColor,
                userName,
                protocolId,
                statusLabel,
                escapeHtml(adminMessage));
    }

    private String getSupportCategoryLabel(SupportRequest.Category category) {
        return switch (category) {
            case BUG -> "Erro ou bug";
            case CATEGORY_SUBCATEGORY_REQUEST -> "Cadastro de nova categoria ou subcategoria";
            case PAYMENT_ISSUE -> "Problemas com pagamento";
            case SERVICE_ISSUE -> "Problemas com serviço";
            case FRAUD -> "Fraude";
            case USER_REPORT -> "Denúncia de usuário";
        };
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
