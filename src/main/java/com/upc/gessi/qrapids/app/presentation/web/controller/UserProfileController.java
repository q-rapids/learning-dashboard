package com.upc.gessi.qrapids.app.presentation.web.controller;

import com.upc.gessi.qrapids.app.config.libs.AuthTools;
import com.upc.gessi.qrapids.app.domain.models.AppUser;
import com.upc.gessi.qrapids.app.domain.repositories.AppUser.UserRepository;
import com.upc.gessi.qrapids.app.domain.repositories.Question.QuestionRepository;
import com.upc.gessi.qrapids.app.domain.repositories.UserGroup.UserGroupRepository;
import com.upc.gessi.qrapids.app.domain.utils.AnonymizationModes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.validation.Valid;

import java.util.Optional;

import static com.upc.gessi.qrapids.app.config.security.SecurityConstants.COOKIE_STRING;

@Controller
@RequestMapping("/profile")
public class UserProfileController {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private QuestionRepository questionRepository;
    @Autowired
    private UserGroupRepository userGroupRepository;

    private BCryptPasswordEncoder bCryptPasswordEncoder;

    private Logger logger = LoggerFactory.getLogger(UserProfileController.class);

    private final String redirectTo = "/profile";

    private static final String REDIRECT = "redirect:";

    public UserProfileController(BCryptPasswordEncoder bCryptPasswordEncoder) {
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
    }

    /**
     * Read all clients by pages
     * @return
     */
    @GetMapping
    private ModelAndView index(@CookieValue(COOKIE_STRING) String token, Pageable page) {
        // Tools users validation
        AuthTools authTools = new AuthTools();
        String userName = authTools.getUserToken( token );

        ModelAndView view = new ModelAndView("/AppUser/Profile");
        try{
            view.addObject( "questions", this.questionRepository.findAll());
            view.addObject("defautlUserGroup", this.userGroupRepository.findByDefaultGroupIsTrue() );
            view.addObject("appuser", this.userRepository.findByUsername( userName ));
            view.addObject("currentAnonymous", this.userRepository.findByUsername( userName ).getAnonymousModeSelected().getModeName());
            view.addObject("anonymizationModes", AnonymizationModes.values());
        }catch (Exception e){
            logger.error(e.getMessage(), e);
        }
        return view;
    }

    /**
     * Return users view update
     * @param id
     * @return
     */
    @RequestMapping(value = "/{id}/update", method = RequestMethod.GET)
    public ModelAndView showUpdateView(@PathVariable("id") Long id) {
        Optional<AppUser> userOptional = this.userRepository.findById(id);
        if (userOptional.isPresent()) {
            AppUser user = userOptional.get();
            ModelAndView modelAndView = new ModelAndView("/AppUser/update");
            modelAndView.addObject("appuser", user);
            return modelAndView;
        } else {
            return new ModelAndView("redirect:/home?error=User+not+found");
        }
    }

    /**
     * Update persistent unit
     * @param user
     * @return
     */
    @RequestMapping(value = "/update", method = RequestMethod.POST)
    public String updateEntity(@ModelAttribute(value = "appuser") @Valid AppUser user ) {
        try{
            Optional<AppUser> userOptional = this.userRepository.findById(user.getId());
            if(userOptional.isPresent()) {
                AppUser userUpdate = userOptional.get();
                userUpdate.setEmail(user.getEmail());
                if(!(user.getAnonymousModeSelected() == null)) {
                    userUpdate.setAnonymousMode(user.isAnonymousMode());
                    userUpdate.setAnonymousModeSelected(user.getAnonymousModeSelected());
                }
                if(!(user.getAppuser_question() == null))
                    userUpdate.setAppuser_question(user.getAppuser_question());
                if (!(user.getQuestion() == null))
                    userUpdate.setQuestion(bCryptPasswordEncoder.encode( user.getQuestion()));
                if (!(user.getPassword() == null))
                    userUpdate.setPassword(bCryptPasswordEncoder.encode( user.getPassword()));

                this.userRepository.save(userUpdate);
                return REDIRECT + this.redirectTo + "?success=" + "Data updated".replace(" ","+");
            } else {
                return REDIRECT + this.redirectTo + "?error=" + "User not found".replace(" ","+");
            }
        } catch( Exception e ){
            return REDIRECT + this.redirectTo + "?error=" + "Something went wrong".replace(" ","+");
        }

    }

}

