package com.blogdock.image;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.blogdock.common.ApiException;

/** POST-05 본문 이미지, POST-07 대표 이미지, AUTH-05·BLOG-02 프로필 이미지가 같이 쓴다 (공통 정책 5). */
@Service
public class ImageService {

    public static final String URL_PREFIX = "/files/";
    public static final long MAX_BYTES = 10L * 1024 * 1024;
    private static final int THUMB_WIDTH = 400;

    private final ImageRepository imageRepository;
    private final Path root;

    public ImageService(ImageRepository imageRepository, @Value("${blogdock.upload-dir}") String uploadDir) {
        this.imageRepository = imageRepository;
        this.root = Path.of(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new IllegalStateException("업로드 폴더를 만들 수 없어요: " + root, e);
        }
    }

    public Path root() {
        return root;
    }

    @Transactional
    public Image upload(Long memberId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ApiException.invalid("file", "이미지를 골라 주세요");
        }
        if (file.getSize() > MAX_BYTES) {
            throw ApiException.invalid("file", "이미지는 한 장에 10MB 이하만 올릴 수 있어요");
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw ApiException.invalid("file", "이미지를 읽지 못했어요");
        }
        // 확장자가 아니라 파일 앞부분(매직 넘버)으로 종류를 판별한다
        ImageType type = ImageType.detect(bytes);
        if (type == null) {
            throw ApiException.invalid("file", "jpg, png, gif, webp 이미지만 올릴 수 있어요");
        }
        BufferedImage img;
        try (InputStream in = new ByteArrayInputStream(bytes)) {
            img = ImageIO.read(in);
        } catch (IOException e) {
            img = null;
        }
        if (img == null) {
            throw ApiException.invalid("file", "이미지가 손상되어 열 수 없어요");
        }

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        String dir = "%d/%02d/".formatted(today.getYear(), today.getMonthValue());
        String name = UUID.randomUUID().toString();
        String originalPath = dir + name + "." + type.extension;
        String thumbPath = dir + name + "_t." + type.thumbFormat;
        try {
            Files.createDirectories(root.resolve(dir));
            Files.write(root.resolve(originalPath), bytes);
            ImageIO.write(thumbnail(img, type), type.thumbFormat, root.resolve(thumbPath).toFile());
        } catch (IOException e) {
            throw new IllegalStateException("이미지 저장 실패", e);
        }
        return imageRepository.save(Image.of(memberId, originalPath, thumbPath, type.mimeType,
                bytes.length, img.getWidth(), img.getHeight()));
    }

    /** 내가 올린 이미지인지 확인하고 돌려준다. */
    @Transactional(readOnly = true)
    public Image findMine(String url, Long memberId) {
        String path = url.startsWith(URL_PREFIX) ? url.substring(URL_PREFIX.length()) : url;
        return imageRepository.findByOriginalPath(path)
                .filter(i -> i.getUploaderId().equals(memberId))
                .orElseThrow(() -> ApiException.invalid("thumbnailUrl", "고른 대표 이미지를 찾을 수 없어요"));
    }

    /** 우리 서버 이미지 주소면 썸네일 주소로, 아니면 그대로. */
    @Transactional(readOnly = true)
    public String thumbOf(String src) {
        if (src == null || !src.startsWith(URL_PREFIX)) {
            return src;
        }
        return imageRepository.findByOriginalPath(src.substring(URL_PREFIX.length()))
                .map(Image::thumbUrl)
                .orElse(src);
    }

    /** 본문에 들어간 내 이미지를 글에 연결한다. */
    @Transactional
    public void attachImages(Long memberId, Long postId, String safeHtml) {
        List<String> paths = new ArrayList<>();
        for (Element img : Jsoup.parseBodyFragment(safeHtml).select("img[src]")) {
            String src = img.attr("src");
            if (src.startsWith(URL_PREFIX)) {
                paths.add(src.substring(URL_PREFIX.length()));
            }
        }
        if (!paths.isEmpty()) {
            imageRepository.findByUploaderIdAndOriginalPathIn(memberId, paths).forEach(i -> i.attachTo(postId));
        }
    }

    private BufferedImage thumbnail(BufferedImage src, ImageType type) {
        int w = Math.min(THUMB_WIDTH, src.getWidth());
        int h = Math.max(1, (int) Math.round(src.getHeight() * (w / (double) src.getWidth())));
        boolean alpha = "png".equals(type.thumbFormat);
        BufferedImage out = new BufferedImage(w, h, alpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        if (!alpha) {
            g.setColor(java.awt.Color.WHITE);
            g.fillRect(0, 0, w, h);
        }
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return out;
    }

    /** 파일 앞 몇 바이트로 종류를 안다. 썸네일은 jpg 또는 투명 배경이 있을 수 있는 png로 만든다. */
    enum ImageType {
        JPEG("image/jpeg", "jpg", "jpg"),
        PNG("image/png", "png", "png"),
        GIF("image/gif", "gif", "png"),
        WEBP("image/webp", "webp", "png");

        final String mimeType;
        final String extension;
        final String thumbFormat;

        ImageType(String mimeType, String extension, String thumbFormat) {
            this.mimeType = mimeType;
            this.extension = extension;
            this.thumbFormat = thumbFormat;
        }

        static ImageType detect(byte[] b) {
            if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
                return JPEG;
            }
            if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') {
                return PNG;
            }
            if (b.length >= 6 && b[0] == 'G' && b[1] == 'I' && b[2] == 'F' && b[3] == '8') {
                return GIF;
            }
            if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                    && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
                return WEBP;
            }
            return null;
        }
    }
}
