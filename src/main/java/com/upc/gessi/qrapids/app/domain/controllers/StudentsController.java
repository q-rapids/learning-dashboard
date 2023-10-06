package com.upc.gessi.qrapids.app.domain.controllers;

import com.upc.gessi.qrapids.app.domain.adapters.QMA.QMAMetrics;
import com.upc.gessi.qrapids.app.domain.models.*;
import com.upc.gessi.qrapids.app.domain.repositories.Metric.MetricRepository;
import com.upc.gessi.qrapids.app.domain.repositories.Project.ProjectRepository;
import com.upc.gessi.qrapids.app.domain.repositories.Student.StudentRepository;
import com.upc.gessi.qrapids.app.domain.repositories.StudentIdentity.StudentIdentityRepository;
import com.upc.gessi.qrapids.app.domain.utils.AnonymizationModes;
import com.upc.gessi.qrapids.app.presentation.rest.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class StudentsController {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private StudentIdentityRepository studentIdentityRepository;

    @Autowired
    private QMAMetrics qmaMetrics;

    @Autowired
    private MetricRepository metricRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private QualityFactorMetricsController qualityFactorMetricsController;

    @Autowired
    private MetricsController metricsController;

    @Autowired
    private UsersController usersController;



    public DTOStudent getDTOStudentFromStudent(Student student){
        List<StudentIdentity> studentIdentities = studentIdentityRepository.findAllByStudent(student);

        Map<DataSource, DTOStudentIdentity> DTOStudentIdentities = new HashMap<>();

        studentIdentities.forEach(identity -> DTOStudentIdentities.put(identity.getDataSource(), new DTOStudentIdentity(identity.getDataSource(), identity.getUsername())));

        return new DTOStudent(student.getId(),student.getName(),DTOStudentIdentities);
    }


    public List<DTOStudent> getStudentsDTOFromProject(Long projectId){
        List<Student> students = studentRepository.findAllByProjectIdOrderByName(projectId);
        List<DTOStudent> dtoStudents = new ArrayList<>();
        for(Student s:students) {
            dtoStudents.add(getDTOStudentFromStudent(s));
        }
        return dtoStudents;
    }

    public List<Student> getStudentsFromProject(Long projectId){
        return studentRepository.findAllByProjectIdOrderByName(projectId);
    }

    public List<StudentIdentity> getStudentIdentities(Student student){
        return studentIdentityRepository.findAllByStudent(student);
    }

    public void addDTOMetricEvaluationToMetricList(String metricExternalId, LocalDate from, LocalDate to, String profileId, String projectExternalId, List<DTOMetricEvaluation> metrics) throws IOException {
        if(from == null || to == null){//not historical
            metrics.add(qmaMetrics.SingleCurrentEvaluation(metricExternalId,projectExternalId));

        } else { //historical
            metrics.addAll(qmaMetrics.SingleHistoricalData(metricExternalId , from, to, projectExternalId, profileId));
        }
    }

    public void anonymizeStudentsFromProject(Project project, AnonymizationModes anonymizationMode){
        List<Student> students = getStudentsFromProject(project.getId());

        Map<Long,String> studentAnonymizedNames = getAnonymizedStudentNames(students, anonymizationMode);

        students.forEach(student -> {
            student.setName(studentAnonymizedNames.get(student.getId()));
        });

        studentRepository.saveAll(students);
    }

    public Map<Long, String> getAnonymizedStudentNames(List<Student> students, AnonymizationModes anonymizationMode){

        Map<Long, String> studentNormalizedNames = new HashMap<>();

            students.forEach(student -> {
                boolean uniqueName = false;
                int it = 0;
                while(!uniqueName) {
                    String anonymizedName = getAnonymizedName(student.getId().intValue(), it, anonymizationMode);

                    if (! studentNormalizedNames.containsValue(anonymizedName)) {
                        studentNormalizedNames.put(student.getId(), anonymizedName);
                        uniqueName = true;
                    }
                    ++it;
                }
            });

            return studentNormalizedNames;
    }

    public Map<Long, String> getNormalizedNamesByProject(Project project) {

        //Gets anonymize variable from the current user (request) context
        boolean anonymize = usersController.hasCurrentUserAnonymousMode() && ! project.isAnonymized();

        List<Student> students = studentRepository.findAllByProjectIdOrderByName(project.getId());

        if(anonymize){
            return getAnonymizedStudentNames(students, usersController.getCurrentUserAnonymizationMode());
        }

        Map<Long, String> studentNormalizedNames = new HashMap<>();

        students.forEach(student -> {
            studentNormalizedNames.put(student.getId(), student.getName());
        });

        return studentNormalizedNames;
    }

    public List<DTOStudentMetrics> getStudentMetricsFromProject(String projectExternalId, LocalDate from, LocalDate to, String profileId) throws IOException {
        Project project = projectRepository.findByExternalId(projectExternalId);

        List<DTOStudent> students = getStudentsDTOFromProject(project.getId());

        List<DTOStudentMetrics> dtoStudentMetrics = new ArrayList<>();

        Map<Long, String> normalizedNames = getNormalizedNamesByProject(project);

        System.out.println("JEJEJEJEJE: " + normalizedNames); //Aqui llega bien


        for(DTOStudent s : students) {
            List<Metric> metrics = metricRepository.findAllByStudentIdOrderByName(s.getId());
            Integer number = metrics.size();
            Map<DataSource, List<DTOMetricEvaluation>> dataSourceMetrics = new HashMap<>();

            for(DataSource source : DataSource.values()){
                dataSourceMetrics.put(source, new ArrayList<>());
            }

            List<DTOMetricEvaluation> metricListNoSource = new ArrayList<>();


            for(Metric m : metrics) {
                String typeOfFactor = qualityFactorMetricsController.getTypeFromFactorOfMetric(m);
                if(typeOfFactor == null){
                    addDTOMetricEvaluationToMetricList(String.valueOf(m.getExternalId()),from, to, profileId, projectExternalId, metricListNoSource);
                } else {
                    try {
                        DataSource source = DataSource.valueOf(typeOfFactor);
                        addDTOMetricEvaluationToMetricList(String.valueOf(m.getExternalId()),from, to, profileId, projectExternalId, dataSourceMetrics.get(source));
                    } catch (IllegalArgumentException exception) {
                        addDTOMetricEvaluationToMetricList(String.valueOf(m.getExternalId()),from, to, profileId, projectExternalId, metricListNoSource);
                    }
                }
            }

            DataSource source = DataSource.values()[0];
            List<DTOMetricEvaluation> orderedMetricList = dataSourceMetrics.get(source);

            dataSourceMetrics.forEach((dataSource, metricList) -> {
                if(! dataSource.equals(source)){
                    orderedMetricList.addAll(metricList);
                }
            });

            orderedMetricList.addAll(metricListNoSource);

            Map<DataSource, DTOStudentIdentity> dtoStudentIdentities = s.getIdentities();


            //normalize names
            metricsController.normalizeMetricsEvaluation(orderedMetricList, students, normalizedNames);

            DTOStudentMetrics temp = new DTOStudentMetrics(normalizedNames.get(s.getId()), dtoStudentIdentities, orderedMetricList, number);
            dtoStudentMetrics.add(temp);
        }
        return dtoStudentMetrics;
    }

    public String normalizedName(String name, List<DTOStudentIdentity> studentIdentities,  String replaceName){
        String normalizedMetricName = name;
        int i = 0;
        while (normalizedMetricName.equals(name) && i < studentIdentities.size()) {
            String username = studentIdentities.get(i).getUsername();
            if (username != null && name.contains(username)) {
                normalizedMetricName = name.replace(studentIdentities.get(i).getUsername(), replaceName);
            }
            ++i;
        }
        return normalizedMetricName;
    }
    //Deprecated
    public String getAnonymizedRandomName(Integer id, Integer offset) {

        int weekNumber = LocalDate.now().getDayOfYear() / 7; // Get the week number

        long seed = ((long)id << 32) + (weekNumber + offset); // Random Seed based on week number and offset

        SecureRandom random = null;

        try {
            random = SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        random.setSeed(seed);


        int leftLimit = 48; // numeral '0'
        int rightLimit = 122; // letter 'z'
        int targetStringLength = 5;

        return random.ints(leftLimit, rightLimit + 1)
                .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                .limit(targetStringLength)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();

    }

    public String getAnonymizedName(Integer id, Integer offset, AnonymizationModes mode) {

        int weekNumber = LocalDate.now().getDayOfYear() / 7; // Get the week number

        long seed = ((long)id << 32) + (weekNumber + offset); // Random Seed based on week number and offset

        SecureRandom random = null;

        try {
            random = SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        random.setSeed(seed);

        Enum<?>[] selectedMode = mode.getValues();

        int length = selectedMode.length;
        int value = random.nextInt(length);

        String anonymizedName = selectedMode[value].toString();
        return anonymizedName.replace("_"," ");
    }

    public Long updateStudentAndMetrics(Long studentID, DTOStudent studentDTO, List<Long> metricsIds, String projectExternalId) {

        Project externalProj = projectRepository.findByExternalId(projectExternalId);
        Optional<Student> studentSearchResult = studentRepository.findStudentById(studentID);

        Student student = null;

        boolean modifiedIdentity = false;
        if(studentSearchResult.isPresent()) {
            student = studentSearchResult.get();

            if(! student.getName().equals(studentDTO.getName()))
                modifiedIdentity = true;

            updateStudent(student,studentDTO.getName(),studentDTO.getIdentities(), metricsIds);
        }
        else {
            student = createStudent(studentDTO, externalProj, metricsIds);
            modifiedIdentity = true;
        }
        if (modifiedIdentity) {
            externalProj.setAnonymized(false);
            projectRepository.save(externalProj);
        }
        return student.getId();
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
            List<StudentIdentity> identities = studentIdentityRepository.findAllByStudent(student);

            studentIdentityRepository.deleteAll(identities);
            studentRepository.delete(student);
        }

    }

    public Student createStudent(DTOStudent studentDTO, Project project, List<Long> metricsIds){
        Student student =  new Student(studentDTO.getName(), project);
        Map<DataSource, DTOStudentIdentity> identities = studentDTO.getIdentities();

        studentRepository.save(student);

        List<StudentIdentity> new_identities = new ArrayList<>();

        identities.forEach(((dataSource, studentIdentity) -> new_identities.add(new StudentIdentity(dataSource,studentIdentity.getUsername(), student))));

        studentIdentityRepository.saveAll(new_identities);

        metricsController.updateStudentMetricsByIds(metricsIds, student);
        return student;
    }

    public void updateStudent(Student student, String studentName, Map<DataSource, DTOStudentIdentity>  newIdentities,  List<Long> metricsIds){
        student.setName(studentName);

        List<StudentIdentity> previousIdentities = studentIdentityRepository.findAllByStudent(student);

        newIdentities.forEach( (dataSource, studentIdentity) -> {
            AtomicBoolean found = new AtomicBoolean(false);

            previousIdentities.forEach(previousIdentity -> {
                if(dataSource.equals(previousIdentity.getDataSource())){
                    previousIdentity.setUsername(studentIdentity.getUsername());
                    found.set(true);
                }
            });

            if(!found.get()){
                previousIdentities.add(new StudentIdentity(dataSource,studentIdentity.getUsername(), student));
            }
        });


        studentRepository.save(student);
        studentIdentityRepository.saveAll(previousIdentities);

        List<Metric> metrics = metricRepository.findAllByStudentId(student.getId());
        for (Metric m : metrics) {
            m.setStudent(null);
            metricRepository.save(m);
        }

        metricsController.updateStudentMetricsByIds(metricsIds, student);
    }
}
