package com.ricemarch.user.repo;

import com.ricemarch.user.pojo.Oauth2Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface OauthClientRepository extends JpaRepository<Oauth2Client, Integer> {
    Oauth2Client findByClientId(String clientId);

    // 对于手机+验证码登录的用户，本身是没有密码的，验证码就是他的密码，所以这个密码需要随时进行更替
    // 用户登录 30 天token 有效，30天以后需要从新登录，如果30天后使用phone + code的话，就需要更新
    // user表的password字段和oauth client 表的 client_secret 字段为当前code。
    @Query(value = "update oauth_client_details set client_secret = ?1 where client_id = ?2", nativeQuery = true)
    @Modifying
    @Transactional
    void updateSecretByClientId(String secret, String clientId);
}
