package com.structurizr.confluence.processor;

import com.structurizr.confluence.client.ConfluenceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles downloading external images and uploading them as Confluence attachments.
 */
public class ImageUploadManager {
    private static final Logger logger = LoggerFactory.getLogger(ImageUploadManager.class);
    
    private final ConfluenceClient confluenceClient;
    private final Map<String, String> uploadedImages = new HashMap<>(); // URL -> attachment filename
    
    public ImageUploadManager(ConfluenceClient confluenceClient) {
        this.confluenceClient = confluenceClient;
    }
    
    /**
     * Downloads an external image and uploads it as an attachment to the specified page.
     * Returns the filename to use for referencing the attachment.
     * 
     * @param imageUrl the external image URL
     * @param pageId the Confluence page ID to attach the image to
     * @return the attachment filename to reference in ADF
     * @throws IOException if download or upload fails
     */
    public String downloadAndUploadImage(String imageUrl, String pageId) throws IOException {
        // Check if we've already uploaded this image
        if (uploadedImages.containsKey(imageUrl)) {
            String filename = uploadedImages.get(imageUrl);
            logger.debug("Image already uploaded: {} -> {}", imageUrl, filename);
            return filename;
        }
        
        try {
            // Extract filename from URL
            String filename = extractFilenameFromUrl(imageUrl);
            
            // Download the image
            byte[] imageContent = confluenceClient.downloadImage(imageUrl);
            
            // Determine MIME type based on file extension
            String mimeType = getMimeTypeFromFilename(filename);
            
            // Upload as attachment
            String attachmentId = confluenceClient.uploadAttachment(pageId, filename, imageContent, mimeType);
            
            // Store mapping for future reference
            uploadedImages.put(imageUrl, filename);
            
            logger.info("Successfully downloaded and uploaded image: {} -> {} (attachment ID: {})", 
                imageUrl, filename, attachmentId);
            
            return filename;
            
        } catch (Exception e) {
            logger.error("Failed to download and upload image: {}", imageUrl, e);
            throw new IOException("Failed to process image: " + imageUrl, e);
        }
    }
    
    /**
     * Extracts a reasonable filename from an image URL.
     */
    String extractFilenameFromUrl(String imageUrl) {
        try {
            URL url = new URL(imageUrl);
            String path = url.getPath();
            
            // Get the last part of the path
            String filename = path.substring(path.lastIndexOf('/') + 1);
            
            // If no filename or no extension, generate one
            if (filename.isEmpty() || !filename.contains(".")) {
                // Generate filename based on URL hash and default extension
                String hash = Integer.toHexString(imageUrl.hashCode());
                filename = "image_" + hash + ".png";
            }
            
            // Sanitize filename - remove special characters
            filename = filename.replaceAll("[^a-zA-Z0-9._-]", "_");
            
            return filename;
            
        } catch (Exception e) {
            // Fallback to hash-based filename
            String hash = Integer.toHexString(imageUrl.hashCode());
            return "image_" + hash + ".png";
        }
    }
    
    /**
     * Determines MIME type based on file extension.
     */
    String getMimeTypeFromFilename(String filename) {
        String lowerFilename = filename.toLowerCase();
        
        if (lowerFilename.endsWith(".png")) {
            return "image/png";
        } else if (lowerFilename.endsWith(".jpg") || lowerFilename.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lowerFilename.endsWith(".gif")) {
            return "image/gif";
        } else if (lowerFilename.endsWith(".svg")) {
            return "image/svg+xml";
        } else if (lowerFilename.endsWith(".bmp")) {
            return "image/bmp";
        } else if (lowerFilename.endsWith(".webp")) {
            return "image/webp";
        } else {
            // Default to PNG for unknown types
            return "image/png";
        }
    }
    
    /**
     * Clears the cache of uploaded images. Use this when starting a new export.
     */
    public void clearCache() {
        uploadedImages.clear();
        logger.debug("Cleared image upload cache");
    }
}