package com.servicehub.serv.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryImageService {

    private static final long MAX_FILE_SIZE =
            5L * 1024 * 1024;

    private final Cloudinary cloudinary;

    public CloudinaryImageService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    public String uploadBookingPartPhoto(MultipartFile photo) {

        if (photo == null || photo.isEmpty()) {
            throw new IllegalArgumentException(
                    "A part photo is required.");
        }

        if (photo.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    "Image size must not exceed 5 MB.");
        }

        String contentType = photo.getContentType();

        if (contentType == null
                || !(contentType.equalsIgnoreCase("image/jpeg")
                || contentType.equalsIgnoreCase("image/png"))) {
            throw new IllegalArgumentException(
                    "Only JPEG and PNG images are supported.");
        }

        try {
            byte[] compressedPhoto = compressImage(photo);

            Map<?, ?> result = cloudinary.uploader().upload(
                    compressedPhoto,
                    ObjectUtils.asMap(
                            "folder", "servicehub/booking-parts",
                            "resource_type", "image",
                            "format", "jpg"
                    )
            );

            Object secureUrl = result.get("secure_url");

            if (secureUrl == null) {
                throw new IllegalStateException(
                        "Cloudinary did not return an image URL.");
            }

            return secureUrl.toString();

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Unable to process the part photo.", e);
        }
    }

    private byte[] compressImage(MultipartFile photo)
            throws IOException {

        try (ByteArrayOutputStream output =
                     new ByteArrayOutputStream()) {

            Thumbnails.of(photo.getInputStream())
                    .size(1200, 1200)
                    .outputFormat("jpg")
                    .outputQuality(0.80)
                    .toOutputStream(output);

            return output.toByteArray();
        }
    }
}