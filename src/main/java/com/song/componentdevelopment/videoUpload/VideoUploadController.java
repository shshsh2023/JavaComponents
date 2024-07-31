package com.song.componentdevelopment.videoUpload;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;

import java.io.*;
import java.net.URL;
import java.util.Enumeration;
import java.util.Map;

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
    public String upload(Model model, MultipartFile file) {
        if(file.isEmpty()){
            model.addAttribute("flag", 0);
            return "videoupload";
        }

        model.addAttribute("flag", 1);

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

        return "videoupload";
    }



    @GetMapping("/download/{filename}")
    public ResponseEntity<byte[]> download(@PathVariable String filename){
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
