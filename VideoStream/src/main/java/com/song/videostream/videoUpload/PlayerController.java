package com.song.videostream.videoUpload;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.File;
import java.io.FileInputStream;
import java.io.RandomAccessFile;
import java.util.List;

/**
 * @author song
 * @version 0.0.1
 * @date 2024/7/27 15:27
 */
@Controller
@RequestMapping("/player")
public class PlayerController {

    @Value("${upload.dir}")
    private String basePath;

    //固定读取大小为5MB
    private static final int READ_SIZE = 5*1024*1024;

    @GetMapping("")
    public String get() {
        return "player";
    }


    @GetMapping("/play/{filename}")
    public ResponseEntity<byte[]> play(@PathVariable("filename") String filename) {
        File file = new File(basePath + filename);

        if (!file.exists()) {
            return null;
        }
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            byte[] bytes = fileInputStream.readAllBytes();
            return ResponseEntity
                    .ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(bytes);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @GetMapping("/stream/{filename}")
    public ResponseEntity<byte[]> streamVideo(@RequestHeader HttpHeaders headers, @PathVariable("filename") String filename) {
        if (!StringUtils.hasText(filename)) return null;

        File file = new File(basePath + filename);
        if (!file.exists()) return null;

        //文件长度
        long length = file.length();

        List<HttpRange> ranges = headers.getRange();
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(file.getPath(), "r")) {

            if (ranges.isEmpty()) {
                byte[] bytes = new byte[READ_SIZE];

                int read_length = (int) Math.min(READ_SIZE, length);
                //固定每次读取文件大小
                randomAccessFile.read(bytes, 0, read_length);

                return ResponseEntity.status(206)
                        .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(read_length))
                        .header(HttpHeaders.CONTENT_TYPE, "video/mp4")
                        .header(HttpHeaders.CONTENT_RANGE, "bytes " + 0 + "-" + (read_length - 1) + "/" + length)
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .body(bytes);
            }

            HttpRange range = ranges.get(0);
            long start = range.getRangeStart(length);
            long end = Math.min((start + READ_SIZE), length);

            randomAccessFile.seek(start);

            byte[] partialVideo = new byte[(int) (end-start)];

            randomAccessFile.read(partialVideo, 0, (int) (end-start));

            return ResponseEntity.status(206)
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(end - start))
                    .header(HttpHeaders.CONTENT_TYPE, "video/mp4")
                    .header(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + (end-1) + "/" + length)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(partialVideo);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

}
