package com.businessos.modules.crm.tag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TagResponse {
    private Long id;
    private String name;
    private String color;
}
