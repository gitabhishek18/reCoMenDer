package com.example.recomender.rating;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RatingRepository extends JpaRepository<Rating, RatingId> {
  List<Rating> findByUser_IdOrderByRatedAtDesc(Long userId);
}
