package com.businessos.modules.search;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter @Setter
public class AskResponse {
    private String question;
    private String answer;
    /** The records the answer was grounded on */
    private List<SearchResultItem> sources;
}
