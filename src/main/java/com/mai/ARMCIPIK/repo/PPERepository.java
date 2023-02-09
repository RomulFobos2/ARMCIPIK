package com.mai.ARMCIPIK.repo;

import com.mai.ARMCIPIK.models.PPE;
import com.mai.ARMCIPIK.models.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PPERepository extends JpaRepository<PPE, Long> {
    PPE findByName(String name);
}
