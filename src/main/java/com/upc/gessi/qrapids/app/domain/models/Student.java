package com.upc.gessi.qrapids.app.domain.models;

import com.upc.gessi.qrapids.app.domain.utils.DataEncryptDecryptConverter;

import javax.persistence.*;

@Entity
@Table(name = "student", uniqueConstraints = {@UniqueConstraint(name="UniqueNameByProject", columnNames = {"name", "projectId"})})
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "name")
    @Convert(converter = DataEncryptDecryptConverter.class)
    private String name;
    @ManyToOne
    @JoinColumn(name="projectId", referencedColumnName = "id")
    private Project project;

    public Student(){}

    public Student(String name, Project project) {
        this.name=name;
        this.project=project;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {return this.name;}

    public void setName(String name) {this.name=name;}

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

}
