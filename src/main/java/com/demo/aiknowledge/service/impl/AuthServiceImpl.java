package com.demo.aiknowledge.service.impl;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.dysmsapi.model.v20170525.SendSmsRequest;
import com.aliyuncs.dysmsapi.model.v20170525.SendSmsResponse;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.exceptions.ClientException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.aiknowledge.common.SensitiveWordUtil;
import com.demo.aiknowledge.entity.User;
import com.demo.aiknowledge.mapper.UserMapper;
import com.demo.aiknowledge.service.AuthService;
import com.demo.aiknowledge.common.ErrorCode;
import com.demo.aiknowledge.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final StringRedisTemplate redisTemplate;
    private final SensitiveWordUtil sensitiveWordUtil;

    @Value("${aliyun.sms.accessKeyId}")
    private String accessKeyId;

    @Value("${aliyun.sms.accessKeySecret}")
    private String accessKeySecret;

    @Value("${aliyun.sms.signName}")
    private String signName;

    @Value("${aliyun.sms.templateCode}")
    private String templateCode;

    private static final String SMS_CODE_PREFIX = "sms:code:";
    private static final long SMS_CODE_EXPIRE = 5; // 5分钟

    @Override
    public void sendSmsCode(String phone) {
        if (!isValidPhone(phone)) {
            throw new BusinessException(ErrorCode.INVALID_PHONE_FORMAT);
        }

        // 生成6位随机验证码
        String code = String.valueOf(new Random().nextInt(900000) + 100000);
        
        // 临时使用控制台打印验证码，模拟发送成功
        log.info("【模拟短信】验证码已发送至 {}: {}", phone, code);
        redisTemplate.opsForValue().set(SMS_CODE_PREFIX + phone, code, SMS_CODE_EXPIRE, TimeUnit.MINUTES);
        
        /* 暂时注释阿里云短信服务代码
        try {
            // 调用阿里云短信服务
            DefaultProfile profile = DefaultProfile.getProfile("cn-hangzhou", accessKeyId, accessKeySecret);
            IAcsClient client = new DefaultAcsClient(profile);

            SendSmsRequest request = new SendSmsRequest();
            request.setPhoneNumbers(phone);
            request.setSignName(signName);
            request.setTemplateCode(templateCode);
            request.setTemplateParam("{\"code\":\"" + code + "\"}");

            SendSmsResponse response = client.getAcsResponse(request);
            
            if ("OK".equals(response.getCode())) {
                log.info("Send SMS code {} to phone {} success", code, phone);
                // 存入 Redis，设置5分钟过期
                redisTemplate.opsForValue().set(SMS_CODE_PREFIX + phone, code, SMS_CODE_EXPIRE, TimeUnit.MINUTES);
            } else {
                log.error("Send SMS failed: {}, message: {}", response.getCode(), response.getMessage());
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "短信发送失败: " + response.getMessage());
            }
        } catch (ClientException e) {
            log.error("Aliyun SMS Client Exception", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "短信服务异常");
        }
        */
    }

    @Override
    public User register(String phone, String code, String password, String username) {
        // 1. 基础校验
        if (!isValidPhone(phone)) {
            throw new BusinessException(ErrorCode.INVALID_PHONE_FORMAT);
        }
        if (!StringUtils.hasText(code)) {
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_REQUIRED);
        }
        if (!StringUtils.hasText(password) || password.length() < 6) {
            throw new BusinessException(ErrorCode.PASSWORD_TOO_SHORT);
        }

        // 2. 校验验证码
        String cachedCode = redisTemplate.opsForValue().get(SMS_CODE_PREFIX + phone);
        if (cachedCode == null) {
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_EXPIRED);
        }
        if (!cachedCode.equals(code)) {
            throw new BusinessException(ErrorCode.INVALID_VERIFICATION_CODE);
        }

        // 3. 检查手机号是否已存在
        User existingUser = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
        if (existingUser != null) {
            throw new BusinessException(ErrorCode.PHONE_ALREADY_REGISTERED);
        }

        // 4. 用户名敏感词校验
        String finalUsername = StringUtils.hasText(username) ? username : "User_" + phone.substring(7);
        if (sensitiveWordUtil.contains(finalUsername)) {
            throw new BusinessException(ErrorCode.USERNAME_SENSITIVE);
        }

        // 5. 创建用户
        User user = new User();
        user.setPhone(phone);
        user.setPassword(password); // 实际生产环境应加密存储
        user.setUsername(finalUsername);
        user.setCreateTime(LocalDateTime.now());
        
        userMapper.insert(user);
        
        // 注册成功后删除验证码
        redisTemplate.delete(SMS_CODE_PREFIX + phone);
        
        return user;
    }

    private boolean isValidPhone(String phone) {
        return phone != null && Pattern.matches("^1[3-9]\\d{9}$", phone);
    }

    @Override
    public User login(String phone, String password) {
        if (!isValidPhone(phone)) {
            throw new BusinessException(ErrorCode.INVALID_PHONE_FORMAT);
        }
        
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        if (!user.getPassword().equals(password)) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }
        return user;
    }

    @Override
    public User updateUserInfo(Long userId, String username, String password) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        if (StringUtils.hasText(username)) {
            user.setUsername(username);
        }
        if (StringUtils.hasText(password)) {
            user.setPassword(password);
        }
        userMapper.updateById(user);
        return user;
    }
}
