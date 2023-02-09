package com.mai.ARMCIPIK.repo;

import com.mai.ARMCIPIK.models.Verification;
import org.springframework.data.jpa.repository.JpaRepository;

//Класс для работы в БД с сущностью User
public interface VerificationRepository extends JpaRepository<Verification, Long> {
}
