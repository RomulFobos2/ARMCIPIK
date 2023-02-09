package com.mai.ARMCIPIK.repo;

import com.mai.ARMCIPIK.models.DataMeter;
import com.mai.ARMCIPIK.models.DataWaterMeter;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DataWaterMeterRepository extends JpaRepository<DataWaterMeter, Long> {
}
