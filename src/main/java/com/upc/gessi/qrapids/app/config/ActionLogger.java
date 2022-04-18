package com.upc.gessi.qrapids.app.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

 /**
  *
  * @author santiago
  */
public class ActionLogger {
    public static final String LOAD = "load page";
    public static final String FINISH_LOAD = "finish load";
    public static final String REDIRECT_TO = "redirect to ";
    public static final String EXIT = "exits app";
     public static final String ENTER = "enters app";


         private final Logger logger = LoggerFactory.getLogger("ActionLogger");

         public ActionLogger() {
         }
         // Old Action
//         public void traceLoadPage(String language, String userId, String sessionId, String currentLocation) {
//            traceAction(language, userId, sessionId, currentLocation,LOAD);
//         }
//
//         public void traceFinishLoadPage(String language, String userId, String sessionId, String currentLocation) {
//            traceAction(language, userId, sessionId, currentLocation, FINISH_LOAD);
//         }
//
//         public void traceExitApp(String language, String userId, String sessionId, String currentLocation) {
//            traceAction(language, userId, sessionId, currentLocation, EXIT);
//         }
//
//         public void traceRedirectTo(String language, String userId, String sessionId, String currentLocation, String  destination) {
//            traceAction(language, userId, sessionId, currentLocation,REDIRECT_TO + destination + " from " + currentLocation);
//         }
//
//         public void traceAction(String language, String userId, String sessionId, String currentLocation, String action) {
//             logger.info("{},{},{},{},{}", language, userId, sessionId, currentLocation, action);
//         }
        public void traceEnterApp(String userId) {
             traceSessionAction(userId, ENTER);
        }
        public void traceExitApp(String userId) {
            traceSessionAction(userId, EXIT);
        }

        public void traceSessionAction(String userId, String action) {
             logger.info("{} {}", userId, action);
        }

}