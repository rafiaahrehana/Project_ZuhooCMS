package com.businessos.shared.storage;

import com.businessos.auth.user.User;
import com.businessos.security.SecurityUtil;
import com.businessos.shared.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LocalFileStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
        ".jpg", ".jpeg", ".png", ".gif", ".webp", ".svg",
        ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".csv", ".txt"
    );

    private static final Set<String> IMAGE_EXTENSIONS = Set.of(
        ".jpg", ".jpeg", ".png", ".gif", ".webp"
    );

    // Extension -> the content-type prefixes/values the browser is expected to report for it.
    // Defense in depth: a mismatch means the extension was likely tampered with.
    private static final Map<String, Set<String>> EXPECTED_CONTENT_TYPES = Map.ofEntries(
        Map.entry(".jpg", Set.of("image/jpeg")),
        Map.entry(".jpeg", Set.of("image/jpeg")),
        Map.entry(".png", Set.of("image/png")),
        Map.entry(".gif", Set.of("image/gif")),
        Map.entry(".webp", Set.of("image/webp")),
        Map.entry(".svg", Set.of("image/svg+xml")),
        Map.entry(".pdf", Set.of("application/pdf")),
        Map.entry(".doc", Set.of("application/msword")),
        Map.entry(".docx", Set.of("application/vnd.openxmlformats-officedocument.wordprocessingml.document")),
        Map.entry(".xls", Set.of("application/vnd.ms-excel")),
        Map.entry(".xlsx", Set.of("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")),
        Map.entry(".csv", Set.of("text/csv", "text/plain", "application/vnd.ms-excel", "application/csv", "application/x-csv")),
        Map.entry(".txt", Set.of("text/plain"))
    );

    private static final long MAX_AVATAR_SIZE_BYTES = 5L * 1024 * 1024; // 5MB
    private static final long MAX_DOCUMENT_SIZE_BYTES = 20L * 1024 * 1024; // 20MB

    private final SecurityUtil securityUtil;

    // Fallback if not configured in application.yml
    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    /** General-purpose upload: documents, images, spreadsheets - see ALLOWED_EXTENSIONS. */
    public String storeFile(MultipartFile file) {
        validateFile(file, ALLOWED_EXTENSIONS, MAX_DOCUMENT_SIZE_BYTES);
        return persist(file);
    }

    /**
     * Profile-picture upload: image files only, capped well below the general
     * multipart limit, and the bytes are decoded with ImageIO to confirm they're
     * a real image - a renamed non-image file (e.g. a script saved as .jpg)
     * won't pass even if the extension and content-type header look right.
     */
    public String storeAvatar(MultipartFile file) {
        validateFile(file, IMAGE_EXTENSIONS, MAX_AVATAR_SIZE_BYTES);
        validateIsRealImage(file);
        return persist(file);
    }

    private String persist(MultipartFile file) {
        try {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);

            String originalFilename = StringUtils.cleanPath(
                    file.getOriginalFilename() != null ? file.getOriginalFilename() : "unnamed");
            String extension = "";
            if (originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            User user = securityUtil.getCurrentUser();
            String prefix = "guest";
            String uid = UUID.randomUUID().toString().substring(0, 8);

            if (user != null) {
                String cleanName = (user.getFirstName() + "_" + user.getLastName())
                        .replaceAll("[^a-zA-Z0-9.-]", "_").toLowerCase();
                prefix = cleanName;
                uid = user.getId() + "_" + uid;
            }

            String fileName = prefix + "_" + uid + extension;

            Path targetLocation = uploadPath.resolve(fileName).normalize();
            if (!targetLocation.startsWith(uploadPath)) {
                throw new BadRequestException("Invalid file name detected");
            }
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/uploads/")
                    .path(fileName)
                    .toUriString();

        } catch (BadRequestException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file. Please try again!", ex);
        }
    }

    /**
     * Validates extension against the given whitelist, cross-checks the browser-reported
     * content-type against what that extension should be, and (if maxSizeBytes is given)
     * enforces a tighter-than-global size cap.
     */
    private void validateFile(MultipartFile file, Set<String> allowedExtensions, Long maxSizeBytes) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File must not be empty");
        }
        if (maxSizeBytes != null && file.getSize() > maxSizeBytes) {
            throw new BadRequestException(
                "File is too large. Maximum size is " + (maxSizeBytes / (1024 * 1024)) + "MB");
        }
        String originalFilename = StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "");
        if (!originalFilename.contains(".")) {
            throw new BadRequestException("File must have a valid extension");
        }
        String extension = originalFilename
                .substring(originalFilename.lastIndexOf(".")).toLowerCase();
        if (!allowedExtensions.contains(extension)) {
            throw new BadRequestException(
                "File type '" + extension + "' is not allowed. "
                + "Allowed types: " + allowedExtensions);
        }
        String contentType = file.getContentType();
        Set<String> expected = EXPECTED_CONTENT_TYPES.get(extension);
        if (contentType != null && expected != null && !expected.contains(contentType.toLowerCase())) {
            throw new BadRequestException(
                "File content does not match its '" + extension + "' extension");
        }
    }

    /** Confirms the uploaded bytes actually decode as an image (not just correctly named). */
    private void validateIsRealImage(MultipartFile file) {
        try {
            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image == null) {
                throw new BadRequestException("File is not a valid image");
            }
        } catch (IOException ex) {
            throw new BadRequestException("File is not a valid image");
        }
    }
}
