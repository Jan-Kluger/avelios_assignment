package avelios.hospital.grpc;

import avelios.hospital.entity.Patient;
import avelios.hospital.repo.PatientRepo;
import avelios.hospital.repo.VisitRepo;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

import java.time.LocalDate;
import java.util.UUID;

@GrpcService
public class PatientGrpcService
extends avelios.hospital.grpc.PatientServiceGrpc.PatientServiceImplBase {
    
    private final PatientRepo patients;
    private final VisitRepo visits;
    
    public PatientGrpcService(PatientRepo patients, VisitRepo visits) {
        this.patients = patients;
        this.visits = visits;
    }
    
    private static UUID uuid(String s) { return UUID.fromString(s); }
    
    private static LocalDate toJavaLocalDate(avelios.hospital.grpc.LocalDate d) {
        return LocalDate.of(d.getYear(), d.getMonth(), d.getDay());
    }
    
    private static avelios.hospital.grpc.LocalDate toProtoDate(LocalDate d) {
        return avelios.hospital.grpc.LocalDate.newBuilder()
        .setYear(d.getYear())
        .setMonth(d.getMonthValue())
        .setDay(d.getDayOfMonth())
        .build();
    }
    
    private static Patient.Sex toEntitySex(avelios.hospital.grpc.Sex sx) {
        return switch (sx) {
            case MALE   -> Patient.Sex.MALE;
            case FEMALE -> Patient.Sex.FEMALE;
            case OTHER  -> Patient.Sex.OTHER;
            default     -> Patient.Sex.SEX_UNSPECIFIED;
        };
    }
    private static avelios.hospital.grpc.Sex toProtoSex(Patient.Sex sx) {
        return switch (sx) {
            case MALE            -> avelios.hospital.grpc.Sex.MALE;
            case FEMALE          -> avelios.hospital.grpc.Sex.FEMALE;
            case OTHER           -> avelios.hospital.grpc.Sex.OTHER;
            case SEX_UNSPECIFIED -> avelios.hospital.grpc.Sex.SEX_UNSPECIFIED;
        };
    }
    
    @Override
    public void createPatient(avelios.hospital.grpc.CreatePatientRequest req,
    StreamObserver<avelios.hospital.grpc.PatientId> out) {
        var p = new Patient();
        p.setName(req.getName());
        p.setSex(toEntitySex(req.getSex()));
        p.setDob(toJavaLocalDate(req.getDob()));
        patients.save(p);
        
        out.onNext(avelios.hospital.grpc.PatientId.newBuilder()
        .setId(p.getId().toString())
        .build());
        out.onCompleted();
    }
    
    @Override
    public void updatePatient(avelios.hospital.grpc.UpdatePatientRequest req,
    StreamObserver<avelios.hospital.grpc.Patient> out) {
        var p = patients.findById(uuid(req.getId())).orElseThrow(() ->
        Status.NOT_FOUND.withDescription("patient not found").asRuntimeException());
        
        if (!req.getName().isBlank()) p.setName(req.getName());
        p.setSex(toEntitySex(req.getSex()));
        p.setDob(toJavaLocalDate(req.getDob()));
        patients.save(p);
        
        out.onNext(avelios.hospital.grpc.Patient.newBuilder()
        .setId(p.getId().toString())
        .setName(p.getName())
        .setSex(toProtoSex(p.getSex()))
        .setDob(toProtoDate(p.getDob()))
        .build());
        out.onCompleted();
    }
    
    @Override
    public void deletePatient(avelios.hospital.grpc.PatientId req,
    StreamObserver<avelios.hospital.grpc.DeletePatientResponse> out) {
        var id = uuid(req.getId());
        if (!patients.existsById(id)) {
            out.onNext(avelios.hospital.grpc.DeletePatientResponse.newBuilder()
            .setDeleted(false).setId(req.getId()).build());
            out.onCompleted();
            return;
        }
        patients.deleteById(id);
        out.onNext(avelios.hospital.grpc.DeletePatientResponse.newBuilder()
        .setDeleted(true).setId(req.getId()).build());
        out.onCompleted();
    }
    
    @Override
    public void listPatients(avelios.hospital.grpc.ListPatientsRequest req,
    StreamObserver<avelios.hospital.grpc.PatientList> out) {
        var list = avelios.hospital.grpc.PatientList.newBuilder();
        patients.findAll().forEach(p ->
        list.addPatients(avelios.hospital.grpc.PatientTuple.newBuilder()
        .setId(p.getId().toString())
        .setName(p.getName())));
        out.onNext(list.build());
        out.onCompleted();
    }
    
    @Override
    public void listHospitalsOfPatient(avelios.hospital.grpc.PatientId req,
    StreamObserver<avelios.hospital.grpc.HospitalList> out) {
        var id = uuid(req.getId());
        var list = avelios.hospital.grpc.HospitalList.newBuilder();
        
        visits.hospitalsOfPatient(id).forEach(h ->
        list.addHospitals(avelios.hospital.grpc.HospitalTuple.newBuilder()
        .setId(h.getId().toString())
        .setName(h.getName()))
        );
        
        out.onNext(list.build());
        out.onCompleted();
    }
}
