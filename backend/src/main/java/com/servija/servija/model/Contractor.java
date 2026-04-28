package com.servija.servija.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "contractors")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@PrimaryKeyJoinColumn(name = "user_id")
public class Contractor extends User {

    @Column
    private String preferredLocation;

    @Column
    @lombok.Builder.Default
    private Integer totalContracts = 0;

    @Column
    @lombok.Builder.Default
    private Integer activeContracts = 0;

    @Column
    @lombok.Builder.Default
    private Integer completedContracts = 0;
}
