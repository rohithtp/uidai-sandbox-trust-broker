package com.uidai.sandbox.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalSystem implements Serializable {
    private String systemId;
    private String systemName;
    private TrustLevel trustLevel;
    private boolean active;
}
