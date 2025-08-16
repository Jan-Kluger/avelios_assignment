package avelios.hospital.repo;
import avelios.hospital.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface VisitRepo extends JpaRepository<Visit, Long> {
    List<Visit> findByHospital_Id(UUID hospitalId);
    List<Visit> findByPatient_Id(UUID patientId);
    
    @Query(
    """
        select distinct v.hospital
        from Visit v
        where v.patient.id = :patientId
    """)
    
    List<avelios.hospital.entity.Hospital> hospitalsOfPatient(@Param("patientId") java.util.UUID patientId);
    
}
