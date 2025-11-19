package com.easylive.service;

import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

public interface FileService {
    String uploadImage(@NotNull MultipartFile file, @NotNull Boolean createThumbnail);

    void getResource(HttpServletResponse response, @NotEmpty String sourceName);


}
