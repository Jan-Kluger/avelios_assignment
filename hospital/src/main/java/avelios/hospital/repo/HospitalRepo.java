package avelios.hospital.repo;
import avelios.hospital.entity.Hospital;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface HospitalRepo extends JpaRepository<Hospital, UUID> {
    Optional<Hospital> findByName(String name);
}
