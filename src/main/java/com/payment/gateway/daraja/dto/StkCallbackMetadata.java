package com.mpesa.integration.daraja.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Optional;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class StkCallbackMetadata {
    @JsonProperty("Item")
    private List<StkCallbackMetadataItem> items;

    public Optional<String> findString(String name) {
        if (items == null || name == null) return Optional.empty();
        return items.stream()
                .filter(i -> name.equals(i.getName()) && i.getValue() != null && !i.getValue().isNull())
                .map(i -> i.getValue().asText())
                .findFirst();
    }
}
