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
    public List<DTOStudentMetrics> getStudentWithMetricsFromProject(String projectName) throws IOException {
        Project p = projectRepository.findByExternalId(projectName);
        Long projectId = p.getId();
        String projectExternalId = p.getExternalId();
        List<Student> students = studentRepository.findAllByProjectIdOrderByName(projectId);
        List<DTOStudentMetrics> dtoStudentMetrics = new ArrayList<>();
        for(Student s : students) {
            List<Metric> metrics = metricRepository.findAllByStudentIdOrderByName(s.getId());
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



    public List<DTOStudentMetricsHistorical> getStudentWithHistoricalMetricsFromProject(String projectName, LocalDate from, LocalDate to, String profileId) throws IOException {
        Project p = projectRepository.findByExternalId(projectName);
        Long projectId = p.getId();
        String projectExternalId = p.getExternalId();
        List<Student> students =studentRepository.findAllByProjectIdOrderByName(projectId);
        List<DTOStudentMetricsHistorical> dtoStudentMetricsHistorical = new ArrayList<>();
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
                    metricListNoSource.add(qmaMetrics.SingleCurrentEvaluation(String.valueOf(m.getExternalId()) ,projectExternalId));
                } else {
                    try {
                        DataSource source = DataSource.valueOf(typeOfFactor);
                        dataSourceMetrics.get(source).addAll(qmaMetrics.SingleHistoricalData(String.valueOf(m.getExternalId()), from, to, projectExternalId, profileId));
                    } catch (IllegalArgumentException exception) {
                        metricListNoSource.addAll(qmaMetrics.SingleHistoricalData(String.valueOf(m.getExternalId()), from, to, projectExternalId, profileId));
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

            DTOStudentMetricsHistorical temp = new DTOStudentMetricsHistorical(s.getName(), dtoStudentIdentities, orderedMetricList, number);
            dtoStudentMetricsHistorical.add(temp);
        }
        return dtoStudentMetricsHistorical;
    }

    public Long updateStudents(String studentID, DTOStudent students, String[] userMetrics, String externalId) {

        Project externalProj = projectRepository.findByExternalId(externalId);
        Optional<Student> s = studentRepository.findStudentById(Long.parseLong(studentID));
        if(s.isPresent()) {
            Student student = s.get();
            student.setName(students.getStudentName());
            Map<DataSource, DTOStudentIdentity> identities = students.getIdentities();

            List<StudentIdentity> previousIdentities = studentIdentityRepository.findAllByStudent(student);

            identities.forEach( (dataSource,studentIdentity) -> {
                boolean found = false;
                for(StudentIdentity previousIdentity: previousIdentities){
                    if(dataSource.equals(previousIdentity.getDataSource())){
                        previousIdentity.setUsername(studentIdentity.getUsername());
                        found = true;
                    }
                }
                if(! found){
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
            for(int i=0; i<userMetrics.length; ++i) {
                Optional<Metric> m = metricRepository.findById(Long.parseLong(userMetrics[i]));
                if (m.isPresent()) {
                    Metric metric = m.get();
                    metric.setStudent(student);
                    metricRepository.save(metric);
                }
            }
            return student.getId();
        }
        else {
            Student student = new Student(students.getStudentName(), externalProj);
            studentRepository.save(student);

            Map<DataSource, DTOStudentIdentity> identities = students.getIdentities();

            List<StudentIdentity> new_identities = new ArrayList<>();
            identities.forEach(((dataSource, studentIdentity) -> new_identities.add(new StudentIdentity(dataSource,studentIdentity.getUsername(), student))));
            studentIdentityRepository.saveAll(new_identities);


            for(int i=0; i<userMetrics.length; ++i) {
                Optional<Metric> m = metricRepository.findById(Long.parseLong(userMetrics[i]));
                if (m.isPresent()) {
                    Metric metric = m.get();
                    metric.setStudent(student);
                    metricRepository.save(metric);
                }
            }
            return student.getId();
        }
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
}
