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
    @RequestMapping(value = {"/", "/{path:[^\\.]*}", "/{path:^(?!api|auth|admin|content|quizzes|scores|user|uploads).*$}/**"})
    public String forward() {
        return "forward:/index.html";
    }
}