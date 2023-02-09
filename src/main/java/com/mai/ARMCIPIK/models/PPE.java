package com.mai.ARMCIPIK.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "t_ppe")
public class PPE {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private int count;
    @DateTimeFormat(pattern = "dd-MM-yyyy")
    private Date testDate;
    @ManyToOne
    @JoinColumn(name = "department_id")
    private Department department;
    @OneToMany(mappedBy = "ppe")
    private List<DataPPE> allDataPPEs = new ArrayList<>(); //Сведения о всех показаниях

    public PPE(String name, int count, Date testDate, Department department) {
        this.name = name;
        this.count = count;
        this.testDate = testDate;
        this.department = department;
    }
}
