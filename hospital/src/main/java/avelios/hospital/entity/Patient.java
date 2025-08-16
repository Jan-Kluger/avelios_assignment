package avelios.hospital.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
public class Patient {
    public enum Sex {
        SEX_UNSPECIFIED, 
        MALE, 
        FEMALE, 
        OTHER 
    }
    
    @Id @GeneratedValue
    private UUID id;
    
    @Column(nullable = false)
    private String name;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Sex sex;
    
    @Column(nullable = false)
    private LocalDate dob;
    
    public UUID getId() {
        return id; 
    }
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name; 
    }
    
    public Sex getSex() {
        return sex;
    }
    
    public void setSex(Sex sex) {
        this.sex = sex;
    }
    
    public LocalDate getDob() {
        return dob;
    }
    
    public void setDob(LocalDate dob) {
        this.dob = dob;
    }
}