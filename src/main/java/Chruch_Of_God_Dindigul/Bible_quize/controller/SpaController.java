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
    // router to handle it. This fixes 404 errors on page reload for routes like /setup, /dashboard, etc.
    // CRITICAL FIX: This pattern is now aligned with SecurityConfig and JwtAuthFilter for consistent SPA route handling.
    @RequestMapping(value = {"/", "/**/{path:[^\\.]*}"})
    public String forward() {
        return "forward:/index.html";
    }
}