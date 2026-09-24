package com.demo.k8s.note;

import java.time.Instant;

public record NoteResponse(Long id, String author, String content, Instant createdAt) {

    public static NoteResponse from(Note note) {
        return new NoteResponse(note.getId(), note.getAuthor(), note.getContent(), note.getCreatedAt());
    }
}
