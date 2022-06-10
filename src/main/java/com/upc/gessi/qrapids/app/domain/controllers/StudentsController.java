package com.upc.gessi.qrapids.app.domain.controllers;

import com.upc.gessi.qrapids.app.domain.adapters.QMA.QMAMetrics;
import com.upc.gessi.qrapids.app.domain.models.Factor;
import com.upc.gessi.qrapids.app.domain.models.Metric;
import com.upc.gessi.qrapids.app.domain.models.Project;
import com.upc.gessi.qrapids.app.domain.models.Student;
import com.upc.gessi.qrapids.app.domain.repositories.Metric.MetricRepository;
import com.upc.gessi.qrapids.app.domain.repositories.Project.ProjectRepository;
import com.upc.gessi.qrapids.app.domain.repositories.Student.StudentRepository;
import com.upc.gessi.qrapids.app.presentation.rest.dto.*;
import com.upc.gessi.qrapids.app.presentation.rest.services.Factors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class StudentsController {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private QMAMetrics qmaMetrics;

    @Autowired
    private MetricRepository metricRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private QualityFactorMetricsController qualityFactorMetricsController;

    public List<DTOStudent> getStudentsFromProject(Long projectId){
        List<Student> students =studentRepository.findAllByProjectId(projectId);
        List<DTOStudent> dtoStudents = new ArrayList<>();
        for(Student s:students) {
            dtoStudents.add(new DTOStudent(s.getId(),s.getName(),s.getTaigaUsername(),s.getGithubUsername()));
        }
        return dtoStudents;
    }

    public List<DTOStudentMetrics> getStudentWithMetricsFromProject(String projectName) throws IOException {
        Project p = projectRepository.findByExternalId(projectName);
        Long projectId = p.getId();
        String projectExternalId = p.getExternalId();
        List<Student> students =studentRepository.findAllByProjectIdOrderByName(projectId);
        List<DTOStudentMetrics> dtoStudentMetrics = new ArrayList<>();
        for(Student s : students) {
            List<Metric> metrics = metricRepository.findAllByStudentIdOrderByName(s.getId());
            List<DTOMetricEvaluation> metricListTaiga = new ArrayList<>();
            List<DTOMetricEvaluation> metricListGithub = new ArrayList<>();
            for(Metric m : metrics) {
                String typeOffactor = qualityFactorMetricsController.getTypeFromFactorOfMetric(m);
                if(typeOffactor.equals("Taiga")) metricListTaiga.add(qmaMetrics.SingleCurrentEvaluation(String.valueOf(m.getExternalId()) ,projectExternalId));
                else if(typeOffactor.equals("Github")) metricListGithub.add(qmaMetrics.SingleCurrentEvaluation(String.valueOf(m.getExternalId()) ,projectExternalId));
            }
            for(DTOMetricEvaluation dto : metricListTaiga) {
                metricListGithub.add(dto);
            }
            DTOStudentMetrics temp = new DTOStudentMetrics(s.getName(), s.getTaigaUsername(), s.getGithubUsername(), metricListGithub);
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
            List<List<DTOMetricEvaluation>> metricListTaiga = new ArrayList<>();
            List<List<DTOMetricEvaluation>> metricListGithub = new ArrayList<>();
            for(Metric m : metrics) {
                String typeOffactor = qualityFactorMetricsController.getTypeFromFactorOfMetric(m);
                if(typeOffactor.equals("Taiga")) metricListTaiga.add(qmaMetrics.SingleHistoricalData(String.valueOf(m.getExternalId()) , from, to, projectExternalId, profileId));
                else if(typeOffactor.equals("Github")) metricListGithub.add(qmaMetrics.SingleHistoricalData(String.valueOf(m.getExternalId()) , from, to, projectExternalId, profileId));
            }
            for(List<DTOMetricEvaluation> list : metricListTaiga) {
                metricListGithub.add(list);
            }
            DTOStudentMetricsHistorical temp = new DTOStudentMetricsHistorical(s.getName(), s.getTaigaUsername(), s.getGithubUsername(), metricListGithub);
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
            student.setGithubUsername(students.getGithubUsername());
            student.setTaigaUsername(students.getTaigaUsername());
            studentRepository.save(student);
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
            Student student = new Student(students.getStudentName(), students.getTaigaUsername(), students.getGithubUsername(), externalProj);
            studentRepository.save(student);
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
