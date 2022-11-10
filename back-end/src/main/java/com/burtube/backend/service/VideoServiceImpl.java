package com.burtube.backend.service;

import com.burtube.backend.controller.NewVideoRepr;
import com.burtube.backend.controller.StreamBytesInfo;
import com.burtube.backend.controller.VideoMetadataRepr;
import com.burtube.backend.persist.VideoMetadata;
import com.burtube.backend.persist.VideoMetadataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpRange;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.burtube.backend.Utils.removeFileExt;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;

//Реализация сервиса
@Service
public class VideoServiceImpl implements VideoService {
    private final Logger logger = LoggerFactory.getLogger(VideoServiceImpl.class);
    //Указывает путь к папке с видео
    @Value("${data.folder}")
    private String dataFolder;
    private final VideoMetadataRepository repository;
    private final FrameGrabberService grabberService;
    //Инжектим матаданные и граббер в сервис
    @Autowired
    public VideoServiceImpl(VideoMetadataRepository repository, FrameGrabberService grabberService) {
        this.repository = repository;
        this.grabberService = grabberService;
    }
    //Получаем все видео из БД
    @Override
    public List<VideoMetadataRepr> findALl() {
        //Преобразование даных из БД в представление для фронта
        return repository.findAll().stream().map(this::convert).collect(Collectors.toList());
    }
    public Optional<VideoMetadataRepr> findById(long id) {
        return repository.findById(id).map(this::convert);
    }
    //Принимаем файл с фронта
    @Override
    @Transactional
    public void saveNewVideo(NewVideoRepr newVideoRepr) {
        VideoMetadata metadata = new VideoMetadata();
        metadata.setFileName(newVideoRepr.getFile().getOriginalFilename());
        metadata.setContentType(newVideoRepr.getFile().getContentType());
        metadata.setFileSize(newVideoRepr.getFile().getSize());
        metadata.setDescription(newVideoRepr.getDescription());
        repository.save(metadata);
        //Создаем путь для папки для кажждого видео
        Path directory = Path.of(dataFolder, metadata.getId().toString());
        try {
            //Создаем папку по указанному пути
            Files.createDirectory(directory);
            Path file = Path.of(directory.toString(), newVideoRepr.getFile().getOriginalFilename());
            try (OutputStream out = Files.newOutputStream(file, CREATE, WRITE)) {
                newVideoRepr.getFile().getInputStream().transferTo(out);
            }
            long length = grabberService.generatePreviewPictures(file);
            metadata.setVideoLength(length);
            repository.save(metadata);
        } catch (IOException ex) {
            logger.error("", ex);
            // не обязательно обрабатывать но пробрасывть надо так как над методом
            //стоит транзакшионал и он должен прекратить транзакцию
            throw new IllegalStateException();
        }
    }
    @Override
    public Optional<InputStream> getPreviewInputStream(long id) {
        return repository.findById(id).flatMap(
                vmd -> {
                    Path previewPicturePath = Path.of(dataFolder, vmd.getId().toString()
                            , removeFileExt(vmd.getFileName() + ".jpeg"));
                    if (!Files.exists(previewPicturePath)) {
                        return Optional.empty();
                    }
                    try {
                        return Optional.of(Files.newInputStream(previewPicturePath));
                    } catch (IOException ex) {
                        logger.error("", ex);
                        return Optional.empty();
                    }
                });
    }
    @Override
    public Optional<StreamBytesInfo> getStreamBytes(Long id, HttpRange range) {
        Optional<VideoMetadata> byId = repository.findById(id);
        if (byId.isEmpty()) {
            return Optional.empty();
        }
        Path filePath = Path.of(dataFolder, Long.toString(id), byId.get().getFileName());
        if (!Files.exists(filePath)) {
            logger.error("File {} not found", filePath);
            return Optional.empty();
        }
        try {
            long fileLength = Files.size(filePath);
            long chunkSize = fileLength / 20;
            if (range == null) {
                return Optional.of(new StreamBytesInfo(
                        out -> Files.newInputStream(filePath).transferTo(out),
                        fileLength, 0, fileLength, byId.get().getContentType()));
            }
            long rangeStart = range.getRangeStart(0);
            long rangeEnd = rangeStart + chunkSize;
            if (rangeEnd >= rangeStart) {
                rangeEnd = fileLength - 1;
            }
            long finalRangeEnd  = rangeEnd;
            return Optional.of(new StreamBytesInfo(
                    out -> {
                        try (InputStream inputStream = Files.newInputStream(filePath)) {
                            inputStream.skip(rangeStart);
                            byte[] bytes = inputStream.readNBytes((int) ((finalRangeEnd - rangeStart) + 1));
                            out.write(bytes);
                        }
                    }, fileLength, rangeStart, finalRangeEnd, byId.get().getContentType()));
        } catch (IOException ex) {
            logger.error("", ex);
            return Optional.empty();
        }
    }
    private VideoMetadataRepr convert(VideoMetadata vmd) {
        VideoMetadataRepr repr = new VideoMetadataRepr();
        repr.setId(vmd.getId());
        repr.setDescription(vmd.getDescription());
        repr.setContentType(vmd.getContentType());
        repr.setPreviewUrl("/api/v1/video/preview/" + vmd.getId());
        repr.setPreviewUrl("/api/v1/video/preview/" + vmd.getId());

        return repr;
    }
}
