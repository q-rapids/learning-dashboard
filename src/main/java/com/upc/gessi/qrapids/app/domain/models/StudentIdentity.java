package com.upc.gessi.qrapids.app.domain.models;

import com.upc.gessi.qrapids.app.domain.utils.DataEncryptDecryptConverter;

import javax.persistence.*;

@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(name = "UniqueStudentIdentity", columnNames = {"dataSource", "studentId"})
})
public class StudentIdentity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dataSource")
    private DataSource dataSource;

    @Column(name = "username")
    @Convert(converter = DataEncryptDecryptConverter.class)
    private String username;

    @ManyToOne
    @JoinColumn(name="studentId", referencedColumnName = "id")
    private Student student;

    public StudentIdentity() {
    }

    public StudentIdentity(DataSource dataSource, String username, Student student) {
        this.dataSource = dataSource;
        this.username = username;
        this.student = student;
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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }
}
