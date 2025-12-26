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
            // REMOVE OR COMMENT OUT .withCannedAcl(CannedAccessControlList.PublicRead)
            amazonS3.putObject(new PutObjectRequest(bucketName, fileName, file.getInputStream(), metadata));
            // .withCannedAcl(CannedAccessControlList.PublicRead)); // THIS LINE IS THE
            // PROBLEM

            // Construct the public URL (assuming your bucket policy allows public reads)
            String fileUrl = amazonS3.getUrl(bucketName, fileName).toString();
            logger.info("Uploaded file to S3: {}", fileUrl);
            return fileUrl;
        } catch (IOException e) {
            logger.error("Error uploading file to S3: {}", e.getMessage(), e); // Log full stack trace
            throw new IOException("Failed to upload file to S3", e);
        }
    }

    private String generateFileName(MultipartFile multipartFile) {
        return UUID.randomUUID().toString() + "_" + multipartFile.getOriginalFilename().replace(" ", "_"); // Add
                                                                                                           // replace                                                                                              // for spaces
    }
}