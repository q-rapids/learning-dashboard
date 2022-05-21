package com.upc.gessi.qrapids.app.domain.controllers;

import com.upc.gessi.qrapids.app.domain.models.Project;
import com.upc.gessi.qrapids.app.domain.models.Student;
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
    private ProjectRepository projectRepository;

    public List<DTOStudent> getStudentsFromProject(Long projectId){
        List<Student> students =studentRepository.findAllByProjectId(projectId);
        List<DTOStudent> dtoStudents = new ArrayList<>();
        for(Student s:students) {
            dtoStudents.add(new DTOStudent(s.getName(),s.getTaigaUsername(),s.getGithubUsername()));
        }
        return dtoStudents;
    }

    public void updateStudents(List<DTOStudent> students) {

        //This function only works properly when all students are from the same project.
        List<Student> stu =studentRepository.findAllByProjectId(students.get(0).getProject().getId());
        studentRepository.deleteAll(stu);
        for(DTOStudent s:students) {
            Optional<Project> p = projectRepository.findById(s.getProject().getId());
            if(p.isPresent()) {
                Project project = p.get();
                Student student = new Student(s.getStudentName(), s.getTaigaUsername(), s.getGithubUsername(), project);
                studentRepository.save(student);
            }
        }
    }

    public void deleteStudentsFromPorjectId(Long id) {
        studentRepository.deleteAllByProjectId(id);
    }
}
