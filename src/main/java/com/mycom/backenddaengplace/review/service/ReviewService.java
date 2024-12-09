package com.mycom.backenddaengplace.review.service;

import com.mycom.backenddaengplace.member.domain.Member;
import com.mycom.backenddaengplace.member.exception.MemberNotFoundException;
import com.mycom.backenddaengplace.member.repository.MemberRepository;
import com.mycom.backenddaengplace.place.domain.Place;
import com.mycom.backenddaengplace.place.exception.PlaceNotFoundException;
import com.mycom.backenddaengplace.place.repository.PlaceRepository;
import com.mycom.backenddaengplace.review.domain.Review;
import com.mycom.backenddaengplace.review.dto.request.ReviewRequest;
import com.mycom.backenddaengplace.review.dto.response.MemberReviewResponse;
import com.mycom.backenddaengplace.review.dto.response.PopularReviewResponse;
import com.mycom.backenddaengplace.review.dto.response.ReviewResponse;
import com.mycom.backenddaengplace.review.exception.ReviewAlreadyExistsException;
import com.mycom.backenddaengplace.review.exception.ReviewNotFoundException;
import com.mycom.backenddaengplace.review.exception.ReviewNotOwnedException;
import com.mycom.backenddaengplace.review.repository.ReviewQueryRepository;
import com.mycom.backenddaengplace.review.repository.ReviewRepository;
import com.mycom.backenddaengplace.trait.domain.TraitTag;
import com.mycom.backenddaengplace.trait.domain.TraitTagCount;
import com.mycom.backenddaengplace.trait.repository.TraitTagCountRepository;
import com.mycom.backenddaengplace.trait.repository.TraitTagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final PlaceRepository placeRepository;
    private final ReviewQueryRepository reviewQueryRepository;
    private final MemberRepository memberRepository;
    private final TraitTagRepository traitTagRepository;
    private final TraitTagCountRepository traitTagCountRepository;

    @Transactional
    public ReviewResponse createReview(Long placeId, ReviewRequest request, Long memberId) {
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new PlaceNotFoundException(placeId));

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException(memberId));

        if (reviewRepository.existsByMemberAndPlace(member, place)) {
            throw new ReviewAlreadyExistsException(member.getId(), placeId);
        }

        Review review = Review.builder()
                .member(member)
                .place(place)
                .content(request.getContent())
                .rating(request.getRating())
                .build();
        reviewRepository.save(review);

        List<TraitTag> traitTags = traitTagRepository.findByContentIn(request.getTraitTags());

        List<TraitTagCount> traitTagCounts = traitTags.stream()
                .map(traitTag -> {
                    return TraitTagCount.builder()
                            .traitTag(traitTag)
                            .review(review)
                            .build();
                })
                .toList();
        traitTagCountRepository.saveAll(traitTagCounts);

        return null;
    }

    @Transactional
    public List<ReviewResponse> getReviews(Long placeId) {
        if (!placeRepository.existsById(placeId)) {
            throw new PlaceNotFoundException(placeId);
        }
        return reviewRepository.findByPlaceId(placeId).stream()
                .map(ReviewResponse::from)
                .collect(Collectors.toList());
    }

    public ReviewResponse getReviewDetail(Long placeId, Long reviewId) {
        if (!placeRepository.existsById(placeId)) {
            throw new PlaceNotFoundException(placeId);
        }
        return ReviewResponse.from(reviewRepository.findByIdAndPlaceId(reviewId, placeId)
                .orElseThrow(() -> new ReviewNotFoundException(reviewId, placeId)));
    }

    @Transactional
    public void deleteReview(Long reviewId, Long memberId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException(reviewId, null));

        if (!review.getMember().getId().equals(memberId)) {
            throw new ReviewNotOwnedException(memberId, reviewId);
        }

        reviewRepository.delete(review);
    }

    public List<PopularReviewResponse> getPopularReviews() {
        return reviewQueryRepository.findPopularReviews().stream()
                .map(PopularReviewResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateReview(Long reviewId, ReviewRequest request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException(reviewId, null));

        if (request.getContent() != null && !request.getContent().isBlank()) {
            review.setContent(request.getContent());
        }

        if (request.getRating() != null && request.getRating() >= 0.0 && request.getRating() <= 5.0) {
            review.setRating(request.getRating());
        }

        // 성향 태그 수정
        traitTagCountRepository.deleteByReviewId(reviewId);
        if (request.getTraitTags() != null && !request.getTraitTags().isEmpty()) {
            List<TraitTag> traitTags = traitTagRepository.findByContentIn(request.getTraitTags());
            List<TraitTagCount> traitTagCounts = traitTags.stream()
                    .map(traitTag -> {
                        return TraitTagCount.builder()
                                .traitTag(traitTag)
                                .review(review)
                                .build();
                    })
                    .toList();
            traitTagCountRepository.saveAll(traitTagCounts);
        }
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
}
