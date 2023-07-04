package com.upc.gessi.qrapids.app.domain.controllers;

import com.upc.gessi.qrapids.app.domain.adapters.QMA.QMAMetrics;
import com.upc.gessi.qrapids.app.domain.models.*;
import com.upc.gessi.qrapids.app.domain.repositories.Metric.MetricRepository;
import com.upc.gessi.qrapids.app.domain.repositories.Project.ProjectRepository;
import com.upc.gessi.qrapids.app.domain.repositories.Student.StudentRepository;
import com.upc.gessi.qrapids.app.domain.repositories.StudentIdentity.StudentIdentityRepository;
import com.upc.gessi.qrapids.app.presentation.rest.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class StudentsController {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private StudentIdentityRepository studentIdentityRepository;

    @Autowired
    private QMAMetrics qmaMetrics;

    @Autowired
    private MetricRepository metricRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private QualityFactorMetricsController qualityFactorMetricsController;

    @Autowired
    private MetricsController metricsController;


    public DTOStudent getDTOStudentFromStudent(Student student){
        List<StudentIdentity> studentIdentities = studentIdentityRepository.findAllByStudent(student);

        Map<DataSource, DTOStudentIdentity> DTOStudentIdentities = new HashMap<>();

        studentIdentities.forEach(identity -> DTOStudentIdentities.put(identity.getDataSource(), new DTOStudentIdentity(identity.getDataSource(), identity.getUsername())));

        return new DTOStudent(student.getId(),student.getName(),DTOStudentIdentities);
    }
    public List<DTOStudent> getStudentsFromProject(Long projectId){
        List<Student> students = studentRepository.findAllByProjectId(projectId);
        List<DTOStudent> dtoStudents = new ArrayList<>();
        for(Student s:students) {
            dtoStudents.add(getDTOStudentFromStudent(s));
        }
        return dtoStudents;
    }

    public List<StudentIdentity> getStudentIdentities(Student student){
        return studentIdentityRepository.findAllByStudent(student);
    }

    public String normalizedName(String name, List<DTOStudentIdentity> studentIdentities,  String replace_name){
        String normalizedMetricName = name;
        int i = 0;
        while (normalizedMetricName.equals(name) && i < studentIdentities.size()) {
            if (name.contains(studentIdentities.get(i).getUsername())) {
                normalizedMetricName = name.replace(studentIdentities.get(i).getUsername(), replace_name);
            }
            ++i;
        }
       return normalizedMetricName;
    }
    public List<DTOStudentMetrics> getStudentWithMetricsFromProject(String projectExternalId) throws IOException {

        Project project = projectRepository.findByExternalId(projectExternalId);
        List<Student> students = studentRepository.findAllByProjectIdOrderByName(project.getId());

        List<DTOStudentMetrics> dtoStudentMetrics = new ArrayList<>();
        for(Student s : students) {

            List<Metric> metrics = metricsController.getNormalizedMetricsByStudentIdOrderByName(s.getId());
            Map<DataSource, List<DTOMetricEvaluation>> dataSourceMetrics = new HashMap<>();
            for(DataSource source : DataSource.values()){
                dataSourceMetrics.put(source, new ArrayList<>());
            }
            List<DTOMetricEvaluation> metricListNoSource = new ArrayList<>();

            for(Metric m : metrics) {
                String typeOfFactor = qualityFactorMetricsController.getTypeFromFactorOfMetric(m);
                if(typeOfFactor == null){
                    metricListNoSource.add(qmaMetrics.SingleCurrentEvaluation(String.valueOf(m.getExternalId()) ,projectExternalId));
                } else {
                    try {
                        DataSource source = DataSource.valueOf(typeOfFactor);
                        dataSourceMetrics.get(source).add(qmaMetrics.SingleCurrentEvaluation(String.valueOf(m.getExternalId()), projectExternalId));
                    } catch (IllegalArgumentException exception) {
                        metricListNoSource.add(qmaMetrics.SingleCurrentEvaluation(String.valueOf(m.getExternalId()), projectExternalId));
                    }
                }
            }

            DataSource source = DataSource.values()[0];
            List<DTOMetricEvaluation> orderedMetricList = dataSourceMetrics.get(source);

            dataSourceMetrics.forEach((dataSource, metricList) -> {
                if(! dataSource.equals(source)){
                    orderedMetricList.addAll(metricList);
                }
            });

            orderedMetricList.addAll(metricListNoSource);

            Map<DataSource, DTOStudentIdentity> dtoStudentIdentities = getDTOStudentFromStudent(s).getIdentities();

            orderedMetricList.forEach(metric -> {
                List<DTOStudentIdentity> studentIdentities = new ArrayList<>(dtoStudentIdentities.values());
                metric.setName(normalizedName(metric.getName(),studentIdentities,s.getName()));
            });

            DTOStudentMetrics temp = new DTOStudentMetrics(s.getName(), dtoStudentIdentities, orderedMetricList);
            dtoStudentMetrics.add(temp);
        }
        return dtoStudentMetrics;
    }

    public void addDTOMetricEvaluationToMetricList(String metricExternalId, LocalDate from, LocalDate to, String profileId, String projectExternalId, List<DTOMetricEvaluation> metrics) throws IOException {
        if(from == null || to == null){//not historical
            metrics.add(qmaMetrics.SingleCurrentEvaluation(metricExternalId,projectExternalId));

        } else { //historical
            metrics.addAll(qmaMetrics.SingleHistoricalData(metricExternalId , from, to, projectExternalId, profileId));
        }
    }

    public List<DTOStudentMetrics> getStudentMetricsFromProject(String projectExternalId, LocalDate from, LocalDate to, String profileId) throws IOException {
        Project project = projectRepository.findByExternalId(projectExternalId);
        List<Student> students = studentRepository.findAllByProjectIdOrderByName(project.getId());

        List<DTOStudentMetrics> dtoStudentMetrics = new ArrayList<>();


        for(Student s : students) {
            List<Metric> metrics = metricRepository.findAllByStudentIdOrderByName(s.getId());
            Integer number = metrics.size();
            Map<DataSource, List<DTOMetricEvaluation>> dataSourceMetrics = new HashMap<>();

            for(DataSource source : DataSource.values()){
                dataSourceMetrics.put(source, new ArrayList<>());
            }

            List<DTOMetricEvaluation> metricListNoSource = new ArrayList<>();

            for(Metric m : metrics) {
                String typeOfFactor = qualityFactorMetricsController.getTypeFromFactorOfMetric(m);
                if(typeOfFactor == null){
                    addDTOMetricEvaluationToMetricList(String.valueOf(m.getExternalId()),from, to, profileId, projectExternalId, metricListNoSource);
                } else {
                    try {
                        DataSource source = DataSource.valueOf(typeOfFactor);
                        addDTOMetricEvaluationToMetricList(String.valueOf(m.getExternalId()),from, to, profileId, projectExternalId, dataSourceMetrics.get(source));
                    } catch (IllegalArgumentException exception) {
                        addDTOMetricEvaluationToMetricList(String.valueOf(m.getExternalId()),from, to, profileId, projectExternalId, metricListNoSource);
                    }
                }
            }

            DataSource source = DataSource.values()[0];
            List<DTOMetricEvaluation> orderedMetricList = dataSourceMetrics.get(source);

            dataSourceMetrics.forEach((dataSource, metricList) -> {
                if(! dataSource.equals(source)){
                    orderedMetricList.addAll(metricList);
                }
            });

            orderedMetricList.addAll(metricListNoSource);

            Map<DataSource, DTOStudentIdentity> dtoStudentIdentities = getDTOStudentFromStudent(s).getIdentities();
            //normalize names
            orderedMetricList.forEach(metric -> {
                List<DTOStudentIdentity> studentIdentities = new ArrayList<>(dtoStudentIdentities.values());
                metric.setName(normalizedName(metric.getName(),studentIdentities,s.getName()));
            });

            DTOStudentMetrics temp = new DTOStudentMetrics(s.getName(), dtoStudentIdentities, orderedMetricList, number);
            dtoStudentMetrics.add(temp);
        }
        return dtoStudentMetrics;
    }

    public Long updateStudentAndMetrics(Long studentID, DTOStudent studentDTO, List<Long> metricsIds, String projectExternalId) {

        Project externalProj = projectRepository.findByExternalId(projectExternalId);
        Optional<Student> studentSearchResult = studentRepository.findStudentById(studentID);
        Map<DataSource, DTOStudentIdentity> identities = studentDTO.getIdentities();

        Student student = null;
        if(studentSearchResult.isPresent()) {
            student = studentSearchResult.get();
            updateStudent(student,studentDTO.getName(),studentDTO.getIdentities(), metricsIds);
        }
        else {
            student = createStudent(studentDTO, externalProj, metricsIds);
        }


        return student.getId();
    }

    public void deleteStudents(Long id) {
        List<Metric> metrics = metricRepository.findAllByStudentId(id);
        for (Metric m : metrics) {
            m.setStudent(null);
            metricRepository.save(m);
        }
        Optional<Student> s = studentRepository.findStudentById(id);
        if(s.isPresent()) {
            Student student = s.get();
            studentRepository.delete(student);
        }

    }

    public Student createStudent(DTOStudent studentDTO, Project project, List<Long> metricsIds){
        Student student =  new Student(studentDTO.getName(), project);
        Map<DataSource, DTOStudentIdentity> identities = studentDTO.getIdentities();

        studentRepository.save(student);

        List<StudentIdentity> new_identities = new ArrayList<>();

        identities.forEach(((dataSource, studentIdentity) -> new_identities.add(new StudentIdentity(dataSource,studentIdentity.getUsername(), student))));

        studentIdentityRepository.saveAll(new_identities);

        metricsController.updateStudentMetricsByIds(metricsIds, student);
        return student;
    }

    public void updateStudent(Student student, String studentName, Map<DataSource, DTOStudentIdentity>  newIdentities,  List<Long> metricsIds){
        student.setName(studentName);

        List<StudentIdentity> previousIdentities = studentIdentityRepository.findAllByStudent(student);

        newIdentities.forEach( (dataSource, studentIdentity) -> {
            AtomicBoolean found = new AtomicBoolean(false);

            previousIdentities.forEach(previousIdentity -> {
                if(dataSource.equals(previousIdentity.getDataSource())){
                    previousIdentity.setUsername(studentIdentity.getUsername());
                    found.set(true);
                }
            });

            if(!found.get()){
                previousIdentities.add(new StudentIdentity(dataSource,studentIdentity.getUsername(), student));
            }
        });


        studentRepository.save(student);
        studentIdentityRepository.saveAll(previousIdentities);

        List<Metric> metrics = metricRepository.findAllByStudentId(student.getId());
        for (Metric m : metrics) {
            m.setStudent(null);
            metricRepository.save(m);
        }

        metricsController.updateStudentMetricsByIds(metricsIds, student);
    }
}
