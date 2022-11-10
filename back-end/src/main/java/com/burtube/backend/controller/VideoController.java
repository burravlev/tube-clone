package com.burtube.backend.controller;

import com.burtube.backend.controller.exceptions.NotFoundException;
import com.burtube.backend.service.VideoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.swing.*;
import java.io.InputStream;
import java.net.http.HttpResponse;
import java.text.DecimalFormat;
import java.util.List;

//Рест контроллер для обработки видео
@RequestMapping("/api/v1/video")
@RestController
public class VideoController {

    private Logger logger = LoggerFactory.getLogger(VideoController.class);

    private final VideoService videoService;

    @Autowired
    public VideoController(VideoService videoService) {
        this.videoService = videoService;
    }

    //Получение всех видео
    @GetMapping("/all")
    public List<VideoMetadataRepr> findAll() {
        return videoService.findALl();
    }

    //Получение одного видео
    @GetMapping("{id}")
    public VideoMetadataRepr findById(@PathVariable("id") long id) {
        return videoService.findById(id).orElseThrow(NotFoundException::new);
    }

    //Получение превьюшек для видео
    @GetMapping(value = "/preview/{id}", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<StreamingResponseBody> getPreviewPicture(@PathVariable("id") Long id) {
        InputStream inputStream =
                videoService.getPreviewInputStream(id).orElseThrow(NotFoundException::new);

        return ResponseEntity.ok(inputStream::transferTo);
    }
    /*
        Содержится в заголовке
        Первый запрос
        Range: bytes=0-1234
        Второй запрос
        Range: bytes=1234-15555
     */

    //Метод стриминга видео
    @GetMapping("/stream/{id}")
    public ResponseEntity<StreamingResponseBody> streamVideo(
            //Отправка больших по обьему файлов(стриминг)
            @RequestHeader(value = "Range", required = false) String httpRangeHeader,
            @PathVariable("id") Long id) {
        List<HttpRange> httpRanges = HttpRange.parseRanges(httpRangeHeader);
        StreamBytesInfo streamBytesInfo = videoService.getStreamBytes(id, httpRanges.get(0)).orElseThrow(NotFoundException::new);
        long bytesLength = streamBytesInfo.getRangeEnd() - streamBytesInfo.getRangeStart() + 1;
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(httpRanges.size() > 0 ? HttpStatus.PARTIAL_CONTENT : HttpStatus.OK)
                .header("Content-Type", streamBytesInfo.getContentType())
                .header("Accept-Ranges", "bytes")
                .header("Content-Length", Long.toString(bytesLength));

        if (httpRanges.size() > 0) {
            builder.header("Content-Range",
                    "bytes" + streamBytesInfo.getRangeStart() + "-" + streamBytesInfo.getRangeEnd()
                            + "/" + streamBytesInfo.getFileSize());
        }
        logger.info("Providing bytes from {} to {}. We are at {}% of overall video", streamBytesInfo.getRangeStart(), streamBytesInfo.getRangeEnd(),
                new DecimalFormat("###.##").format(100.0 * streamBytesInfo.getRangeStart() / streamBytesInfo.getFileSize()));

        return ResponseEntity.ok().build();

    }

    //Загрузка видео с клиента
    @PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> updloadVideo(NewVideoRepr newVideoRepr) {
        try {
            videoService.saveNewVideo(newVideoRepr);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    //Обработка исключение NOT FOUND
    @ExceptionHandler
    public ResponseEntity<Void> notFoundException(NotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }
}
