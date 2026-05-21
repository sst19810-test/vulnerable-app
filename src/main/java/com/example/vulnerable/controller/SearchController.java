package com.example.vulnerable.controller;

import org.springframework.web.bind.annotation.*;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    // VULNERABILITY: Expression Language Injection
    @GetMapping("/eval")
    public String evaluateExpression(@RequestParam String expression) throws ScriptException {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("JavaScript");
        Object result = engine.eval(expression);  // Code injection vulnerability
        return result.toString();
    }

    // VULNERABILITY: Server-Side Request Forgery (SSRF)
    @GetMapping("/fetch")
    public String fetchUrl(@RequestParam String url) throws Exception {
        java.net.URL urlObj = new java.net.URL(url);
        java.io.BufferedReader reader = new java.io.BufferedReader(
            new java.io.InputStreamReader(urlObj.openStream())
        );
        StringBuilder content = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            content.append(line);
        }
        reader.close();
        return content.toString();
    }

    // VULNERABILITY: Open Redirect
    @GetMapping("/redirect")
    public void redirect(@RequestParam String url, javax.servlet.http.HttpServletResponse response) throws Exception {
        response.sendRedirect(url);  // No validation
    }
}
