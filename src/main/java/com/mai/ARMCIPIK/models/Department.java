package com.mai.ARMCIPIK.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "t_department")
public class Department {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String address;
    private String email;
    private String number;
    @Column(length = 10000)
    private String description; //Описание департамента
    @Column(length = 10000)
    private String shortDescription; //Описание департамента

    @OneToOne
    @JoinColumn(name = "role_id")
    private Role departmentRole;

    public Department(String name, String address, String email, String number, String description, String shortDescription, Role departmentRole) {
        this.name = name;
        this.address = address;
        this.email = email;
        this.number = number;
        this.description = description;
        this.shortDescription = shortDescription;
        this.departmentRole = departmentRole;
    }
}
