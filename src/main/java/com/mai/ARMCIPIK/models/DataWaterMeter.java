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
@Table(name = "t_data_water_meter")
public class DataWaterMeter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private long valueMeter = 0;
    @DateTimeFormat(pattern = "dd-MM-yyyy")
    private Date dateOfDataMeter;//Дата показания
    @ManyToOne
    private WaterMeter waterMeter;
    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;
    private boolean isStartMonth;

    public DataWaterMeter(Date dateOfDataMeter, WaterMeter waterMeter, boolean isStartMonth) {
        this.dateOfDataMeter = dateOfDataMeter;
        this.waterMeter = waterMeter;
        this.isStartMonth = isStartMonth;
    }
}
