package com.upc.gessi.qrapids.app.domain.controllers;

import com.upc.gessi.qrapids.app.domain.models.AppUser;
import com.upc.gessi.qrapids.app.domain.models.Project;
import com.upc.gessi.qrapids.app.domain.repositories.AppUser.UserRepository;
import com.upc.gessi.qrapids.app.domain.repositories.Project.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import com.upc.gessi.qrapids.app.config.libs.AuthTools;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UsersController {

    @Value("${security.enable}")
    private boolean securityEnable;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;
    private AuthTools authTools;

    public List<Project> getAllowedProjects(String token) {
        if(!securityEnable) return projectRepository.findAll();;
        String name = this.authTools.getUserToken(token);
        AppUser user = userRepository.findByUsername(name);
        if(user.getAdmin()) {
            return projectRepository.findAll();
        }
        return user.getAllowedProjects();
    }

    public AppUser findUserByName (String userName) {
        return userRepository.findByUsername(userName);
    }

    public Boolean getIfAdmin(String token) {
        if(!securityEnable) return true;
        String name = this.authTools.getUserToken(token);
        AppUser user = userRepository.findByUsername(name);
        return user.getAdmin();
    }
}
