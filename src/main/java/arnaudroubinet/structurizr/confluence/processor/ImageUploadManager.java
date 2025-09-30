package arnaudroubinet.structurizr.confluence.processor;

import arnaudroubinet.structurizr.confluence.client.ConfluenceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles downloading external images and uploading them as Confluence attachments.
 */
public class ImageUploadManager {
    private static final Logger logger = LoggerFactory.getLogger(ImageUploadManager.class);
    
    private final ConfluenceClient confluenceClient;
    private final Map<String, MediaUploadResult> uploadedImages = new HashMap<>(); // key -> result
    
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
            MediaUploadResult cached = uploadedImages.get(imageUrl);
            logger.debug("Image already uploaded: {} -> {}", imageUrl, cached.filename());
            return cached.filename();
        }

        try {
            // Extract filename from URL
            String filename = extractFilenameFromUrl(imageUrl);

            // Download the image
            byte[] imageContent = confluenceClient.downloadImage(imageUrl);

            // Determine MIME type based on file extension
            String mimeType = getMimeTypeFromFilename(filename);

            // Try detailed upload first to get media identifiers; fallback to legacy upload if unavailable
            MediaUploadResult result;
            try {
                ConfluenceClient.AttachmentDetails details = confluenceClient.uploadAttachmentDetailed(pageId, filename, imageContent, mimeType);
                if (details != null) {
                    result = new MediaUploadResult(details.filename(), details.fileId(), details.collectionName());
                    logger.info("Successfully downloaded and uploaded image (detailed): {} -> {} (attachment ID: {}, fileId: {}, collection: {})",
                        imageUrl, details.filename(), details.attachmentId(), details.fileId(), details.collectionName());
                } else {
                    // Null details from mock/unavailable method -> fallback
                    String attachmentId = confluenceClient.uploadAttachment(pageId, filename, imageContent, mimeType);
                    result = new MediaUploadResult(filename, null, null);
                    logger.info("Successfully downloaded and uploaded image (fallback): {} -> {} (attachment ID: {} - no media IDs)",
                        imageUrl, filename, attachmentId);
                }
            } catch (Exception detailedEx) {
                logger.debug("uploadAttachmentDetailed failed, falling back to uploadAttachment: {}", detailedEx.toString());
                String attachmentId = confluenceClient.uploadAttachment(pageId, filename, imageContent, mimeType);
                result = new MediaUploadResult(filename, null, null);
                logger.info("Successfully downloaded and uploaded image (fallback): {} -> {} (attachment ID: {} - no media IDs)",
                    imageUrl, filename, attachmentId);
            }

            // Store mapping for future reference
            uploadedImages.put(imageUrl, result);

            return result.filename();

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
            java.net.URI uri = java.net.URI.create(imageUrl);
            String path = uri.getPath();
            
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
     * Uploads a local file as an attachment to the specified page.
     * 
     * @param localFile the local file to upload
     * @param pageId the Confluence page ID to attach the file to
     * @return the attachment filename to reference in ADF
     * @throws IOException if upload fails
     */
    public String uploadLocalFile(File localFile, String pageId) throws IOException {
        String filename = localFile.getName();

        // Check if we've already uploaded this file
        String cacheKey = "local:" + localFile.getAbsolutePath();
        if (uploadedImages.containsKey(cacheKey)) {
            MediaUploadResult cached = uploadedImages.get(cacheKey);
            logger.debug("Local file already uploaded: {} -> {}", filename, cached.filename());
            return cached.filename();
        }

        try {
            // Read the file content
            byte[] fileContent = Files.readAllBytes(localFile.toPath());

            // Determine MIME type based on file extension
            String mimeType = getMimeTypeFromFilename(filename);

            // Try detailed upload for media identifiers first; fallback to legacy upload
            MediaUploadResult result;
            try {
                ConfluenceClient.AttachmentDetails details = confluenceClient.uploadAttachmentDetailed(pageId, filename, fileContent, mimeType);
                if (details != null) {
                    result = new MediaUploadResult(details.filename(), details.fileId(), details.collectionName());
                    logger.info("Successfully uploaded local file (detailed): {} to page {} (fileId: {}, collection: {})",
                        filename, pageId, details.fileId(), details.collectionName());
                } else {
                    String attachmentId = confluenceClient.uploadAttachment(pageId, filename, fileContent, mimeType);
                    result = new MediaUploadResult(filename, null, null);
                    logger.info("Successfully uploaded local file (fallback): {} to page {} (attachment ID: {} - no media IDs)",
                        filename, pageId, attachmentId);
                }
            } catch (Exception detailedEx) {
                logger.debug("uploadAttachmentDetailed failed for local file, falling back: {}", detailedEx.toString());
                String attachmentId = confluenceClient.uploadAttachment(pageId, filename, fileContent, mimeType);
                result = new MediaUploadResult(filename, null, null);
                logger.info("Successfully uploaded local file (fallback): {} to page {} (attachment ID: {} - no media IDs)",
                    filename, pageId, attachmentId);
            }

            // Cache the result
            uploadedImages.put(cacheKey, result);
            return result.filename();

        } catch (IOException e) {
            logger.error("Failed to upload local file: {} to page {}", filename, pageId, e);
            throw e;
        }
    }
    
    /**
     * Clears the cache of uploaded images. Use this when starting a new export.
     */
    public void clearCache() {
        uploadedImages.clear();
        logger.debug("Cleared image upload cache");
    }

    /**
     * Returns uploaded media info for a previously uploaded key (URL or local:file path).
     */
    public MediaUploadResult getMediaInfo(String key) {
        return uploadedImages.get(key);
    }

    /**
     * Simple carrier for media identifiers required by ADF media nodes.
     * filename is still provided for fallback when media ids are missing.
     */
    public static record MediaUploadResult(String filename, String fileId, String collectionName) {}
}