package com.mai.ARMCIPIK.repo;

import com.mai.ARMCIPIK.models.DataMeter;
import com.mai.ARMCIPIK.models.ValueForWeek;
import org.springframework.data.jpa.repository.JpaRepository;

//Класс для работы в БД с сущностью DataMeter (сущность хранить показания счетчиков и поле Счетчик к которому относится)
public interface ValueForWeekRepository extends JpaRepository<ValueForWeek, Long> {
}
