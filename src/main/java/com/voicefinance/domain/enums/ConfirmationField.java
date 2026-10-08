package com.voicefinance.domain.enums;

public enum ConfirmationField {
    AMOUNT,
    CATEGORY,
    DESCRIPTION,
    OCCURRED_ON,
    DATE;

    public static ConfirmationField fromFieldName(String fieldName) {
        if (fieldName == null) return null;
        return switch (fieldName.trim().toLowerCase()) {
            case "amount", "valor" -> AMOUNT;
            case "category", "categoria" -> CATEGORY;
            case "description", "descrição", "descricao" -> DESCRIPTION;
            case "occurredon", "occurred_on", "data do gasto", "data", "date" -> OCCURRED_ON;
            default -> {
                try {
                    yield ConfirmationField.valueOf(fieldName.trim().toUpperCase());
                } catch (IllegalArgumentException e) {
                    yield null;
                }
            }
        };
    }
}
