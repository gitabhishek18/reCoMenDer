package com.example.recomender.rating;

import java.math.BigDecimal;

import com.example.recomender.movie.Movie;
import com.example.recomender.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ratings")
@Getter @Setter @NoArgsConstructor
public class Rating {
  @EmbeddedId
  private RatingId id;

  @MapsId("userId")
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @MapsId("movieId")
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "movie_id", nullable = false)
  private Movie movie;

  @Column(nullable = false, precision = 2, scale = 1)
  private BigDecimal rating;

  @Column(name = "rated_at", nullable = false)
  private Long ratedAt;
}