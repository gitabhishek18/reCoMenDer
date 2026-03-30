package com.example.recomender.rating;


import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MyRatingResponse {
    private Long movieId;
    private String title;
    private Short year;
    private String genres;
    private BigDecimal rating;
    private Long ratedAt;
}