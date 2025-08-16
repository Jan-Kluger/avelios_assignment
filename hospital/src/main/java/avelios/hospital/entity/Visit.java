package avelios.hospital.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
public class Visit {
    @Id @GeneratedValue
    private Long id;
    
    @ManyToOne(optional = false)
    private Hospital hospital;
    
    @ManyToOne(optional = false)
    private Patient patient;
    
    @Column(nullable = false)
    private LocalDate visitDate;
    
    public Visit() {}
    public Visit(Hospital hospital, Patient patient, LocalDate visitDate) {
        this.hospital = hospital;
        this.patient = patient;
        this.visitDate = visitDate;
    }
    
    public Long getId() {
        return id;
    }
    
    public Hospital getHospital() {
        return hospital;
    }

    public Patient getPatient() {
        return patient;
    }

    public LocalDate getVisitDate() {
        return visitDate;
    }
    
    public void setHospital(Hospital hospital) {
        this.hospital = hospital;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public void setVisitDate(LocalDate visitDate) {
        this.visitDate = visitDate;
    }
    
}