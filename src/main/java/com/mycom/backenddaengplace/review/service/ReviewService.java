package com.mycom.backenddaengplace.review.service;

import com.mycom.backenddaengplace.member.domain.Member;
import com.mycom.backenddaengplace.member.exception.MemberNotFoundException;
import com.mycom.backenddaengplace.member.repository.MemberRepository;
import com.mycom.backenddaengplace.place.domain.Place;
import com.mycom.backenddaengplace.place.exception.PlaceNotFoundException;
import com.mycom.backenddaengplace.place.repository.PlaceRepository;
import com.mycom.backenddaengplace.review.domain.Review;
import com.mycom.backenddaengplace.review.dto.request.ReviewRequest;
import com.mycom.backenddaengplace.review.dto.response.PopularReviewResponse;
import com.mycom.backenddaengplace.review.dto.response.ReviewResponse;
import com.mycom.backenddaengplace.review.dto.response.MemberReviewResponse;
import com.mycom.backenddaengplace.review.exception.ReviewAlreadyExistsException;
import com.mycom.backenddaengplace.review.exception.ReviewException;
import com.mycom.backenddaengplace.review.exception.ReviewNotFoundException;
import com.mycom.backenddaengplace.review.exception.ReviewNotOwnedException;
import com.mycom.backenddaengplace.review.repository.ReviewQueryRepository;
import com.mycom.backenddaengplace.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final PlaceRepository placeRepository;
    private final MemberRepository memberRepository;
    private final ReviewQueryRepository reviewQueryRepository;

    @Transactional
    public ReviewResponse createReview(Long placeId, ReviewRequest request) {
        // 회원 조회
        Member member = memberRepository.findById(request.getMemberId())
                .orElseThrow(() -> new MemberNotFoundException(request.getMemberId()));

        // 장소 조회
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new PlaceNotFoundException(placeId));

        // 리뷰 중복 검사
        if (reviewRepository.existsByMemberAndPlace(member, place)) {
            throw new ReviewAlreadyExistsException(request.getMemberId(), placeId);
        }

        // 리뷰 생성
        Review review = Review.builder()
                .member(member)
                .place(place)
                .content(request.getContent())
                .rating(request.getRating())
                .traitTag(request.getTraitTag())
                .build();

        review = reviewRepository.save(review);

        return ReviewResponse.from(review);
    }

    public List<ReviewResponse> getReviews(Long placeId) {
        if (!placeRepository.existsById(placeId)) {
            throw new PlaceNotFoundException(placeId);
        }

        List<Review> reviews = reviewRepository.findByPlaceId(placeId);
        return reviews.stream()
                .map(ReviewResponse::from)
                .collect(Collectors.toList());
    }

    public ReviewResponse getReviewDetail(Long placeId, Long reviewId) {
        // 장소 존재 여부 확인
        if (!placeRepository.existsById(placeId)) {
            throw new PlaceNotFoundException(placeId);
        }

        // 리뷰 조회
        Review review = reviewRepository.findByIdAndPlaceId(reviewId, placeId)
                .orElseThrow(() -> new ReviewNotFoundException(reviewId, placeId));

        return ReviewResponse.from(review);
    }

    public List<MemberReviewResponse> getUserReview(Long memberId) {
        if (!memberRepository.existsById(memberId)) {
            throw new MemberNotFoundException(memberId);
        }

        List<Review> reviews = reviewRepository.findByMemberId(memberId);
        return reviews.stream()
                .map(MemberReviewResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteReview(Long memberId, Long reviewId) {
        if (!memberRepository.existsById(memberId)) {
            throw new MemberNotFoundException(memberId);
        }

        Review review = reviewRepository.findByIdAndMemberId(reviewId, memberId);
        if (review == null) {
            throw new ReviewNotOwnedException(memberId, reviewId);
        }

        reviewRepository.delete(review);

    }


    public List<PopularReviewResponse> getPopularReviews() {
        List<Review> reviews = reviewQueryRepository.findPopularReviews();
        return reviews.stream()
                .map(PopularReviewResponse::from)
                .collect(Collectors.toList());
    }

    public MemberReviewResponse getReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> ReviewException.notFound(reviewId));

        return MemberReviewResponse.from(review);
    }

    @Transactional
    public void updateReview(Long memberId, Long reviewId, ReviewRequest request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException(reviewId, null));

        if (!review.getMember().getId().equals(memberId)) {
            throw new ReviewNotOwnedException(memberId, reviewId);
        }
        review.update(request.getContent(), request.getRating(), request.getTraitTag());
    }
}
