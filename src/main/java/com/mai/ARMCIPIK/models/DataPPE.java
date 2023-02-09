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
@Table(name = "t_data_ppe")
public class DataPPE {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private boolean finishTest;
    @DateTimeFormat(pattern = "dd-MM-yyyy")
    private Date dateOfDataPPE;//Дата показания
    @ManyToOne
    private PPE ppe;
    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    public DataPPE(boolean finishTest, Date dateOfDataPPE, PPE ppe) {
        this.finishTest = finishTest;
        this.dateOfDataPPE = dateOfDataPPE;
        this.ppe = ppe;
    }
}
