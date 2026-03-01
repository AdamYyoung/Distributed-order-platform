package org.inventoryservice.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderRequest {
    @NotNull(message = "productId cannot be empty!")
    private Long productId;
    @Min(value = 1, message= "at least deduct 1!")
    private Integer count;
    private String orderToken;
}
