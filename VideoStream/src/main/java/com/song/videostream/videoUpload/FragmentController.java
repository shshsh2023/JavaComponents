package com.song.videostream.videoUpload;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * @author song
 * @version 0.0.1
 * @date 2024/8/1 10:24
 */
@Controller
@RequestMapping("/fragment")
public class FragmentController {

    @Value("${upload.download_dir}")
    private String DOWNLOAD_DIR;

    @Value("${upload.dir}")
    private String basePath;

    private static int read_times = 0;

    //固定读取大小为5MB
    private static final int READ_SIZE = 5 * 1024 * 1024;


    @RequestMapping("/page/upload")
    public String get() {
        return "videoFragmentUpload";
    }

    @RequestMapping("/page/download")
    public String get01() {
        return "videoFragmentDownload";
    }


    /**
     * 下载视频文件，直接下载 暂停之后，浏览器会控制继续下载
     * 分片下载
     * 请求头的range参数和相应 206 部分内容响应
     *
     * @param headers
     * @param filename
     * @return
     */
    @GetMapping("/download/{filename}")
    public ResponseEntity<byte[]> videoFragmentDownload(
            @RequestHeader HttpHeaders headers,
            @PathVariable("filename") String filename,
            @RequestHeader("User-Agent") String userAgent
    ) {
        Path file_path;
        if (!StringUtils.hasText(filename) || !Files.exists(file_path = Path.of(DOWNLOAD_DIR, filename))) return null;

        List<HttpRange> range = headers.getRange();

        File file = new File(DOWNLOAD_DIR + filename);

        long file_length = file.length();

        try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw")) {
            //没有范围
            if (range.isEmpty()) {
                //返回固定长度的数据大小
                int readLength = (int) Math.min(READ_SIZE, file_length);

                byte[] bytes = new byte[readLength];
                randomAccessFile.read(bytes);

                return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .contentLength(readLength)
                        .header(HttpHeaders.CONTENT_RANGE, "bytes " + 0 + "-" + (readLength - 1) + "/" + file_length)
                        .body(bytes);
            }

            //有范围的
            HttpRange httpRange = range.get(0);
            long rangeStart = httpRange.getRangeStart(file_length);
            long rangeEnd = Math.min(httpRange.getRangeEnd(file_length), rangeStart + READ_SIZE - 1);

            long readLength = rangeEnd - rangeStart + 1;

            byte[] bytes = new byte[(int) (rangeEnd - rangeStart + 1)];
            randomAccessFile.seek(rangeStart);
            randomAccessFile.read(bytes);

            return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(readLength)
                    .header(HttpHeaders.CONTENT_RANGE, "bytes " + rangeStart + "-" + rangeEnd + "/" + file_length)
                    .body(bytes);

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 分片文件上传
     *
     * @param chunkSize   每个分片大小
     * @param chunkNumber 当前分片
     * @param md5         文件总MD5
     * @param file        当前分片文件数据
     * @param totalNumber 文件分片总数
     * @return
     * @throws IOException
     */
    @PostMapping("/upload")
    public ResponseEntity<String> videoFragmentUpload(@RequestParam("file") MultipartFile file,
                                                      @RequestParam("chunkSize") Integer chunkSize,
                                                      @RequestParam("chunkNumber") Integer chunkNumber,
                                                      @RequestParam("md5") String md5,
                                                      @RequestParam("totalNumber") Integer totalNumber,
                                                      @RequestHeader("User-Agent") String userAgent) throws Exception {
        if (file.isEmpty()) return ResponseEntity.badRequest().body("发生错误!");
        System.out.println(chunkNumber);
        //目标存放文件，使用MD5作为文件夹和名字
        String dstFile = String.format("%s\\%s\\%s.%s", basePath, md5, md5, StringUtils.getFilenameExtension(file.getOriginalFilename()));

        //上传分片信息存放位置
        String confFile = String.format("%s\\%s\\%s.conf", basePath, md5, md5);

        //第一次创建分片记录文件
        //创建目录
        File dir = new File(dstFile).getParentFile();
        //判断之前文件是否存在
        if (!dir.exists()) {
            dir.mkdirs();
            //所有分片状态设置为0
            byte[] bytes = new byte[totalNumber];
            Files.write(Path.of(confFile), bytes);
        }

        //随机分片写入文件
        try (
                RandomAccessFile randomAccessFileDstFile = new RandomAccessFile(dstFile, "rw");
                RandomAccessFile randomAccessConfFile = new RandomAccessFile(confFile, "rw");
                InputStream inputStream = file.getInputStream()
        ) {
            //定位到分片的偏移量
            randomAccessFileDstFile.seek((long) chunkNumber * chunkSize);
            //写入分片数据
            randomAccessFileDstFile.write(inputStream.readAllBytes());
            //定位到当前分片的状态位置
            randomAccessConfFile.seek(chunkNumber);
            //设置当前分片上传状态为1
            randomAccessConfFile.write(1);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("服务器异常");
        }
        return ResponseEntity.ok(dstFile);
    }


    /**
     * 获取文件分片状态，检测文件MD5合法性
     *
     * @param md5
     * @return
     * @throws Exception
     */
    @RequestMapping("/checkFile")
    public ResponseEntity<Map<String, String>> checkFile(@RequestParam("md5") String md5) throws Exception {
        System.out.println(md5);
        if (!StringUtils.hasText(md5)) return ResponseEntity.badRequest().body(Map.of("msg", "无md5"));

        String uploadPathConf = String.format("%s\\%s\\%s.conf", basePath, md5, md5);
        Path conf_path = Path.of(uploadPathConf);
        //MD5目录不存在文件从未上传过
        if (!Files.exists(conf_path.getParent())) {
            return ResponseEntity.ok(Map.of("msg", "文件未上传"));
        }

        //判断文件是否上传成功
        StringBuilder stringBuilder = new StringBuilder();

        //读取chunk的状态
        byte[] bytes = Files.readAllBytes(conf_path);

        for (byte b : bytes) {
            stringBuilder.append(String.valueOf(b));
        }

        if (stringBuilder.toString().contains("0")) {
            //文件未上传完成，返回每个分片状态，前端将未上传的分片继续上传
            return ResponseEntity.ok(Map.of("chunks", stringBuilder.toString()));
        }

        System.out.println(stringBuilder);

        //所有分片上传完成计算文件md5
        File file = new File(String.format("%s\\%s\\", basePath, md5));
        File[] files = file.listFiles();
        if (files == null) return ResponseEntity.ok().body(Map.of("msg", "上传失败"));

        //遍历出conf的其他文件
        String filePath = "";
        for (File f : files) {
            //计算文件MD5是否相等
            if (!f.getName().contains("conf")) {
                filePath = f.getAbsolutePath();
                try (
                        FileInputStream fileInputStream = new FileInputStream(f)
                ) {
                    String md5DigestAsHex = DigestUtils.md5DigestAsHex(fileInputStream);
                    if (!md5DigestAsHex.equalsIgnoreCase(md5)) {
                        return ResponseEntity.ok(Map.of("msg", "文件上传失败"));
                    }
                }
            }
        }

        return ResponseEntity.ok(Map.of("path", filePath));
    }
}
