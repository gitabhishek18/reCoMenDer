package com.example.recomender.rating;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RatingRequest {

    @NotNull
    private Long movieId;

    @NotNull
    @DecimalMin("0.5")
    @DecimalMax("5.0")
    private Double rating;
}