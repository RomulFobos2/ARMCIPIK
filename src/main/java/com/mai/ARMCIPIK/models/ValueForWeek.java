package com.mai.ARMCIPIK.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.*;
import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "t_value_for_week")
public class ValueForWeek {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String strTime; //1:00 или 17:00 или 23:00
    private long value = 0;
    boolean workDay;
    @ManyToOne
    private WeekRequest weekRequest;

    public ValueForWeek(String strTime, long value, boolean workDay, WeekRequest weekRequest) {
        this.strTime = strTime;
        this.value = value;
        this.workDay = workDay;
        this.weekRequest = weekRequest;
    }
}
