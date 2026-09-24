package com.demo.k8s.note;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NoteRequest(
        @NotBlank(message = "内容不能为空")
        @Size(max = 200, message = "内容不能超过 200 字")
        String content,

        @Size(max = 40, message = "昵称不能超过 40 字")
        String author
) {
}
