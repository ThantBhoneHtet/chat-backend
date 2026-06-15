package tbh.chat_app.service;

import java.io.IOException;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger; // Import Logger
import org.slf4j.LoggerFactory; // Import LoggerFactory

import com.amazonaws.services.s3.AmazonS3;
// import com.amazonaws.services.s3.model.CannedAccessControlList; // Remove this import
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;

@Service
public class S3Service {

    private static final Logger logger = LoggerFactory.getLogger(S3Service.class);

    @Autowired
    private AmazonS3 amazonS3;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    public String uploadFile(MultipartFile file) throws IOException {
        String fileName = generateFileName(file);
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        metadata.setContentType(file.getContentType());

        try {
            amazonS3.putObject(new PutObjectRequest(bucketName, fileName, file.getInputStream(), metadata));

            String fileUrl = amazonS3.getUrl(bucketName, fileName).toString();
            logger.info("Uploaded file to S3: {}", fileUrl);
            return fileUrl;
        } catch (IOException e) {
            logger.error("Error uploading file to S3: {}", e.getMessage(), e); // Log full stack trace
            throw new IOException("Failed to upload file to S3", e);
        }
    }

    public void deleteFile(String fileUrl) {
        String fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1); // https://bucket-name.s3.region.amazonaws.com/<UUID_filename>.jpg
        try {
            amazonS3.deleteObject(bucketName, fileName);
            logger.info("Deleted file from S3: {}", fileUrl);
        } catch (Exception e) {
            logger.error("Error deleting file from S3: {}", e.getMessage(), e); // Log full stack trace
            throw new RuntimeException("Failed to delete file from S3", e);
        }
    }

    private String generateFileName(MultipartFile multipartFile) {
        return UUID.randomUUID().toString() + "_" + multipartFile.getOriginalFilename().replace(" ", "_");                                                                                                                                                                                              // for spaces
    }
}