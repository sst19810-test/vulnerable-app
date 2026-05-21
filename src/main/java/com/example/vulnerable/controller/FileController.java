package com.example.vulnerable.controller;

import com.example.vulnerable.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.*;

@RestController
@RequestMapping("/api/files")
public class FileController {

    @Autowired
    private FileService fileService;

    // VULNERABILITY: Path Traversal
    @GetMapping("/read")
    public String readFile(@RequestParam String filename) throws IOException {
        return fileService.readFile(filename);
    }

    // VULNERABILITY: Arbitrary File Upload
    @PostMapping("/upload")
    public String uploadFile(@RequestParam String filename, @RequestParam String content) {
        return fileService.writeFile(filename, content);
    }

    // VULNERABILITY: Command Injection
    @GetMapping("/execute")
    public String executeCommand(@RequestParam String cmd) throws IOException {
        return fileService.executeCommand(cmd);
    }

    // VULNERABILITY: XXE (XML External Entity)
    @PostMapping("/parse-xml")
    public String parseXml(@RequestBody String xmlContent) {
        return fileService.parseXml(xmlContent);
    }

    // VULNERABILITY: Insecure Deserialization
    @PostMapping("/deserialize")
    public Object deserializeObject(@RequestBody byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        ObjectInputStream ois = new ObjectInputStream(bis);
        return ois.readObject();  // Dangerous!
    }
}
