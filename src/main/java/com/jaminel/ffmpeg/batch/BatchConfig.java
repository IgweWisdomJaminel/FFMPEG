package com.jaminel.ffmpeg.batch;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableBatchProcessing
public class BatchConfig {

    private static final Logger logger = LoggerFactory.getLogger(BatchConfig.class);

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Bean
    public Job videoMergeJob() {
        Step step = new StepBuilder("mergeVideosStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    String tempDir = (String) chunkContext.getStepContext().getJobParameters().get("tempDir");
                    mergeVideos(tempDir);
                    return RepeatStatus.FINISHED;
                }, transactionManager).build();

        return new JobBuilder("videoMergeJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(step)
                .listener(new JobExecutionListener() {
                    @Override
                    public void beforeJob(JobExecution jobExecution) {
                        logger.info("Starting video merge job...");
                    }

                    @Override
                    public void afterJob(JobExecution jobExecution) {
                        logger.info("Video merge job completed.");
                    }
                })
                .build();

        //make more

    }

    private void mergeVideos(String tempDir) {
        try {
            // Directory containing video files
            Path videoDir = Paths.get(tempDir);
            List<Path> videoFiles = Files.list(videoDir)
                    .filter(Files::isRegularFile)
                    .filter(file -> file.toString().endsWith(".mp4"))
                    .collect(Collectors.toList());

            // Create a text file listing all video files
            Path fileList = videoDir.resolve("filelist.txt");
            try (BufferedWriter writer = Files.newBufferedWriter(fileList)) {
                for (Path videoFile : videoFiles) {
                    writer.write("file '" + videoFile.toString().replace("\\", "/") + "'");
                    writer.newLine();
                }
            }

            // Path for the merged video
            Path mergedVideo = videoDir.resolve("merged_video.mp4");

            // Command to merge videos using FFmpeg
            String command = String.format("ffmpeg -f concat -safe 0 -i %s -c copy %s",
                    fileList.toString().replace("\\", "/"), mergedVideo.toString().replace("\\", "/"));


            Process process = Runtime.getRuntime().exec(command);
            process.waitFor();

            if (process.exitValue() == 0) {
                logger.info("Merged video saved as: {}", mergedVideo.getFileName());
            } else {
                try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String errorLine;
                    while ((errorLine = errorReader.readLine()) != null) {
                        logger.error(errorLine);
                    }
                }
                throw new RuntimeException("FFmpeg failed to merge videos");
            }

        } catch (Exception e) {
            logger.error("An error occurred while merging videos", e);
        }
    }
}