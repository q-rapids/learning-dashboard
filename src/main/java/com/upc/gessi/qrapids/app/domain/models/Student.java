package com.upc.gessi.qrapids.app.domain.models;

import javax.persistence.*;

@Entity
@Table(name = "student")
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "name" , unique = true)
    private String name;
    @Column(name = "taiga_username")
    private String taiga_username;
    @Column(name = "github_username")
    private String github_username;
    @Column(name = "prt_username")
    private String prt_username;

    @ManyToOne
    @JoinColumn(name="projectId", referencedColumnName = "id")
    private Project project;

    public Student(){}

    public Student(String name, String taiga_username, String github_username, String prt_username, Project project) {
        this.name=name;
        this.taiga_username=taiga_username;
        this.github_username=github_username;
        this.prt_username=prt_username;
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

    public String getTaigaUsername() {return this.taiga_username;}

    public void setTaigaUsername(String taiga_username) {this.taiga_username=taiga_username;}

    public String getGithubUsername() {return this.github_username;}

    public void setGithubUsername(String github_username) {this.github_username=github_username;}

    public String getPrtUsername() {return this.prt_username;}

    public void setPrtUsername(String prt_username) {this.prt_username=prt_username;}

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

}
