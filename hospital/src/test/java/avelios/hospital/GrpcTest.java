package avelios.hospital;

import io.grpc.ManagedChannel;
import io.grpc.inprocess.InProcessChannelBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

import avelios.hospital.grpc.*;


@SpringBootTest
@ActiveProfiles("test")
class GrpcTest {
    
    private ManagedChannel channel;
    private HospitalServiceGrpc.HospitalServiceBlockingStub hospital;
    private PatientServiceGrpc.PatientServiceBlockingStub patient;
    
    @BeforeEach
    void setUp() {
        // Must match grpc.server.in-process-name in application-test.properties
        channel = InProcessChannelBuilder.forName("test").directExecutor().build();
        hospital = HospitalServiceGrpc.newBlockingStub(channel);
        patient  = PatientServiceGrpc.newBlockingStub(channel);
    }
    
    @AfterEach
    void tearDown() {
        channel.shutdownNow();
    }
    
    // ---- helpers ----
    private static avelios.hospital.grpc.LocalDate protoDate(int y, int m, int d) {
        return avelios.hospital.grpc.LocalDate.newBuilder()
        .setYear(y).setMonth(m).setDay(d).build();
    }
    
    // ---- tests ----
    
    @Test
    void createPatient_works() {
        var res = patient.createPatient(
        CreatePatientRequest.newBuilder()
        .setName("Alice")
        .setSex(Sex.FEMALE)
        .setDob(protoDate(1990, 5, 10))
        .build());
        assertNotNull(res.getId());
        assertFalse(res.getId().isBlank());
    }
    
    @Test
    void createHospital_works() {
        var res = hospital.createHospital(
        CreateHospitalRequest.newBuilder().setName("General Hospital").build());
        assertNotNull(res.getId());
        assertFalse(res.getId().isBlank());
    }
    
    @Test
    void createTwoPatients_sameName_shouldWork() {
        var p1 = patient.createPatient(CreatePatientRequest.newBuilder()
        .setName("Bob").setSex(Sex.MALE).setDob(protoDate(1985, 1, 1)).build());
        var p2 = patient.createPatient(CreatePatientRequest.newBuilder()
        .setName("Bob").setSex(Sex.MALE).setDob(protoDate(1985, 1, 1)).build());
        assertNotEquals(p1.getId(), p2.getId(), "Different UUIDs expected even if names match");
    }
    
    @Test
    void createTwoHospitals_sameName_shouldWork() {
        // Ensure Hospital.name is NOT unique in your @Entity
        var h1 = hospital.createHospital(CreateHospitalRequest.newBuilder()
        .setName("SameName").build());
        var h2 = hospital.createHospital(CreateHospitalRequest.newBuilder()
        .setName("SameName").build());
        assertNotEquals(h1.getId(), h2.getId(), "Different UUIDs expected for duplicate names");
    }
    
    @Test
    void registerPatientAtHospital_thenListPatients_containsPatient() {
        var hid = hospital.createHospital(CreateHospitalRequest.newBuilder()
        .setName("City Hospital").build()).getId();
        
        var pid = patient.createPatient(CreatePatientRequest.newBuilder()
        .setName("Carol").setSex(Sex.FEMALE).setDob(protoDate(1992, 2, 20)).build()).getId();
        
        var ack = hospital.registerPatient(RegisterPatientRequest.newBuilder()
        .setPatientId(pid).setHospitalId(hid).build());
        assertTrue(ack.getRegistered());
        
        var plist = hospital.listPatientsOfHospital(HospitalId.newBuilder().setId(hid).build());
        var found = plist.getPatientsList().stream().anyMatch(t -> t.getId().equals(pid));
        assertTrue(found, "Registered patient should appear in hospital's patient list");
    }
    
    @Test
    void deleteCreatedPatient_works() {
        var pid = patient.createPatient(CreatePatientRequest.newBuilder()
        .setName("Dave").setSex(Sex.MALE).setDob(protoDate(1995, 3, 15)).build()).getId();
        
        var del = patient.deletePatient(PatientId.newBuilder().setId(pid).build());
        assertTrue(del.getDeleted(), "Deleting existing patient should return deleted=true");
    }
    
    @Test
    void deleteNonExistentPatient_returnsFalse() {
        var randomId = UUID.randomUUID().toString();
        var del = patient.deletePatient(PatientId.newBuilder().setId(randomId).build());
        assertFalse(del.getDeleted(), "Deleting unknown patient should return deleted=false");
    }
    
    @Test
    void listPatientsOfHospital_containsAliceBobCharlie_withCorrectIdsAndNames() {
        var hid = hospital.createHospital(
        CreateHospitalRequest.newBuilder().setName("ListTest Hospital").build()
        ).getId();
        
        var aliceId = patient.createPatient(CreatePatientRequest.newBuilder()
        .setName("Alice").setSex(Sex.FEMALE).setDob(protoDate(1990, 5, 10)).build()).getId();
        
        var bobId = patient.createPatient(CreatePatientRequest.newBuilder()
        .setName("Bob").setSex(Sex.MALE).setDob(protoDate(1988, 1, 1)).build()).getId();
        
        var charlieId = patient.createPatient(CreatePatientRequest.newBuilder()
        .setName("Charlie").setSex(Sex.OTHER).setDob(protoDate(1992, 12, 3)).build()).getId();
        
        hospital.registerPatient(RegisterPatientRequest.newBuilder()
        .setPatientId(aliceId).setHospitalId(hid).build());
        hospital.registerPatient(RegisterPatientRequest.newBuilder()
        .setPatientId(bobId).setHospitalId(hid).build());
        hospital.registerPatient(RegisterPatientRequest.newBuilder()
        .setPatientId(charlieId).setHospitalId(hid).build());
        
        var list = hospital.listPatientsOfHospital(HospitalId.newBuilder().setId(hid).build());
        var tuples = list.getPatientsList();
        
        assertEquals(3, tuples.size());
        
        var expectedByName = java.util.Map.of(
        "Alice", aliceId,
        "Bob", bobId,
        "Charlie", charlieId
        );
        
        var actualByName = new java.util.HashMap<String, String>();
        for (var t : tuples) {
            actualByName.put(t.getName(), t.getId());
        }
        
        assertEquals(expectedByName.keySet(), actualByName.keySet());
        for (var name : expectedByName.keySet()) {
            assertEquals(expectedByName.get(name), actualByName.get(name));
        }
    }
    
}
