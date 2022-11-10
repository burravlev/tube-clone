package com.burtube.backend.persist;

import javax.persistence.*;

@Entity
@Table(name = "video_metadata")
public class VideoMetadata {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    @Column(name = "file_name")
    private String fileName;
    @Column(name = "content_type")
    private String contentType;
    @Column(name = "description")
    private String description;
    @Column(name = "file_size")
    private Long fileSize;
    @Column(name = "video_length")
    private Long videoLength;
    public VideoMetadata() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public Long getVideoLength() {
        return videoLength;
    }

    public void setVideoLength(Long videoLength) {
        this.videoLength = videoLength;
    }
}
