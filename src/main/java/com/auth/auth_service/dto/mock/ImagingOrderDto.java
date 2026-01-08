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
public class ImagingOrderDto {
    private String orderId;
    private String patientId;
    private String orderedBy;
    private String modality; // X-Ray, CT, MRI, Ultrasound
    private String bodyPart;
    private String status; // Ordered, Scheduled, Completed, Cancelled
    private String priority; // Routine, Urgent, STAT
    private String clinicalIndication;
    private String branch;
    private String department;
    private LocalDateTime orderedAt;
    private LocalDateTime scheduledAt;
}

