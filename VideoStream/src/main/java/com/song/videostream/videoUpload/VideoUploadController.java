package com.song.videostream.videoUpload;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;

/**
 * @author song
 * @version 0.0.1
 * @date 2024/7/27 10:15
 */

@Controller
@RequestMapping("/video")
public class VideoUploadController {

    @Value("${upload.dir}")
    private String uploadDir;

    @GetMapping("")
    public String get(){
        return "videoupload";
    }

    @PostMapping("/upload")
    public ResponseEntity<String> upload(@RequestParam("file") MultipartFile file) {
        if(file.isEmpty()){
            return ResponseEntity.badRequest().body("上传失败");
        }

        File basePath = new File(uploadDir);
        if(!basePath.exists()){
            basePath.mkdir();
        }

        String filePath = uploadDir + file.getOriginalFilename();
        System.out.println(filePath);
        try(FileOutputStream fileOutputStream = new FileOutputStream(filePath)) {
            fileOutputStream.write(file.getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return ResponseEntity.ok(filePath);
    }

    /**
     * 整体请求下载文件，服务器负载大
     * @param filename
     * @return
     */
    @GetMapping("/download/{filename}")
    public ResponseEntity<byte[]> download(@PathVariable("filename") String filename){
        File basePath = new File(uploadDir);
        if(!basePath.exists() || !StringUtils.hasText(filename)){
            return null;
        }

        try (FileInputStream fileInputStream = new FileInputStream(uploadDir + filename)) {

            byte[] bytes = fileInputStream.readAllBytes();

            return ResponseEntity
                    .ok()
                    .header("Content-Disposition", "attachment;filename=test.mp4")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(bytes);

        } catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }


}
