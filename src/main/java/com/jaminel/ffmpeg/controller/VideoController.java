package com.jaminel.ffmpeg.controller;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/video")
    //Controller Vid input
public class VideoController {

    private static final Logger logger = LoggerFactory.getLogger(VideoController.class);

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private Job videoMergeJob;


    @GetMapping("/greetings")
    public String greetings (){
        return "Weclome";
    }
    @PostMapping("/merge")
    public ResponseEntity<String> mergeVideos(@RequestParam("videos") List<MultipartFile> videos) {
        if (videos.size() != 3) {
            logger.error("Invalid number of videos uploaded: {}", videos.size());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Please upload exactly 3 videos.");
        }

        File tempDir = null;
        try {
            // Save the uploaded files to a temporary directory
            tempDir = new File(System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString());
            if (!tempDir.exists()) {
                tempDir.mkdirs();
            }

            saveFile(videos.get(0), tempDir);
            saveFile(videos.get(1), tempDir);
            saveFile(videos.get(2), tempDir);

            // Launch the batch job to merge the videos
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("tempDir", tempDir.getAbsolutePath())
                    .toJobParameters();
            jobLauncher.run(videoMergeJob, jobParameters);

            // Return the path to the merged video
            File mergedVideo = new File(tempDir, "merged_video.mp4");
            return ResponseEntity.ok(mergedVideo.getAbsolutePath());

        } catch (IOException e) {
            logger.error("An error occurred while merging the videos.", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while merging the videos.");
        } catch (JobExecutionException e) {
            logger.error("An error occurred while executing the batch job.", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while executing the batch job.");
        } finally {
            if (tempDir != null) {
                try {
                    FileUtils.deleteDirectory(tempDir);
                } catch (IOException e) {
                    logger.error("Failed to delete temporary directory.", e);
                }
            }
        }
    }

    @PostMapping("/mergeFromDrive")
    public ResponseEntity<String> mergeVideosFromDrive(@RequestParam("urls") List<String> urls) {
        if (urls.size() != 3) {
            logger.error("Invalid number of video URLs provided: {}", urls.size());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Please provide exactly 3 video URLs.");
        }

        File tempDir = null;
        try {
            // Create a temporary directory
            tempDir = new File(System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString());
            if (!tempDir.exists()) {
                tempDir.mkdirs();
            }

            // Download videos from Google Drive URLs
            for (String url : urls) {
                downloadVideoFromGoogleDrive(url, tempDir);
            }

            // Launch the batch job to merge the videos
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("tempDir", tempDir.getAbsolutePath())
                    .toJobParameters();
            jobLauncher.run(videoMergeJob, jobParameters);

            // Return the path to the merged video
            File mergedVideo = new File(tempDir, "merged_video.mp4");
            return ResponseEntity.ok(mergedVideo.getAbsolutePath());

        } catch (IOException e) {
            logger.error("An error occurred while merging the videos from Google Drive.", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while merging the videos.");
        } catch (JobExecutionException e) {
            logger.error("An error occurred while executing the batch job.", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while executing the batch job.");
        } finally {
            if (tempDir != null) {
                try {
                    FileUtils.deleteDirectory(tempDir);
                } catch (IOException e) {
                    logger.error("Failed to delete temporary directory.", e);
                }
            }
        }
    }



    private File saveFile(MultipartFile multipartFile, File directory) throws IOException {
        File file = new File(directory, multipartFile.getOriginalFilename());
        FileUtils.copyInputStreamToFile(multipartFile.getInputStream(), file);
        return file;
    }

    private File downloadVideoFromGoogleDrive(String fileUrl, File directory) throws IOException {
        URL url = new URL(fileUrl);
        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
        httpConn.setRequestMethod("GET");
        httpConn.setDoOutput(true);

        try (InputStream inputStream = httpConn.getInputStream()) {
            String fileName = UUID.randomUUID().toString() + ".mp4";
            File downloadedFile = new File(directory, fileName);
            Files.copy(inputStream, downloadedFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return downloadedFile;
        }
    }
}
