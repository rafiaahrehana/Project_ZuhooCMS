package com.businessos.modules.itam.assetimport;

import org.springframework.web.multipart.MultipartFile;

public interface AssetImportService {
    String getTemplateCsv();
    byte[] getTemplateXlsx();
    AssetImportResultResponse importCsv(MultipartFile file);
}
