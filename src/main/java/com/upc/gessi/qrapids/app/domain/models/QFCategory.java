package com.upc.gessi.qrapids.app.domain.models;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name="quality_factor_categories")
public class QFCategory implements Serializable {

    // SerialVersion UID
    private static final long serialVersionUID = 16L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="type")
    private String type;

    @Column(name="name")
    private String name;

    @Column(name="color")
    private String color;

    @Column(name="upperThreshold")
    private float upperThreshold;


    public QFCategory() {
    }

    public QFCategory(String name, String color, float upperThreshold, String type) {
        this.name = name;
        this.color = color;
        this.upperThreshold = upperThreshold;
        this.type=type;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public float getUpperThreshold() {
        return upperThreshold;
    }

    public void setUpperThreshold(float upperThreshold) {
        this.upperThreshold = upperThreshold;
    }
}
