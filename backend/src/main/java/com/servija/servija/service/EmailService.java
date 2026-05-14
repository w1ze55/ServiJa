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
                "Verificacao de Email - ServiJa",
                buildVerificationEmailTemplate(userName, verificationCode),
                "codigo de verificacao",
                verificationCode);
    }

    @Async
    public void sendPasswordResetEmail(String toEmail, String userName, String resetToken) {
        sendHtmlEmail(
                toEmail,
                "Recuperacao de Senha - ServiJa",
                buildPasswordResetEmailTemplate(userName, resetToken),
                "token de recuperacao",
                resetToken);
    }

    @Async
    public void sendSupportRequestCreatedEmail(
            String toEmail,
            String userName,
            Long protocolId,
            SupportRequest.Category category) {
        sendHtmlEmail(
                toEmail,
                "Solicitacao de suporte criada - Protocolo #" + protocolId,
                buildSupportRequestCreatedTemplate(userName, protocolId, category),
                "protocolo de suporte",
                String.valueOf(protocolId));
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
                "Atualizacao do chamado #" + protocolId + " - " + statusLabel,
                buildSupportRequestDecisionTemplate(userName, protocolId, status, adminMessage),
                "resposta do suporte",
                adminMessage);
    }

    private void sendHtmlEmail(
            String toEmail,
            String subject,
            String htmlContent,
            String fallbackLabel,
            String fallbackValue) {
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
            logFallback(toEmail, fallbackLabel, fallbackValue);
        } catch (MailException e) {
            log.error("Erro de SMTP ao enviar email para {}: {}", toEmail, e.getMessage(), e);
            logFallback(toEmail, fallbackLabel, fallbackValue);
        } catch (Exception e) {
            log.error("Erro generico ao enviar email para {}: {}", toEmail, e.getMessage(), e);
            logFallback(toEmail, fallbackLabel, fallbackValue);
        }
    }

    private void logFallback(String toEmail, String fallbackLabel, String fallbackValue) {
        log.warn("Fallback de email para {} com {}: {}", toEmail, fallbackLabel, fallbackValue);
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
                            <h2>Ola, %s!</h2>
                            <p>Obrigado por se cadastrar no ServiJa.</p>
                            <p>Para confirmar seu endereco de e-mail, utilize o codigo abaixo:</p>
                        </div>
                        <div class="code-box">%s</div>
                        <div class="message">
                            <p>Este codigo e valido por <strong>24 horas</strong>.</p>
                            <p>Se voce nao solicitou este codigo, ignore este e-mail.</p>
                        </div>
                        <div class="warning">Nunca compartilhe este codigo com ninguem.</div>
                        <div class="footer">
                            <p>Este e um e-mail automatico, por favor nao responda.</p>
                            <p>&copy; 2026 ServiJa. Todos os direitos reservados.</p>
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
                            <h2>Ola, %s!</h2>
                            <p>Recebemos uma solicitacao para redefinir sua senha.</p>
                            <p>Utilize o codigo abaixo para criar uma nova senha:</p>
                        </div>
                        <div class="code-box">%s</div>
                        <div class="message">
                            <p>Este codigo e valido por <strong>1 hora</strong>.</p>
                            <p>Se voce nao solicitou a redefinicao de senha, ignore este e-mail.</p>
                        </div>
                        <div class="warning">Por seguranca, nunca compartilhe este codigo com ninguem.</div>
                        <div class="footer">
                            <p>Este e um e-mail automatico, por favor nao responda.</p>
                            <p>&copy; 2026 ServiJa. Todos os direitos reservados.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(userName, resetCode);
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
                        <h2>Ola, %s!</h2>
                        <p>Sua solicitacao de suporte foi criada com sucesso.</p>
                        <p>Nossa equipe tem ate <strong>7 dias uteis</strong> para analisar e retornar sua solicitacao.</p>
                        <div class="protocol-box">Protocolo #%s</div>
                        <div class="card">
                            <p><strong>Categoria:</strong> %s</p>
                            <p>Guarde este protocolo para acompanhar o chamado quando precisar.</p>
                        </div>
                        <div class="footer">
                            <p>Este e um e-mail automatico, por favor nao responda.</p>
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
                        <h2>Ola, %s!</h2>
                        <p>Seu chamado de suporte com protocolo <strong>#%s</strong> recebeu uma atualizacao.</p>
                        <div class="status-box">%s</div>
                        <div class="response-box">
                            <strong>Mensagem da equipe:</strong>
                            <p>%s</p>
                        </div>
                        <div class="footer">
                            <p>Este e um e-mail automatico, por favor nao responda.</p>
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
            case SERVICE_ISSUE -> "Problemas com servico";
            case FRAUD -> "Fraude";
            case USER_REPORT -> "Denuncia de usuario";
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
