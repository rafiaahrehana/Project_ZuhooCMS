package com.businessos.modules.crm.tag;

import java.util.List;

public class TagMapper {

    public static TagResponse toResponse(Tag tag) {
        return TagResponse.builder()
                .id(tag.getId())
                .name(tag.getName())
                .color(tag.getColor())
                .build();
    }

    public static List<TagResponse> toResponseList(List<Tag> tags) {
        return tags.stream().map(TagMapper::toResponse).toList();
    }
}
