package com.demo.k8s.note;

import java.util.List;

public record NoteListResponse(List<NoteResponse> notes, boolean cached, int ttlSeconds) {
}
