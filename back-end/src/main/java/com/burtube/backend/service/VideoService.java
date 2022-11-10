package com.burtube.backend.service;

import com.burtube.backend.controller.NewVideoRepr;
import com.burtube.backend.controller.StreamBytesInfo;
import com.burtube.backend.controller.VideoMetadataRepr;
import org.springframework.http.HttpRange;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;


public interface VideoService {
    List<VideoMetadataRepr> findALl();
    Optional<VideoMetadataRepr> findById(long id);
    void saveNewVideo(NewVideoRepr newVideoRepr);
    Optional<InputStream> getPreviewInputStream(long id);
    Optional<StreamBytesInfo> getStreamBytes(Long id, HttpRange range);
}
