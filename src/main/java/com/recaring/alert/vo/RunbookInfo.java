package com.recaring.alert.vo;

import com.recaring.alert.dataaccess.entity.AlertRunbook;

import java.util.List;

public record RunbookInfo(
        Long id,
        String alertName,
        String errorSignature,
        List<String> commands,
        String resolutionContext,
        int successCount
) {
    public static RunbookInfo from(AlertRunbook entity) {
        return new RunbookInfo(
                entity.getId(),
                entity.getAlertName(),
                entity.getErrorSignature(),
                entity.getCommands(),
                entity.getResolutionContext(),
                entity.getSuccessCount()
        );
    }
}
