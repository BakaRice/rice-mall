package com.ricemarch.repo;

import com.ricemarch.pojo.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User,Integer> {
    User queryByUserName(String userName);
}
