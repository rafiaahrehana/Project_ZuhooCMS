package com.businessos.modules.search;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class AskRequest {

    @NotBlank(message = "Question is required")
    @Size(max = 500)
    private String question;
}
