package com.song.videostream;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class VideoStream {

    public static void main(String[] args) {
        SpringApplication.run(VideoStream.class, args);
    }

}
