package com.ricemarch.user.service;


import com.alibaba.fastjson.JSONObject;
import com.ricemarch.common.response.CommonResponse;
import com.ricemarch.common.response.ResponseCode;
import com.ricemarch.common.response.ResponseUtils;
import com.ricemarch.user.config.GiteeConfig;
import com.ricemarch.user.pojo.AuthGrantType;
import com.ricemarch.user.pojo.Oauth2Client;
import com.ricemarch.user.pojo.RegisterType;
import com.ricemarch.user.pojo.User;
import com.ricemarch.user.processor.RedisCommonProcessor;
import com.ricemarch.user.repo.OauthClientRepository;
import com.ricemarch.user.repo.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.util.MultiValueMap;
import org.springframework.http.HttpEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Service
public class UserRegisterLoginService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OauthClientRepository oauthClientRepository;

    @Autowired
    private RedisCommonProcessor redisCommonProcessor;

    @Autowired
    private RestTemplate innerRestTemplate;

    @Autowired
    private RestTemplate outerRestTemplate;

    @Autowired
    private GiteeConfig giteeConfig;

    @Resource(name = "transactionManager")
    private JpaTransactionManager transactionManager;


    // 如果当前存在事物，就加入该事务，如果不存在，就创建一个新的事务
    // @Transactional(propagation = Propagation.REQUIRED)
    public CommonResponse namePasswordRegister(User user) {
        // 新用户的注册
        if (userRepository.findByUserName(user.getUserName()) == null
                && oauthClientRepository.findByClientId(user.getUserName()) == null) {
            // user 信息组装
            BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
            String password = user.getPasswd();
            String encodePassword = bCryptPasswordEncoder.encode(password);
            user.setPasswd(encodePassword);

            Oauth2Client oauth2Client = Oauth2Client.builder()
                    .clientId(user.getUserName())
                    .clientSecret(encodePassword)
                    .resourceIds(RegisterType.USER_PASSWORD.name())
                    .authorizedGrantTypes(AuthGrantType.refresh_token.name().concat(",").concat(AuthGrantType.password.name()))
                    .scope("web")
                    .authorities(RegisterType.USER_PASSWORD.name())
                    .build();
            // start 事务
            Integer uid = this.saveUserAndOauthClient(user, oauth2Client);
            // end 事务
            String personId = uid + 10000000 + "";
            redisCommonProcessor.setExpiredDays(personId, user, 30);
            // return user信息 + token信息给前端
            return ResponseUtils.okResponse(
                    formatResponseContent(user,
                            // 执行generateOauthToken的时候，事务还没有commit，而我们的db用的是
                            // rr模式，读取不到未 commit的新数据
                            generateOauthToken(AuthGrantType.password, user.getUserName(), password, user.getUserName(), password))

            );
        }
        return ResponseUtils.failResponse(ResponseCode.BAD_REQUEST.getCode(), null, "User already exist！ Please login！");
    }

    public CommonResponse phoneCodeRegister(String phoneNumber, String code) {
        String cacheCode = String.valueOf(redisCommonProcessor.get(phoneNumber));

        if (cacheCode == null) {
            return ResponseUtils.failResponse(ResponseCode.BAD_REQUEST.getCode(), null, "验证码已经过期");
        }
        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
        String encodePassword = bCryptPasswordEncoder.encode(code);
        User user = userRepository.findByUserPhone(phoneNumber);
        if (user == null) {
            // 注册流程
            String username = getSystemDefinedUserName(phoneNumber);
            user = User.builder()
                    .userName(username).passwd("").userPhone(phoneNumber).userRole(RegisterType.PHONE_NUMBER.name()).build();


            Oauth2Client oauth2Client = Oauth2Client.builder()
                    .clientId(phoneNumber).clientSecret(encodePassword)
                    .resourceIds(RegisterType.PHONE_NUMBER.name())
                    .authorizedGrantTypes(AuthGrantType.refresh_token.name().concat(",").
                            concat(AuthGrantType.client_credentials.name()))
                    .scope("web").authorities(RegisterType.PHONE_NUMBER.name())
                    .build();

            Integer userId = this.saveUserAndOauthClient(user, oauth2Client);
            String personId = userId + 10000000 + "";
            redisCommonProcessor.setExpiredDays(personId, user, 30);
        } else {
            oauthClientRepository.updateSecretByClientId(encodePassword, phoneNumber);
        }
        return ResponseUtils.okResponse(formatResponseContent(user,
                generateOauthToken(AuthGrantType.client_credentials, null, null, phoneNumber, code)));
    }

    public CommonResponse thirdPartGiteeCallback(HttpServletRequest request) {
        String code = request.getParameter("code");
        String state = request.getParameter("state");

        if (!giteeConfig.getState().equals(state)) {
            throw new UnsupportedOperationException("Invalid state!");
        }
        String tokenUrl = String.format(giteeConfig.getTokenUrl(),
                giteeConfig.getClientId(), giteeConfig.getClientSecret(),
                giteeConfig.getCallBack(), code);
        JSONObject tokenResult = outerRestTemplate.postForObject(tokenUrl, null, JSONObject.class);
        String token = String.valueOf(tokenResult.get("access_token"));

        String userUrl = String.format(giteeConfig.getUserUrl(), token);
        JSONObject userInfo = outerRestTemplate.getForObject(userUrl, JSONObject.class);

        String username = giteeConfig.getState().concat("_").concat(String.valueOf(userInfo.get("name")));
        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
        String encodePassword = bCryptPasswordEncoder.encode(username);

        User user = userRepository.findByUserName(username);
        if (user == null) {
            user = User.builder()
                    .userName(username)
                    .passwd("")
                    .userRole(RegisterType.THIRD_PARTY.name())
                    .build();
            Oauth2Client oauth2Client = Oauth2Client.builder()
                    .clientId(username)
                    .clientSecret(encodePassword)
                    .resourceIds(RegisterType.THIRD_PARTY.name())
                    .authorizedGrantTypes(AuthGrantType.refresh_token.name().concat(",").
                            concat(AuthGrantType.client_credentials.name()))
                    .scope("web")
                    .authorities(RegisterType.THIRD_PARTY.name())
                    .build();
            Integer userId = this.saveUserAndOauthClient(user, oauth2Client);
            String personId = userId + 10000000 + "";
            redisCommonProcessor.setExpiredDays(personId, user, 30);
        }
        return ResponseUtils.okResponse(formatResponseContent(user,
                generateOauthToken(AuthGrantType.client_credentials, null, null, username, username)));
    }

    public CommonResponse login(String username, String password) {
        User user = userRepository.findByUserName(username);
        if(user == null) {
            return ResponseUtils.failResponse(ResponseCode.BAD_REQUEST.getCode(), null, "user not exist!");
        }

        Map content = formatResponseContent(user,
                generateOauthToken(AuthGrantType.password, username, password, username, password));
        String personId = user.getId() + 10000000 + "";
        redisCommonProcessor.setExpiredDays(personId, user, 30);
        return ResponseUtils.okResponse(content);
    }

    private String getSystemDefinedUserName(String phoneNumber) {
        // 前缀 MALL_ + 当前时间戳 + 手机号后4位
        return "MALL_" + System.currentTimeMillis() + phoneNumber.substring(phoneNumber.length() - 4);
    }

    // 校长，我们可以不可以创建一个专门的 对象，进行这两部分内容的返回呢？
    // 乡长本人目前用的所有的Map相关的传接参，request及response，都可以定义为相应的 java 对象
    private Map formatResponseContent(User user, Map oauth2Client) {
        return new HashMap() {{
            put("user", user);
            put("oauth", oauth2Client);
        }};
    }

    private Integer saveUserAndOauthClient(User user, Oauth2Client oauth2Client) {
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        def.setTimeout(30);
        TransactionStatus status = transactionManager.getTransaction(def);
        try {
            user = this.userRepository.save(user);
            this.oauthClientRepository.save(oauth2Client);
            transactionManager.commit(status);
        } catch (Exception e) {
            if (!status.isCompleted()) {
                transactionManager.rollback(status);
            }
            throw new UnsupportedOperationException("DB Save failed!");
        }
        return user.getId();
    }

    private Map generateOauthToken(AuthGrantType authGrantType, String username, String password,
                                   String clientId, String clientSecret) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", authGrantType.name());
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        if (authGrantType == AuthGrantType.password) {
            params.add("username", username);
            params.add("password", password);
        }
        HttpEntity<MultiValueMap<String, String>> requestEntity =
                new HttpEntity<>(params, httpHeaders);
        return innerRestTemplate.postForObject("http://oauth2-service/oauth/token", requestEntity, Map.class);
    }

}
