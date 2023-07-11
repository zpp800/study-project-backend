package com.zpp.service.impl;

import com.zpp.entity.DbAccount;
import com.zpp.mapper.UserMapper;
import com.zpp.service.AuthorizeService;
import com.zpp.utils.EmailContent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthorizeServiceImpl implements AuthorizeService {
    @Value("${spring.mail.username}")
    String from;
    private final UserMapper userMapper;
    private final MailSender mailSender;
    private final StringRedisTemplate template;

    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if(username == null)
            throw new UsernameNotFoundException("用户名不能为空");
        DbAccount account = userMapper.findAccountByNameOrEmail(username);
        if(account == null)
            throw new UsernameNotFoundException("用户名或密码错误");
        return User
                .withUsername(account.getUsername())
                .password(account.getPassword())
                .roles("user")
                .build();
    }

    @Override
    public String sendValidateEmail(String email, String sessionId, boolean hasAccount) {
        String key = "email:" + sessionId + ":" + email + ":" +hasAccount;
        System.out.println("sessionId = " + sessionId);
        if(Boolean.TRUE.equals(template.hasKey(key))) {
            Long expire = Optional.ofNullable(template.getExpire(key, TimeUnit.SECONDS)).orElse(0L);
            if(expire > 120) return "请求频繁，请稍后再试";
        }
        DbAccount account = userMapper.findAccountByNameOrEmail(email);
        if(hasAccount && account == null) return "没有此邮件地址的账户";
        if(!hasAccount && account != null) return "此邮箱已被其他用户注册";
        Random random = new Random();
        int code = random.nextInt(899999) + 100000;
        System.out.println("code = " + code);
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        if(hasAccount){
            message.setSubject(EmailContent.subject2);
            message.setText(EmailContent.text2(code));
        }else {
            message.setSubject(EmailContent.subject1);
            message.setText(EmailContent.text1(code));
        }

        try {
            System.out.println("************ before" + LocalTime.now());
            mailSender.send(message);
            System.out.println("************ after" + LocalTime.now());
            template.opsForValue().set(key, String.valueOf(code), 3, TimeUnit.MINUTES);
            return null;
        } catch (MailException e) {
            e.printStackTrace();
            return "邮件发送失败，请检查邮件地址是否有效";
        }
    }

    @Override
    public String validateAndRegister(String username, String password, String email, String code, String sessionId) {
        String key = "email:" + sessionId + ":" + email + ":false";
        System.out.println("sessionId2 = " + sessionId);
        if(Boolean.TRUE.equals(template.hasKey(key))) {
            String s = template.opsForValue().get(key);
            if(s == null) return "验证码失效，请重新请求";
            if(s.equals(code)) {
                DbAccount account = userMapper.findAccountByNameOrEmail(username);
                if(account != null) return "此用户名已被注册，请更换用户名";
                template.delete(key);
                password = encoder.encode(password);
                if (userMapper.createAccount(username, password, email) > 0) {
                    return null;
                } else {
                    return "内部错误，请联系管理员";
                }
            } else {
                return "验证码错误，请检查后再提交";
            }
        } else {
            return "请先请求一封验证码邮件";
        }
    }

    @Override
    public String validateOnly(String email, String code, String sessionId) {
        String key = "email:" + sessionId + ":" + email + ":true";
        if(Boolean.TRUE.equals(template.hasKey(key))) {
            String s = template.opsForValue().get(key);
            if(s == null) return "验证码失效，请重新请求";
            if(s.equals(code)) {
                template.delete(key);
                return null;
            } else {
                return "验证码错误，请检查后再提交";
            }
        } else {
            return "请先请求一封验证码邮件";
        }
    }

    @Override
    public boolean resetPassword(String password, String email) {
        password = encoder.encode(password);
        boolean b = userMapper.resetPasswordByEmail(password, email) > 0;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject(EmailContent.subject3);
        message.setText(EmailContent.text3);
        mailSender.send(message);
        return b;
    }
}
