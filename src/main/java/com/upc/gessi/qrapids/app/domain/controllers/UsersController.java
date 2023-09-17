package com.upc.gessi.qrapids.app.domain.controllers;

import com.upc.gessi.qrapids.app.domain.models.AppUser;
import com.upc.gessi.qrapids.app.domain.models.Project;
import com.upc.gessi.qrapids.app.domain.repositories.AppUser.UserRepository;
import com.upc.gessi.qrapids.app.domain.repositories.Project.ProjectRepository;
import com.upc.gessi.qrapids.app.domain.utils.AnonymizationModes;
import org.springframework.beans.factory.annotation.Autowired;
import com.upc.gessi.qrapids.app.config.libs.AuthTools;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class UsersController {

    @Value("${security.enable}")
    private boolean securityEnable;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;
    private AuthTools authTools;

    public Set<Project> getAllowedProjects(String token, Long id) {
        if (!securityEnable) return new HashSet<>(projectRepository.findAll());
        if (id !=null) {
            Optional<AppUser> user = userRepository.findById(id);
            if (user.isPresent()) {
                AppUser temp = user.get();
                if (temp.getAdmin()){
                    return new HashSet<>(projectRepository.findAll());
                }
                return temp.getAllowedProjects();
            }
        }
        String name = this.authTools.getUserToken(token);
        AppUser user = userRepository.findByUsername(name);
        if (user.getAdmin()) {
            return new HashSet<>(projectRepository.findAll());
        }
        return user.getAllowedProjects();
    }


    public AppUser findUserByName (String userName) {
        return userRepository.findByUsername(userName);
    }

    public void setLastConnection(String username, LocalDateTime date) {

        AppUser user = findUserByName(username);
        user.setDate(date);
        userRepository.save(user);

    }

    public Boolean getIfAdmin(String token) {
        if(!securityEnable) return true;
        String name = this.authTools.getUserToken(token);
        AppUser user = userRepository.findByUsername(name);
        return user.getAdmin();
    }

    public void updateAllowedProjects(List <Long> allowedProjects, Long id) {

        Optional<AppUser> user = userRepository.findById(id);
        AppUser temp = null;
        if (user.isPresent()) {
            temp = user.get();
        }
        temp.removeAllAllowedProjects();
        for(int i=0; i<allowedProjects.size(); ++i) {
            Optional<Project> prj= projectRepository.findById(allowedProjects.get(i));
            Project tempprj=null;
            if(prj.isPresent()) {
                tempprj = prj.get();
            }
            temp.addAllowedProjects(tempprj);
        }
        userRepository.save(temp);
    }

    public AppUser getCurrentUser(){
        Authentication currentUser = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByUsername(currentUser.getPrincipal().toString());
    }

    public boolean hasCurrentUserAnonymousMode(){
        return  getCurrentUser().isAnonymousMode();
    }

    public AnonymizationModes getCurrentUserAnonymizationMode(){
        return getCurrentUser().getAnonymousModeSelected();
    }
}
