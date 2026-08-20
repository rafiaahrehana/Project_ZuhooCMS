package com.raf.zuhoo.ui.catalog;

import com.raf.zuhoo.data.model.response.CompanyServiceResponse;

public class CatalogRow {

    private final boolean header;
    private final String headerName;
    private final CompanyServiceResponse service;

    private CatalogRow(boolean header, String headerName, CompanyServiceResponse service) {
        this.header = header;
        this.headerName = headerName;
        this.service = service;
    }

    public static CatalogRow header(String name) {
        return new CatalogRow(true, name, null);
    }

    public static CatalogRow service(CompanyServiceResponse service) {
        return new CatalogRow(false, null, service);
    }

    public boolean isHeader() {
        return header;
    }

    public String getHeaderName() {
        return headerName;
    }

    public CompanyServiceResponse getService() {
        return service;
    }
}
