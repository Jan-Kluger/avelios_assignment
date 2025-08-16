package avelios.hospital.repo;
import avelios.hospital.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface PatientRepo extends JpaRepository<Patient, UUID> {}