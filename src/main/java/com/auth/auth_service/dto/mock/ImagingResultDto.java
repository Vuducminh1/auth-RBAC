package com.auth.auth_service.dto.mock;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImagingResultDto {
    private String resultId;
    private String orderId;
    private String patientId;
    private String radiologistId;
    private String modality;
    private String bodyPart;
    private String status; // Pending, Preliminary, Final
    private String findings;
    private String impression;
    private String recommendations;
    private List<String> imageUrls;
    private String branch;
    private String department;
    private LocalDateTime performedAt;
    private LocalDateTime reportedAt;
}

