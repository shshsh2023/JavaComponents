package com.song.videostream.videoUpload;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @author song
 * @version 0.0.1
 * @date 2024/7/31 15:36
 */
@Controller
public class IndexController {


    @RequestMapping("/")
    public String index(){
        return "index";
    }

}
