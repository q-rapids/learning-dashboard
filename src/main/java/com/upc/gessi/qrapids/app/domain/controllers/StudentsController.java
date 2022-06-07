package com.upc.gessi.qrapids.app.domain.controllers;

import com.upc.gessi.qrapids.app.domain.models.Metric;
import com.upc.gessi.qrapids.app.domain.models.Project;
import com.upc.gessi.qrapids.app.domain.models.Student;
import com.upc.gessi.qrapids.app.domain.repositories.Metric.MetricRepository;
import com.upc.gessi.qrapids.app.domain.repositories.Project.ProjectRepository;
import com.upc.gessi.qrapids.app.domain.repositories.Student.StudentRepository;
import com.upc.gessi.qrapids.app.presentation.rest.dto.DTOProject;
import com.upc.gessi.qrapids.app.presentation.rest.dto.DTOStudent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class StudentsController {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private MetricRepository metricRepository;

    @Autowired
    private ProjectRepository projectRepository;

    public List<DTOStudent> getStudentsFromProject(Long projectId){
        List<Student> students =studentRepository.findAllByProjectId(projectId);
        List<DTOStudent> dtoStudents = new ArrayList<>();
        for(Student s:students) {
            dtoStudents.add(new DTOStudent(s.getId(),s.getName(),s.getTaigaUsername(),s.getGithubUsername()));
        }
        return dtoStudents;
    }

    public Long updateStudents(String studentID, DTOStudent students, String[] userMetrics, String externalId) {

        Project externalProj = projectRepository.findByExternalId(externalId);
        Optional<Student> s = studentRepository.findStudentById(Long.parseLong(studentID));
        if(s.isPresent()) {
            Student student = s.get();
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