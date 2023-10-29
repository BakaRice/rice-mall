package com.ricemarch.user.service;


import com.ricemarch.common.response.CommonResponse;
import com.ricemarch.common.response.ResponseCode;
import com.ricemarch.common.response.ResponseUtils;
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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.util.MultiValueMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
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
    private RestTemplate restTemplate;

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
        return restTemplate.postForObject("http://oauth2-service/oauth/token", requestEntity, Map.class);
    }

}
