package com.mai.ARMCIPIK.repo;

import com.mai.ARMCIPIK.models.Role;
import org.springframework.data.jpa.repository.JpaRepository;

//Класс для работы в БД с сущностью Role
public interface RoleRepository extends JpaRepository<Role, Long> {
    Role findByName(String name);
}
