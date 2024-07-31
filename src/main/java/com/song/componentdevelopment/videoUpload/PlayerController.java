package com.song.componentdevelopment.videoUpload;

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
import java.util.HashSet;
import java.util.LinkedList;
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

    @GetMapping("")
    public String get() {
        return "player";
    }


    @GetMapping("/play/{filename}")
    public ResponseEntity<byte[]> play(@PathVariable String filename) {
        File file = new File(basePath + filename);

        if (!file.exists()) {
            return null;
        }

        try (FileInputStream fileInputStream = new FileInputStream(file)) {

            byte[] bytes = fileInputStream.readAllBytes();


            return ResponseEntity
                    .ok()
//                    .header("Content-Disposition", "attachment;filename=test.mp4")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(bytes);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @GetMapping("/stream/{filename}")
    public ResponseEntity<byte[]> streamVideo(@RequestHeader HttpHeaders headers, @PathVariable String filename) {
        if (!StringUtils.hasText(filename)) return null;

        File file = new File(basePath + filename);
        if (!file.exists()) return null;

        long length = file.length();
        System.out.println(length);
        List<HttpRange> ranges = headers.getRange();
//        System.out.println("range s:" + ranges.get(0).getRangeStart(length));
//        System.out.println("range e:" + ranges.get(0).getRangeEnd(length));
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(file.getPath(), "r")) {

            int size_read = 5 * 1024 * 1024;   // 20M
            byte[] bytes = new byte[size_read];

//            if (ranges.isEmpty()) {
//                //固定每次读取文件大小
//                randomAccessFile.read(bytes, 0, size_read);
//
//                return ResponseEntity.status(206)
//                        .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(20 * 1024 * 1024))
//                        .header(HttpHeaders.CONTENT_TYPE, "video/mp4")
//                        .header(HttpHeaders.CONTENT_RANGE, "bytes " + 0 + "-" + (size_read - 1) + "/" + length)
////                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
//                        .body(bytes);
//            }

            HttpRange range = ranges.get(0);
            long start = range.getRangeStart(length);
            long end = Math.min((start + size_read), length);
//            long end = range.getRangeEnd(length);

            randomAccessFile.seek(start);

            byte[] partialVideo = new byte[(int) (end-start)];

            randomAccessFile.read(partialVideo, 0, (int) (end-start));
            return ResponseEntity.status(206)
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(end - start))
                    .header(HttpHeaders.CONTENT_TYPE, "video/mp4")
                    .header(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + (end-1) + "/" + length)
//                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(partialVideo);

        } catch (Exception e) {
            e.printStackTrace();
        }


        return null;
    }

}
