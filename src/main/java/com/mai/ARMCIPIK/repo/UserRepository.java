package com.mai.ARMCIPIK.repo;

import com.mai.ARMCIPIK.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

//Класс для работы в БД с сущностью User
public interface UserRepository extends JpaRepository<User, Long> {
    User findByUsername(String username);
}
