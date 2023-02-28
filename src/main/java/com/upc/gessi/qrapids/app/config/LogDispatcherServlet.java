package com.upc.gessi.qrapids.app.config;

import javax.servlet.DispatcherType;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.upc.gessi.qrapids.app.config.libs.AuthTools;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.web.util.WebUtils;
import org.springframework.core.log.LogFormatUtils;
import org.springframework.util.StringUtils;
import org.springframework.http.MediaType;

import java.util.*;
import java.util.stream.Collectors;

import static com.upc.gessi.qrapids.app.config.security.SecurityConstants.COOKIE_STRING;

public class LogDispatcherServlet extends org.springframework.web.servlet.DispatcherServlet {

    private String logURL;
    private AuthTools authTools;

    @Override
    protected void doDispatch(HttpServletRequest request, HttpServletResponse response) throws Exception {
        /*
        if (!(response instanceof ContentCachingResponseWrapper))
            response = new ContentCachingResponseWrapper(response);
        */
        super.doDispatch(request, response);
        //String URL = request.getRequestURI();

        /*
        if (URL.contains("api/me") && ! URL.contains(("api/metrics"))) {
            ContentCachingResponseWrapper wrapper = WebUtils.getNativeResponse(response, ContentCachingResponseWrapper.class);
            if (wrapper != null) {
                byte[] buf = wrapper.getContentAsByteArray();
                int length = Math.min(buf.length, 1024);
                String responseBody = new String(buf, 0, length, wrapper.getCharacterEncoding());
                JsonElement parsedData = new JsonParser().parse(responseBody);
                if (!parsedData.isJsonNull()) {
                    JsonObject data = parsedData.getAsJsonObject();
                    String username = data.get("userName").getAsString();
                    logRequest(logURL, username);
                }
            }
        }
        else if (LogsFilter(getRequestUri(request))) {
            logURL = createLogRequest(request);
        }
         */

        logURL = createLogRequest(request);
        this.authTools = new AuthTools();
        String cookie_token = this.authTools.getCookieToken( request, COOKIE_STRING );
        String username = AuthTools.getUser(cookie_token);
        logRequest(logURL, username);

        /*
        ContentCachingResponseWrapper responseWrapper = WebUtils.getNativeResponse(response, ContentCachingResponseWrapper.class);
        if (responseWrapper != null) {
            responseWrapper.copyBodyToResponse();
        }
         */
    }

    private String createLogRequest(HttpServletRequest request) {
        String params;
        String contentType = request.getContentType();
        if (StringUtils.startsWithIgnoreCase(contentType, "multipart/")) {
            params = "multipart";
        }
        else {
            params = request.getParameterMap().entrySet().stream()
                    .map(entry -> entry.getKey() + ":" + Arrays.toString(entry.getValue()))
                    .collect(Collectors.joining(", "));
        }

        //String queryString = request.getQueryString();
        //String queryClause = (StringUtils.hasLength(queryString) ? "?" + queryString : "");
        String dispatchType = (!DispatcherType.REQUEST.equals(request.getDispatcherType()) ?
                "\"" + request.getDispatcherType() + "\" dispatch for " : "");
        //String message = (dispatchType + request.getMethod() + " \"" + getRequestUri(request) +
                //queryClause + "\", parameters={" + params + "} ");

        String message = (dispatchType + request.getMethod() + " \"" + getRequestUri(request) +
            "\", parameters={" + params + "} ");

        if (logger.isTraceEnabled()) {
            List<String> values = Collections.list(request.getHeaderNames());
            String headers = values.size() > 0 ? "masked" : "";
            if (isEnableLoggingRequestDetails()) {
                headers = values.stream().map(name -> name + ":" + Collections.list(request.getHeaders(name)))
                        .collect(Collectors.joining(", "));
            }
            return message + ", headers={" + headers + "} in DispatcherServlet '" + getServletName() + "'";
        }
        else return message;
    }

    /*
    private String getParamsFromRequest(HttpServletRequest request) {
        Enumeration<String> paramsNamesEnum = request.getParameterNames();
        StringBuilder paramsInfo = new StringBuilder();
        boolean first = true;
        while (paramsNamesEnum.hasMoreElements()) {
            String name = paramsNamesEnum.nextElement();
            String[] values = request.getParameterValues(name);
            for (String value : values) {
                if (!first) paramsInfo.append(", Name: ").append(name).append(" Value: ").append(value);
                else paramsInfo.append("Name: ").append(name).append(" Value: ").append(value);
            }
        }
        logger.debug(paramsInfo.toString());
        return paramsInfo.toString();
    }
     */

    private void logRequest(String message, String username) {
        if (username != null) {
            LogFormatUtils.traceDebug(logger, traceOn ->
                    message + "- Action performed by " + username);
        }
        else {
            LogFormatUtils.traceDebug(logger, traceOn ->
                    message + "- No user is logged in yet");
        }
    }

    private static String getRequestUri(HttpServletRequest request) {
        String uri = (String) request.getAttribute(WebUtils.INCLUDE_REQUEST_URI_ATTRIBUTE);
        if (uri == null) {
            uri = request.getRequestURI();
        }
        return uri;
    }

    /*
    private boolean LogsFilter(String URL) {
        return  !URL.contains("api") && !URL.contains("js") && !URL.contains("css") &&
                !URL.contains("icons") && !URL.contains(".ico") && !URL.contains("fonts") &&
                !URL.contains("ws") && !URL.contains("elasticsearch") && !URL.contains("http-outgoing-") &&
                !URL.contains("Ant") && !URL.contains("FORWARD") && !URL.contains("doesn't match");
    }
     */

}
