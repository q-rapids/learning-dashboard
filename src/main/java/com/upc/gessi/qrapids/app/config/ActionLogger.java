package com.upc.gessi.qrapids.app.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

 /**
  *
  * @author santiago
  */
public class ActionLogger {
    public static final String EXIT = "exits app";
    public static final String TIMEOUT = "'s session has timed out";
    public static final String ENTER = "enters app";


         private final Logger logger = LoggerFactory.getLogger("ActionLogger");

         public ActionLogger() { }
         public void traceEnterApp(String userId, String cookie_token) {
             traceSessionAction(userId, ENTER, cookie_token);
         }
         public void traceExitApp(String userId, String cookie_token) {
            traceSessionAction(userId, EXIT, cookie_token);
         }
         public void traceSessionTimeout(String userId, String cookie_token) {
            traceSessionAction(userId, TIMEOUT, cookie_token);
         }

         public void traceSessionAction(String userId, String action, String sessionId) {
             logger.info("{} {} ({})", userId, action, sessionId);
         }

}