package com.mai.ARMCIPIK.repo;

import com.mai.ARMCIPIK.models.Meter;
import org.springframework.data.jpa.repository.JpaRepository;

//Класс для работы в БД с сущностью Счетчик (Meter)
public interface MeterRepository extends JpaRepository<Meter, Long> {
    Meter findByName(String name);
}
