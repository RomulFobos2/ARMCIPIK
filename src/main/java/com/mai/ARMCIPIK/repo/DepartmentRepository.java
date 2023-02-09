package com.mai.ARMCIPIK.repo;

import com.mai.ARMCIPIK.models.Department;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
    Department findByName(String name);
}
