package avelios.hospital.grpc;

import avelios.hospital.entity.Hospital;
import avelios.hospital.entity.Visit;
import avelios.hospital.repo.HospitalRepo;
import avelios.hospital.repo.PatientRepo;
import avelios.hospital.repo.VisitRepo;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@GrpcService
public class HospitalGrpcService extends HospitalServiceGrpc.HospitalServiceImplBase {
    
    private final HospitalRepo hospitals;
    private final PatientRepo patients;
    private final VisitRepo visits;
    
    public HospitalGrpcService(HospitalRepo hospitals, PatientRepo patients, VisitRepo visits) {
        this.hospitals = hospitals;
        this.patients = patients;
        this.visits = visits;
    }
    
    private static UUID uuid(String s) {
        return UUID.fromString(s);
    }
    
    @Override
    public void createHospital(avelios.hospital.grpc.CreateHospitalRequest req,
    StreamObserver<avelios.hospital.grpc.HospitalId> out) {
        if (req.getName().isBlank()) {
            out.onError(Status.INVALID_ARGUMENT.withDescription("name required").asRuntimeException());
            return;
        }
        var h = new Hospital();
        h.setName(req.getName());
        hospitals.save(h);
        
        out.onNext(avelios.hospital.grpc.HospitalId.newBuilder()
        .setId(h.getId().toString())
        .build());
        out.onCompleted();
    }
    
    @Override
    public void updateHospital(avelios.hospital.grpc.UpdateHospitalRequest req,
    StreamObserver<avelios.hospital.grpc.Hospital> out) {
        var id = uuid(req.getId());
        var h = hospitals.findById(id).orElseThrow(() ->
        Status.NOT_FOUND.withDescription("hospital not found").asRuntimeException());
        if (!req.getName().isBlank()) {
            h.setName(req.getName());
        }
        hospitals.save(h);
        
        out.onNext(avelios.hospital.grpc.Hospital.newBuilder()
        .setId(h.getId().toString())
        .setName(h.getName())
        .build());
        out.onCompleted();
    }
    
    @Override
    public void deleteHospital(avelios.hospital.grpc.HospitalId req,
    StreamObserver<avelios.hospital.grpc.DeleteHospitalResponse> out) {
        var id = uuid(req.getId());
        if (!hospitals.existsById(id)) {
            out.onNext(avelios.hospital.grpc.DeleteHospitalResponse.newBuilder()
            .setDeleted(false)
            .setId(req.getId())
            .build());
            out.onCompleted();
            return;
        }
        
        hospitals.deleteById(id);
        
        out.onNext(avelios.hospital.grpc.DeleteHospitalResponse.newBuilder()
        .setDeleted(true)
        .setId(req.getId())
        .build());
        out.onCompleted();
    }
    
    @Override
    public void listHospitals(avelios.hospital.grpc.ListHospitalsRequest req,
    StreamObserver<avelios.hospital.grpc.HospitalList> out) {
        var list = avelios.hospital.grpc.HospitalList.newBuilder();
        hospitals.findAll().forEach(h ->
        list.addHospitals(avelios.hospital.grpc.HospitalTuple.newBuilder()
        .setId(h.getId().toString())
        .setName(h.getName())));
        out.onNext(list.build());
        out.onCompleted();
    }
    
    @Override
    public void listPatientsOfHospital(avelios.hospital.grpc.HospitalId req,
    StreamObserver<avelios.hospital.grpc.PatientList> out) {
        var id = uuid(req.getId());
        var list = avelios.hospital.grpc.PatientList.newBuilder();
        
        // distinct patients via a set
        Set<UUID> seen = new HashSet<>();
        visits.findByHospital_Id(id).forEach(v -> {
            var p = v.getPatient();
            if (seen.add(p.getId())) {
                list.addPatients(avelios.hospital.grpc.PatientTuple.newBuilder()
                .setId(p.getId().toString())
                .setName(p.getName()));
            }
        });
        out.onNext(list.build());
        out.onCompleted();
    }
    
    @Override
    public void registerPatient(avelios.hospital.grpc.RegisterPatientRequest req,
    StreamObserver<avelios.hospital.grpc.RegisterAck> out) {
        var p = patients.findById(uuid(req.getPatientId())).orElse(null);
        var h = hospitals.findById(uuid(req.getHospitalId())).orElse(null);
        if (p == null || h == null) {
            out.onError(Status.NOT_FOUND.withDescription("patient or hospital not found").asRuntimeException());
            return;
        }
        visits.save(new Visit(h, p, LocalDate.now()));
        
        out.onNext(avelios.hospital.grpc.RegisterAck.newBuilder()
        .setRegistered(true)
        .setPatientId(req.getPatientId())
        .setHospitalId(req.getHospitalId())
        .build());
        out.onCompleted();
    }
}
