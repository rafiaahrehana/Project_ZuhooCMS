package com.businessos.modules.itam.assetimport;

import com.businessos.auth.role.enums.PermissionCode;
import com.businessos.auth.role.service.AuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/itam/asset-import")
@PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
public class AssetImportController {

    private final AssetImportService assetImportService;
    private final AuthorizationService authorizationService;

    @GetMapping(value = "/template", produces = "text/csv")
    public ResponseEntity<String> getTemplate() {
        authorizationService.checkPermission(PermissionCode.ASSET_IMPORT_VIEW);
        return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=asset-import-template.csv")
            .contentType(MediaType.parseMediaType("text/csv"))
            .body(assetImportService.getTemplateCsv());
    }

    @GetMapping("/template.xlsx")
    public ResponseEntity<byte[]> getTemplateXlsx() {
        authorizationService.checkPermission(PermissionCode.ASSET_IMPORT_VIEW);
        return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=asset-import-template.xlsx")
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(assetImportService.getTemplateXlsx());
    }

    @PostMapping
    public ResponseEntity<AssetImportResultResponse> importCsv(@RequestParam("file") MultipartFile file) {
        authorizationService.checkPermission(PermissionCode.ASSET_IMPORT_VIEW);
        return ResponseEntity.ok(assetImportService.importCsv(file));
    }
}
