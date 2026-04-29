package com.fund.service;

import com.fund.entity.User;
import com.fund.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
public class AuthService {

    private static final String VERIFY_CODE_PREFIX = "verify:code:";
    private static final int VERIFY_CODE_LENGTH = 6;
    private static final int VERIFY_CODE_EXPIRE_MINUTES = 10;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private JavaMailSender mailSender;

    public void sendVerifyCode(String email) {
        if (userMapper.countByEmail(email) > 0) {
            throw new RuntimeException("该邮箱已被注册");
        }

        String verifyCode = generateVerifyCode();
        redisTemplate.opsForValue().set(
            VERIFY_CODE_PREFIX + email,
            verifyCode,
            VERIFY_CODE_EXPIRE_MINUTES,
            TimeUnit.MINUTES
        );

        sendEmail(email, verifyCode);
    }

    public User register(String email, String password, String nickname, String verifyCode) {
        String cachedCode = redisTemplate.opsForValue().get(VERIFY_CODE_PREFIX + email);
        if (cachedCode == null || !cachedCode.equals(verifyCode)) {
            throw new RuntimeException("验证码错误或已过期");
        }

        if (userMapper.countByEmail(email) > 0) {
            throw new RuntimeException("该邮箱已被注册");
        }

        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setNickname(nickname != null && !nickname.isEmpty() ? nickname : email.split("@")[0]);
        userMapper.insert(user);

        redisTemplate.delete(VERIFY_CODE_PREFIX + email);

        return user;
    }

    public User login(String email, String password) {
        User user = userMapper.findByEmail(email);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (user.getStatus() == null || user.getStatus() == 0) {
            throw new RuntimeException("账号已被禁用");
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("密码错误");
        }
        return user;
    }

    public User getUserById(Long id) {
        return userMapper.findById(id);
    }

    private String generateVerifyCode() {
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < VERIFY_CODE_LENGTH; i++) {
            code.append(random.nextInt(10));
        }
        return code.toString();
    }

    private void sendEmail(String to, String code) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("基金管理系统验证码");
            message.setText("您的验证码是：" + code + "，10分钟内有效。");
            message.setFrom("2837589703@qq.com");
            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("邮件发送失败: " + e.getMessage());
        }
    }
}
