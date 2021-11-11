package com.upc.gessi.qrapids.app.domain.controllers;

import com.upc.gessi.qrapids.app.domain.models.AppUser;
import com.upc.gessi.qrapids.app.domain.models.Project;
import com.upc.gessi.qrapids.app.domain.repositories.AppUser.UserRepository;
import com.upc.gessi.qrapids.app.domain.repositories.Project.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import com.upc.gessi.qrapids.app.config.libs.AuthTools;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class UsersController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;
    private AuthTools authTools;

    public Set<Project> getAllowedProjects(String token) {
        String name = this.authTools.getUserToken(token);
        AppUser user = userRepository.findByUsername(name);
        if(user.getAdmin()) {
            return new HashSet<>(projectRepository.findAll());
        }
        return user.getAllowedProjects();
    }

    public AppUser findUserByName (String userName) {
        return userRepository.findByUsername(userName);
    }

    public Boolean getIfAdmin(String token) {
        String name = this.authTools.getUserToken(token);
        AppUser user = userRepository.findByUsername(name);
        if(user.getAdmin()) return true;
        else return false;
    }
}
