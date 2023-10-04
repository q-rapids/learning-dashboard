package com.upc.gessi.qrapids.app.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.upc.gessi.qrapids.app.config.ActionLogger;
import com.upc.gessi.qrapids.app.config.libs.AuthTools;
import com.upc.gessi.qrapids.app.domain.controllers.ProjectsController;
import com.upc.gessi.qrapids.app.domain.controllers.UsersController;
import com.upc.gessi.qrapids.app.domain.models.AppUser;
import com.upc.gessi.qrapids.app.domain.repositories.AppUser.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Logger;

import static com.upc.gessi.qrapids.app.config.security.SecurityConstants.*;


public class JWTAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

	private AuthenticationManager authenticationManager;

	private UsersController usersController;

	private SessionTimer sessionTimer;

	// √Tools Auth
	AuthTools authTools;

	public JWTAuthenticationFilter(AuthenticationManager authenticationManager, UsersController usersController) {
		this.authenticationManager = authenticationManager;
		this.usersController=usersController;
	}

	/**
	 * Login Request attempt
	 * @param req
	 * @param res
	 * @return
	 * @throws AuthenticationException
	 */
	@Override
	public Authentication attemptAuthentication(HttpServletRequest req,
												HttpServletResponse res) throws AuthenticationException {

		// AppUser credentials
		AppUser creds;

		try { // JSON Request
			creds = new ObjectMapper()
					.readValue(req.getInputStream(), AppUser.class);

		} catch (IOException e) {

			// Form validation
			String username = req.getParameter("username");
			String password = req.getParameter("password");

			if( "".equals(username) || username.isEmpty() || "".equals(password) || password.isEmpty() ) {
				throw new RuntimeException(e);
			} else {
				// Form has data of autentication
				creds = new AppUser();
				creds.setUsername( username );
				creds.setPassword( password );
			}
		}

		return authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(
						creds.getUsername(),
						creds.getPassword(),
						new ArrayList<>())
		);
	}

	/**
	 * Auth Return parameters
	 * @param req
	 * @param res
	 * @param chain
	 * @param auth
	 * @throws IOException
	 * @throws ServletException
	 */
	@Override
	protected void successfulAuthentication(HttpServletRequest req,
											HttpServletResponse res,
											FilterChain chain,
											Authentication auth) throws IOException, ServletException {

		// Auth tools
		this.authTools = new AuthTools();

		// Token creation
		String token = Jwts.builder()
				.setSubject(((org.springframework.security.core.userdetails.User) auth.getPrincipal()).getUsername())
				.setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_JWT_TOKEN_TIME))
				.signWith(SignatureAlgorithm.HS512, SECRET.getBytes())
				.compact();

		// Request origin
		boolean origin = this.authTools.originRequest( req );

		if( origin ){

			// Web Application
            // Set token auth in HTTP Only cookie client.
			Cookie qrapids_token_client = new Cookie( COOKIE_STRING, token);
			this.sessionTimer = SessionTimer.getInstance();

			ActionLogger al = new ActionLogger();
			String username = ((User) auth.getPrincipal()).getUsername();
			al.traceEnterApp(username, token);
			DateTimeFormatter dtf = DateTimeFormatter.ofPattern("uuuu/MM/dd HH:mm:ss");
			//userController.setLastConnection(username, dtf.format(now));

			// Configuration
			// Changed HttpOnly to false to read it from the application
			qrapids_token_client.setHttpOnly( true );
			qrapids_token_client.setMaxAge(  (int) EXPIRATION_COOKIE_TIME / 1000 );

			res.addCookie( qrapids_token_client );
			sessionTimer.startTimer(username, token, (int) EXPIRATION_COOKIE_TIME / 1000);

			res.sendRedirect("StrategicIndicators/CurrentChart");

		} else {
			// API send header with token auth
			res.addHeader(HEADER_STRING, TOKEN_PREFIX + token);
		}
	}


}
