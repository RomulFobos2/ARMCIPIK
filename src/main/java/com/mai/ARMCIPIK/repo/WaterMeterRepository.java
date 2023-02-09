package com.mai.ARMCIPIK.repo;

import com.mai.ARMCIPIK.models.Meter;
import com.mai.ARMCIPIK.models.WaterMeter;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WaterMeterRepository extends JpaRepository<WaterMeter, Long> {
    WaterMeter findByFactoryNumber(String factoryNumber);
}
