package com.upc.gessi.qrapids.app.config.security;

import com.upc.gessi.qrapids.app.config.libs.AuthTools;
import com.upc.gessi.qrapids.app.config.libs.RouteFilter;
import com.upc.gessi.qrapids.app.domain.models.AppUser;
import com.upc.gessi.qrapids.app.domain.repositories.AppUser.UserRepository;
import com.upc.gessi.qrapids.app.domain.models.Route;
import com.upc.gessi.qrapids.app.domain.repositories.Route.RouteRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.upc.gessi.qrapids.app.config.security.SecurityConstants.*;

public class JWTAuthorizationFilter extends BasicAuthenticationFilter {

	private AuthTools authTools;
	private RouteFilter routeFilter;

    private UserRepository userRepository;

    private RouteRepository routeRepository;

	private boolean DEBUG = false;

	private Logger oldlogger = LoggerFactory.getLogger(JWTAuthorizationFilter.class);

    private java.util.logging.Logger logger = java.util.logging.Logger.getLogger("navigation");

	public JWTAuthorizationFilter(AuthenticationManager authManager) {
		super(authManager);
	}

    public JWTAuthorizationFilter(AuthenticationManager authManager, UserRepository userRepository, RouteRepository routeRepository ) {
        super(authManager);
        this.userRepository = userRepository;
        this.routeRepository = routeRepository;
    }

	/**
	 * Extraemos datos de la petición, Cabecera de autenticación
	 * @param req
	 * @param res
	 * @param chain
	 * @throws IOException
	 * @throws ServletException
	 */
	@Override
	protected void doFilterInternal(HttpServletRequest req,
									HttpServletResponse res,
									FilterChain chain) throws IOException, ServletException {

        SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(this);

        this.authTools = new AuthTools();
        this.routeFilter = new RouteFilter();
        // is an External request? true -> WebPage : false -> external
        // boolean origin = this.authTools.originRequest( req );



        // Authorization object
		UsernamePasswordAuthenticationToken authentication;

		// Get header & cookie auth string variables
        String header = req.getHeader(HEADER_STRING);
        String cookie_token = this.authTools.getCookieToken( req, COOKIE_STRING );
        String token = "";

        if ( cookie_token != null && cookie_token != "" && !cookie_token.isEmpty() ) {
            // WeaApp Client internal application

            authentication = this.authTools.tokenValidation( cookie_token );
            token = cookie_token;

            logMessage(" Origin - WebApp ");

        } else {

            // External application API Access
            if( header == null || !header.startsWith(TOKEN_PREFIX) ){

                logMessage(" No token API ");

                chain.doFilter(req, res);

                return;
            }

            authentication = getAuthentication( req );
            token = req.getHeader( HEADER_STRING );

            logMessage(" Origin - ApiCall ");

        }

        /** --[ Filter implementation ]-- */

        // Flag valitation
        boolean isAllowed = false;

        // Global route
        String origin_request = req.getRequestURI();

        // User container
        AppUser user = null;

        // List of routes container
        List<Route> routes = new ArrayList<>();

        // Public resources
        isAllowed = this.routeFilter.publicURLAttemp( origin_request );

        if (! isAllowed )
            isAllowed = this.routeFilter.globalURLAttemp( origin_request );

        // We verify if route is a public resource
        if( ! isAllowed ) {

            // User data from DB
            user = this.userRepository.findByUsername( this.authTools.getUserToken( token ) );


            if ( user!=null && user.getAdmin() )
                isAllowed = true;


            // Test elements and try to verify if the current route is allowed for the current user
            else{
                //isAllowed = true;
                // Cast set object ot List of AppUSers
                // Old version that checks if the route is allowed for a not admin user
                //routes.addAll( user.getUserGroup().getRoutes() );
                //isAllowed = this.routeFilter.filterShiled( origin_request, token, routes );
                // New version.
                isAllowed = !this.routeFilter.filterShiled( origin_request, token, routeRepository.findAll());
            }

        }

        /** [ Route Filtering ] */

        // Verfiy an redirect if user does not have permission to use the current route.
        if ( ! isAllowed ){

            res.sendRedirect( "/login?error=User+does+not+have+permission" );

        } else {

            // Request origin
            boolean origin = this.authTools.originRequest( req );

           //if( !req.getRequestURI().contains("js") && !req.getRequestURI().contains("css") && !req.getRequestURI().contains("app")) {
                /*String token_new = Jwts.builder()
                        .setSubject("aleix")
                        .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_JWT_TOKEN_TIME))
                        .signWith(SignatureAlgorithm.HS512, SECRET.getBytes())
                        .compact();*/
                Cookie cookie = new Cookie(COOKIE_STRING, null); // Not necessary, but saves bandwidth.
		        cookie.setHttpOnly(true);
		        cookie.setMaxAge(0); // Don't set to -1 or it will become a session cookie!
		        res.addCookie(cookie);

                // Web Application
                // Set token auth in HTTP Only cookie client.
                Cookie qrapids_token_client = new Cookie(COOKIE_STRING, token);

                // Configuration
                // Changed HttpOnly to false to read it from the application
                qrapids_token_client.setHttpOnly(true);
                qrapids_token_client.setMaxAge((int) EXPIRATION_COOKIE_TIME / 1000);
                qrapids_token_client.setPath("/");

                res.addCookie(qrapids_token_client);
            //}


            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
            LocalDateTime now = LocalDateTime.now();
            logMessage(origin_request + " <- -> [Final status] : " + isAllowed);
            if(user!=null)logger.info(user.getUsername() + " goes to " + origin_request+ " " + now);

            SecurityContextHolder.getContext().setAuthentication(authentication);
            chain.doFilter(req, res);

        }

	}

	private void logMessage (String message) {
        if (this.DEBUG)
            oldlogger.info(message);
    }

	/**
	 * Obtención de token de la cabecera previamente obtenida en doFilterInternal
	 * Obteción de datos de usuario serializados
	 * @param request
	 * @return
	 */
	private UsernamePasswordAuthenticationToken getAuthentication(HttpServletRequest request) {

		String token = request.getHeader(HEADER_STRING);

		return this.authTools.tokenValidation( token );

	}

}
