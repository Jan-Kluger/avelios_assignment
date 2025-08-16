package avelios.hospital.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
public class Hospital {
    @Id @GeneratedValue
    private UUID id;
    
    @Column(nullable = false)
    private String name;
    
    public UUID getId() { 
        return id; 
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}