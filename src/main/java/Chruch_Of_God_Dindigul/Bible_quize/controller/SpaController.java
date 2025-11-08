package Chruch_Of_God_Dindigul.Bible_quize.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * This controller is responsible for forwarding all non-API, non-static-file
 * requests to the index.html page. This is the standard mechanism to support
 * client-side routing in a Single Page Application (SPA) built with frameworks
 * like React, Angular, or Vue.
 */
@Controller
public class SpaController {
    // This mapping acts as a catch-all for any path that does not contain a dot (i.e., is not a file)
    // and is not an API route. It forwards the request to index.html, allowing the client-side
    // router to handle it. This fixes 404 errors for pages like /setup, /dashboard, etc.
    // CRITICAL FIX: The pattern uses a negative lookahead `(?!api|uploads|error|.*\\..*$)` to match
    // any path that does NOT start with 'api', 'uploads', 'error', and does NOT contain a dot.
    // This is the most robust way to separate SPA routes from API calls and static file requests.
    @RequestMapping(value = {"/", "/{path:(?!api|uploads|error|.*\\..*$).*}/**"})
    public String forward() {
        return "forward:/index.html";
    }
}