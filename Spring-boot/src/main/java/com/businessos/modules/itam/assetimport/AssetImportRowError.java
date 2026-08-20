package com.businessos.modules.itam.assetimport;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssetImportRowError {
    private int row;
    private String reason;
}
