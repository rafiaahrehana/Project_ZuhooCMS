package com.businessos.modules.itam.assetimport;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssetImportResultResponse {
    private int totalRows;
    private int succeeded;
    private int failed;
    private List<AssetImportRowError> errors;
}
