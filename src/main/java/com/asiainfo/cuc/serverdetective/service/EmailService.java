package com.asiainfo.cuc.serverdetective.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMailMessage;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.text.MessageFormat;
import java.util.Map;

@Service
@Slf4j
public class EmailService {

    @Autowired
    private JavaMailSender javaMailSender;

    @Value("${email.from}")
    private String from;

    @Value("${email.to}")
    private String[] to;

    @Value("${email.title}")
    private String title;

    @Value("${email.specialTo}")
    private String[] specialTo;

    @Value("${email.special}")
    private boolean special;

    public void sendBase(String content){
        send(content);
    }
    public void sendSecret(String content){
        sendTo(content,specialTo);
    }

    private void send(String content){
        defaultSend(this.from,this.title,content,this.to);
    }
    private void sendTo(String content,String... specialTo){
        defaultSend(this.from,this.title,content,specialTo);
    }

    private void defaultSend(String from,String title,String content,String... to){
//        SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
//        simpleMailMessage.setFrom(from);
//        simpleMailMessage.setTo(to);
//        simpleMailMessage.setSubject(title);
//        simpleMailMessage.setText(content);
//        javaMailSender.send(simpleMailMessage);

        MimeMessage message = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = null;
        try {
            helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setFrom(from);
            helper.setSubject(title);
            helper.setText(buildHtml(content), true);

        } catch (MessagingException e) {
            e.printStackTrace();
        }
        javaMailSender.send(message);
        log.info("邮件发送完毕" );
    }
    private static String buildHtml(String content)  {

        String contentText = "<html>\n" +
                "<body>\n" +
                content+
                "</body>\n" +
                "</html>";
        return contentText;
    }
}
