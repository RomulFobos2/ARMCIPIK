package com.mai.ARMCIPIK.repo;

import com.mai.ARMCIPIK.models.DataMeter;
import com.mai.ARMCIPIK.models.MeasuringTool;
import org.springframework.data.jpa.repository.JpaRepository;

//Класс для работы в БД с сущностью DataMeter (сущность хранить показания счетчиков и поле Счетчик к которому относится)
public interface DataMeterRepository extends JpaRepository<DataMeter, Long> {
}
