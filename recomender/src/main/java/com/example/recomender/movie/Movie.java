package com.example.recomender.movie;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "movies")
@Getter @Setter @NoArgsConstructor
public class Movie {
  @Id
  private Long id;

  @Column(nullable = false)
  private String title;

  @Column(nullable = false)
  private String genres;

  private Short year;
}