package com.upc.gessi.qrapids.app.domain.controllers;

import com.upc.gessi.qrapids.app.domain.models.Project;
import com.upc.gessi.qrapids.app.domain.models.Student;
import com.upc.gessi.qrapids.app.domain.repositories.Project.ProjectRepository;
import com.upc.gessi.qrapids.app.domain.repositories.Student.StudentRepository;
import com.upc.gessi.qrapids.app.presentation.rest.dto.DTOProject;
import com.upc.gessi.qrapids.app.presentation.rest.dto.DTOStudent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
        List<DTOStudent> dtoStudents = null;
        for(Student s:students) {
            DTOProject dtoProject = new DTOProject(s.getProject().getId(), s.getProject().getExternalId(), s.getProject().getName(), s.getProject().getDescription(), s.getProject().getLogo(), s.getProject().getActive(), s.getProject().getBacklogId(), s.getProject().getTaigaURL(), s.getProject().getGithubURL(), s.getProject().getIsGlobal());

            dtoStudents.add(new DTOStudent(s.getName(),s.getTaigaUsername(),s.getGithubUsername(),dtoProject));
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
                Student student = new Student(s.getName(), s.getTaigaUsername(), s.getGithubUsername(), project);
                studentRepository.save(student);
            }
        }
    }
}
