package com.upc.gessi.qrapids.app.domain.models;

import javax.persistence.*;

@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(name = "UniqueProjectIdentity", columnNames = {"dataSource", "projectId"})
})
public class ProjectIdentity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dataSource")
    private DataSource dataSource;

    @Column(name = "url")
    private String url;

    @ManyToOne
    @JoinColumn(name="projectId", referencedColumnName = "id")
    private Project project;

    public ProjectIdentity() {
    }

    public ProjectIdentity(DataSource dataSource, String url, Project project) {
        this.dataSource = dataSource;
        this.url = url;
        this.project = project;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }
}
