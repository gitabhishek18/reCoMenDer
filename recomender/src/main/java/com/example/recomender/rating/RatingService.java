package com.example.recomender.rating;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.recomender.movie.Movie;
import com.example.recomender.movie.MovieRepository;
import com.example.recomender.user.User;
import com.example.recomender.user.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RatingService {
    private final RatingRepository ratingRepository;
    private final MovieRepository movieRepository;
    private final UserRepository userRepository;

    @Transactional
    public void upsertRating(Long userId,Long movieId,double ratingVal){
        User user=userRepository.findById(userId)
        .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"user not found"));
        Movie movie=movieRepository.findById(movieId)
        .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"movie not found"));
        RatingId id = new RatingId(userId, movieId);

        Rating rating=ratingRepository.findById(id)
        .orElseGet(()->{
            Rating r=new Rating();
            r.setId(id);
            r.setMovie(movie);
            r.setUser(user);
            return r;
        });
        rating.setRating(BigDecimal.valueOf(ratingVal));
        rating.setRatedAt(System.currentTimeMillis()/1000);

        ratingRepository.save(rating);
    }
    public List<MyRatingResponse> getRatingResponsesForUser(Long userId) {
    return ratingRepository.findByUser_IdOrderByRatedAtDesc(userId)
            .stream()
            .map(rating -> new MyRatingResponse(
                rating.getMovie().getId(),
                rating.getMovie().getTitle(),
                rating.getMovie().getYear(),
                rating.getMovie().getGenres(),
                rating.getRating(),
                rating.getRatedAt()
            ))
            .toList();
}
}
