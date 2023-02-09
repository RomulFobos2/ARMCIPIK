package com.mai.ARMCIPIK.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.*;
import java.util.Date;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "t_data_meter")
public class DataMeter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private long valueMeter = 0;
    @DateTimeFormat(pattern = "dd-MM-yyyy")
    private Date dateOfDataMeter;//Дата показания
    private int indexDay; //0 понедельник- 6 воскресенье
    private String nameDay;
    @ManyToOne
    private Meter meter;
    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;
    private long differentWitPrevValue = 0; //разница с предыдущим значением (смотри посследнее не нудевое значение по дате)

    public DataMeter(Date dateOfDataMeter, int indexDay, String nameDay, Meter meter) {
        this.dateOfDataMeter = dateOfDataMeter;
        this.indexDay = indexDay;
        this.nameDay = nameDay;
        this.meter = meter;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataMeter dataMeter = (DataMeter) o;
        return dateOfDataMeter.equals(dataMeter.dateOfDataMeter) && meter.equals(dataMeter.meter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dateOfDataMeter, meter);
    }
}
