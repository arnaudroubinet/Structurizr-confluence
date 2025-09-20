package com.structurizr.confluence.processor;

import com.structurizr.Workspace;
import com.structurizr.view.View;
import com.structurizr.view.ViewSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Exports diagrams from a Structurizr workspace using Puppeteer script.
 * Downloads diagrams as local files that can then be uploaded to Confluence.
 */
public class DiagramExporter {
    private static final Logger logger = LoggerFactory.getLogger(DiagramExporter.class);
    
    private final String structurizrUrl;
    private final String username;
    private final String password;
    private final String workspaceId;
    private final Path outputDirectory;
    
    public DiagramExporter(String structurizrUrl, String username, String password, String workspaceId) {
        this.structurizrUrl = structurizrUrl;
        this.username = username;
        this.password = password;
        this.workspaceId = workspaceId;
        this.outputDirectory = Paths.get("target", "diagrams");
    }
    
    /**
     * Creates a DiagramExporter from environment variables.
     * 
     * @param workspaceId the workspace ID to export
     * @return configured DiagramExporter or null if environment variables are missing
     */
    public static DiagramExporter fromEnvironment(String workspaceId) {
        String url = System.getenv("STRUCTURIZR_URL");
        String user = System.getenv("STRUCTURIZR_USER");
        String password = System.getenv("STRUCTURIZR_PASSWORD");
        
        if (url == null || user == null || password == null) {
            logger.warn("STRUCTURIZR_URL, STRUCTURIZR_USER, or STRUCTURIZR_PASSWORD environment variables not set. Diagram export will be skipped.");
            return null;
        }
        
        return new DiagramExporter(url, user, password, workspaceId);
    }
    
    /**
     * Exports all diagrams from the workspace using the Puppeteer script.
     * 
     * @param workspace the workspace to analyze for diagram export
     * @return list of exported diagram files
     * @throws IOException if export fails
     */
    public List<File> exportDiagrams(Workspace workspace) throws IOException {
        logger.info("Starting diagram export using Puppeteer for workspace {}", workspaceId);
        
        // Create output directory
        Files.createDirectories(outputDirectory);
        
        // Construct workspace URL
        String workspaceUrl = structurizrUrl;
        if (!workspaceUrl.endsWith("/")) {
            workspaceUrl += "/";
        }
        workspaceUrl += "workspace/" + workspaceId;
        
        logger.info("Exporting diagrams from: {}", workspaceUrl);
        
        try {
            // Install Node.js dependencies if needed
            installNodeDependencies();
            
            // Run Puppeteer script to export diagrams
            ProcessBuilder processBuilder = new ProcessBuilder(
                "node", "export-diagrams.js",
                workspaceUrl,
                "png",  // Export as PNG format
                username,
                password
            );
            
            processBuilder.directory(new File("."));
            processBuilder.redirectErrorStream(true);
            
            Process process = processBuilder.start();
            
            // Wait for completion with timeout
            boolean finished = process.waitFor(120, TimeUnit.SECONDS);
            
            if (!finished) {
                process.destroyForcibly();
                throw new IOException("Diagram export timed out after 120 seconds");
            }
            
            int exitCode = process.exitValue();
            if (exitCode != 0) {
                throw new IOException("Diagram export failed with exit code: " + exitCode);
            }
            
            logger.info("Diagram export completed successfully");
            
            // Move exported files to output directory and return list
            return moveExportedFiles();
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Diagram export was interrupted", e);
        }
    }
    
    /**
     * Installs Node.js dependencies if package.json exists.
     */
    private void installNodeDependencies() throws IOException {
        File packageJson = new File("package.json");
        File nodeModules = new File("node_modules");
        
        if (packageJson.exists() && !nodeModules.exists()) {
            logger.info("Installing Node.js dependencies...");
            
            try {
                ProcessBuilder npm = new ProcessBuilder("npm", "install");
                npm.directory(new File("."));
                Process process = npm.start();
                
                boolean finished = process.waitFor(60, TimeUnit.SECONDS);
                if (!finished) {
                    process.destroyForcibly();
                    throw new IOException("npm install timed out");
                }
                
                if (process.exitValue() != 0) {
                    throw new IOException("npm install failed with exit code: " + process.exitValue());
                }
                
                logger.info("Node.js dependencies installed successfully");
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("npm install was interrupted", e);
            }
        }
    }
    
    /**
     * Moves exported diagram files to the target directory.
     * 
     * @return list of moved diagram files
     * @throws IOException if file operations fail
     */
    private List<File> moveExportedFiles() throws IOException {
        List<File> diagramFiles = new ArrayList<>();
        File currentDir = new File(".");
        
        // Look for PNG files created by the export script
        File[] files = currentDir.listFiles((dir, name) -> 
            name.endsWith(".png") && !name.equals("export-diagrams.js"));
        
        if (files != null) {
            for (File file : files) {
                Path sourcePath = file.toPath();
                Path targetPath = outputDirectory.resolve(file.getName());
                
                // Move file to target directory
                Files.move(sourcePath, targetPath);
                diagramFiles.add(targetPath.toFile());
                
                logger.debug("Moved diagram file: {} -> {}", sourcePath, targetPath);
            }
        }
        
        logger.info("Moved {} diagram files to {}", diagramFiles.size(), outputDirectory);
        return diagramFiles;
    }
    
    /**
     * Gets the output directory where diagrams are stored.
     * 
     * @return output directory path
     */
    public Path getOutputDirectory() {
        return outputDirectory;
    }
    
    /**
     * Cleans up exported diagram files.
     * 
     * @throws IOException if cleanup fails
     */
    public void cleanup() throws IOException {
        if (Files.exists(outputDirectory)) {
            Files.walk(outputDirectory)
                .sorted((path1, path2) -> path2.compareTo(path1)) // Delete files before directories
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        logger.warn("Failed to delete: {}", path, e);
                    }
                });
        }
    }
}