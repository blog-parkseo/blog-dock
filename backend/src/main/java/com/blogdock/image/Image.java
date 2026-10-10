package com.blogdock.image;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "image")
public class Image {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uploader_id", nullable = false, updatable = false)
    private Long uploaderId;

    /** 이 이미지가 들어간 글. 올리고 아직 발행 전이면 null. */
    @Column(name = "post_id")
    private Long postId;

    /** 업로드 폴더 기준 경로. 예: 2026/10/abc.jpg */
    @Column(name = "original_path", nullable = false, updatable = false)
    private String originalPath;

    @Column(name = "thumb_path", nullable = false, updatable = false)
    private String thumbPath;

    @Column(name = "mime_type", nullable = false, updatable = false)
    private String mimeType;

    @Column(name = "size_bytes", nullable = false, updatable = false)
    private int sizeBytes;

    @Column(nullable = false, updatable = false)
    private int width;

    @Column(nullable = false, updatable = false)
    private int height;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Image() {
    }

    public static Image of(Long uploaderId, String originalPath, String thumbPath, String mimeType,
                           int sizeBytes, int width, int height) {
        Image i = new Image();
        i.uploaderId = uploaderId;
        i.originalPath = originalPath;
        i.thumbPath = thumbPath;
        i.mimeType = mimeType;
        i.sizeBytes = sizeBytes;
        i.width = width;
        i.height = height;
        i.createdAt = Instant.now();
        return i;
    }

    public void attachTo(Long postId) {
        this.postId = postId;
    }

    public String url() {
        return ImageService.URL_PREFIX + originalPath;
    }

    public String thumbUrl() {
        return ImageService.URL_PREFIX + thumbPath;
    }

    public Long getId() {
        return id;
    }

    public Long getUploaderId() {
        return uploaderId;
    }

    public String getOriginalPath() {
        return originalPath;
    }
}
