package com.upc.gessi.qrapids.app.domain.models;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

import javax.persistence.*;

@Entity
@Table(name = "metric")
public class Metric {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "externalId")
    private String externalId;
    @Column(name = "name")
    private String name;
    @Column(name = "description")
    private String description;
    @Column(name = "threshold")
    private Float threshold;
    @Column(name = "web_url")
    private String webUrl;
    @Column(name = "category_name", columnDefinition = "varchar default 'Default'")
    private String categoryName;

    @ManyToOne
    @JoinColumn(name="projectId", referencedColumnName = "id")
    private Project project;

    @ManyToOne
    @JoinColumn(name="studentId", referencedColumnName = "id")
    private Student student;

    public Metric(){}

    public Metric(String externalId, String name, String description, Project project, String categoryName) {
        this.externalId = externalId;
        this.name = name;
        this.description = description;
        this.categoryName = categoryName;
        setProject(project);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public Float getThreshold() {
        return threshold;
    }

    public void setThreshold(Float threshold) {
        this.threshold = threshold;
    }

    public String getWebUrl() {
        return webUrl;
    }

    public void setWebUrl(String webUrl) {
        this.webUrl = webUrl;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public Student getStudent() {return this.student;}

    public void setStudent(Student student) {this.student=student;}
}
