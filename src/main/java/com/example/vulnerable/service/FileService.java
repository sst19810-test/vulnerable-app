package com.example.vulnerable.service;

import com.example.vulnerable.util.XmlParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

@Service
public class FileService {

    @Autowired
    private XmlParser xmlParser;

    // VULNERABILITY: Path Traversal
    public String readFile(String filename) throws IOException {
        File file = new File("/tmp/" + filename);  // No path validation
        BufferedReader reader = new BufferedReader(new FileReader(file));
        StringBuilder content = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            content.append(line).append("\n");
        }
        reader.close();
        return content.toString();
    }

    // VULNERABILITY: Arbitrary file write
    public String writeFile(String filename, String content) {
        try {
            // VULNERABILITY: No validation of filename or path
            FileWriter writer = new FileWriter("/tmp/" + filename);
            writer.write(content);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            // Reading XML from file (in real apps this often comes from user input)
            File xmlFile = new File("malicious.xml");
            Document doc = builder.parse(xmlFile);

            // Extract and print user data (simulating real processing)
            NodeList users = doc.getElementsByTagName("username");
            for (int i = 0; i < users.getLength(); i++) {
                System.out.println("Username: " + users.item(i).getTextContent());
            }
            return "File written successfully";
        } catch (IOException e) {
            return "Error: " + e.getMessage();  // VULNERABILITY: Error message disclosure
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        } catch (SAXException e) {
            throw new RuntimeException(e);
        }
    }

    // VULNERABILITY: Command Injection
    public String executeCommand(String cmd) throws IOException {
        Runtime runtime = Runtime.getRuntime();
        Process process = runtime.exec(cmd);  // Directly executing user input

        BufferedReader reader = new BufferedReader(
            new InputStreamReader(process.getInputStream())
        );

        StringBuilder output = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }
        return output.toString();
    }

    // VULNERABILITY: XXE
    public String parseXml(String xmlContent) {
        return xmlParser.parse(xmlContent);
    }

    // VULNERABILITY: Zip Slip
    public void unzip(String zipFilePath, String destDirectory) throws IOException {
        java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(
            new FileInputStream(zipFilePath)
        );

        java.util.zip.ZipEntry zipEntry = zis.getNextEntry();
        while (zipEntry != null) {
            // VULNERABILITY: No validation of zip entry path
            File newFile = new File(destDirectory + File.separator + zipEntry.getName());
            new File(newFile.getParent()).mkdirs();
            FileOutputStream fos = new FileOutputStream(newFile);

            byte[] buffer = new byte[1024];
            int len;
            while ((len = zis.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }
            fos.close();
            zipEntry = zis.getNextEntry();
        }
        zis.closeEntry();
        zis.close();
    }
}
