package com.auth.auth_service.dto.mock;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClinicalNoteDto {
    private String noteId;
    private String patientId;
    private String authorId;
    private String authorRole; // Doctor, Nurse
    private String noteType; // Progress, Assessment, Procedure
    private String content;
    private String branch;
    private String department;
    private LocalDateTime createdAt;
}

