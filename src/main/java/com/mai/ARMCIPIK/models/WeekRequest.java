package com.mai.ARMCIPIK.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.*;
import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "t_week_request")
public class WeekRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @DateTimeFormat(pattern = "dd-MM-yyyy")
    private Date dateCreate;
    @DateTimeFormat(pattern = "dd-MM-yyyy")
    private Date dateFrom;
    @DateTimeFormat(pattern = "dd-MM-yyyy")
    private Date dateTo;
    private int number;

    @OneToMany(mappedBy = "weekRequest")
    private List<ValueForWeek> allValueForWeeks = new ArrayList<>(); //Сведения о всех показаниях

    private int countWorkDay; //Кол-во рабочих дней в периоде
    private int countWorkOffDay; //Кол-во не рабочих дней в периоде

    @ManyToOne
    @JoinColumn(name = "department_id")
    private Department department;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;


    public WeekRequest(Date dateCreate, Date dateFrom, Date dateTo, int countWorkDay, int countWorkOffDay, int number,
                       Department department, User user) {
        this.dateCreate = dateCreate;
        this.dateFrom = dateFrom;
        this.dateTo = dateTo;
        this.allValueForWeeks = allValueForWeeks;
        this.countWorkDay = countWorkDay;
        this.countWorkOffDay = countWorkOffDay;
        this.number = number;
        this.department = department;
        this.user = user;
    }
}
