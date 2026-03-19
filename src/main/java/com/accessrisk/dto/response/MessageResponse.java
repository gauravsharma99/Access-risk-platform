package com.accessrisk.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Lightweight wrapper for endpoints that confirm an action but return no resource body.
 * Used by assignment endpoints (assign role to user, assign permission to role).
 */
@Getter
@AllArgsConstructor
public class MessageResponse {

    private String message;

    public static MessageResponse of(String message) {
        return new MessageResponse(message);
    }
}
