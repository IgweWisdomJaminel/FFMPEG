# FFMPEG Video Processing Application

This project is a Spring Boot application designed to handle video processing tasks, specifically merging video files using FFMPEG. It provides an API to upload and merge exactly three video files into a single output file.

## Features

- **Upload and Merge Videos**: Upload exactly three video files and merge them into a single output file.
- **Batch Processing**: Utilizes Spring Batch for efficient and reliable batch processing of video files.
- **File Handling**: Temporary storage of uploaded files for processing, with automatic cleanup.
- **Logging**: Comprehensive logging using SLF4J and Logback for monitoring and debugging.

## Technologies Used

- **Spring Boot**: Framework for building the application.
- **Spring Batch**: Batch processing of video files.
- **Spring Security**: Secures the application endpoints.
- **PostgreSQL**: Database for storing application data.
- **SLF4J and Logback**: Logging framework for detailed application logs.
- **Apache Commons IO**: Utilities for file handling.
- **Maven**: Build and dependency management.

## Prerequisites

- **Java 17**: Ensure Java 17 is installed and configured.
- **PostgreSQL**: Set up a PostgreSQL database.
- **Maven**: Ensure Maven is installed for building the project.

## Getting Started

1. **Clone the repository**:
   ```sh
   git clone https://github.com/yourusername/ffmpeg-video-processing.git
   cd ffmpeg-video-processing
   ```

2. **Configure the application**:
   - Update the `application.properties` file with your PostgreSQL database configuration and other necessary settings.

3. **Build the project**:
   ```sh
   mvn clean install
   ```

4. **Run the application**:
   ```sh
   mvn spring-boot:run
   ```

## API Endpoints

- **POST /api/video/merge**: Upload three video files and merge them into a single video.

### Example Request

```sh
curl -X POST "http://localhost:8080/api/video/merge" \
  -F "videos=@video1.mp4" \
  -F "videos=@video2.mp4" \
  -F "videos=@video3.mp4"
```

## Error Handling

- The application returns appropriate HTTP status codes and error messages for various error scenarios such as incorrect number of files uploaded or internal processing errors.

## Logging

- Logs are configured using Logback and can be found in the console output. Ensure `logback-spring.xml` is correctly configured for more detailed log management.

## Contribution

Feel free to fork the repository and submit pull requests. For major changes, please open an issue first to discuss what you would like to change.

## License

This project is licensed under the Apache License. See the `LICENSE` file for more details.

---
