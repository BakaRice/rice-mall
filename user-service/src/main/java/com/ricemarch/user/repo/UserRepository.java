package com.ricemarch.user.repo;

import com.ricemarch.user.pojo.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    User findByUserName(String username);

    User findByUserPhone(String phoneNumber);

    @Query(value = "update user set user_phone=?1 where id=?2", nativeQuery = true)
    @Modifying
    @Transactional
    void updateUserPhoneById(String phoneNumber, Integer id);
}
