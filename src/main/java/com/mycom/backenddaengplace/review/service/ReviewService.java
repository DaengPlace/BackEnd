package com.mycom.backenddaengplace.review.service;

import com.mycom.backenddaengplace.common.service.S3ImageService;
import com.mycom.backenddaengplace.member.domain.Member;
import com.mycom.backenddaengplace.member.exception.MemberNotFoundException;
import com.mycom.backenddaengplace.member.repository.MemberRepository;
import com.mycom.backenddaengplace.place.domain.Place;
import com.mycom.backenddaengplace.place.exception.PlaceNotFoundException;
import com.mycom.backenddaengplace.place.repository.PlaceRepository;
import com.mycom.backenddaengplace.review.domain.MediaFile;
import com.mycom.backenddaengplace.review.domain.Review;
import com.mycom.backenddaengplace.review.dto.request.ReviewRequest;
import com.mycom.backenddaengplace.review.dto.response.MemberReviewResponse;
import com.mycom.backenddaengplace.review.dto.response.PopularReviewResponse;
import com.mycom.backenddaengplace.review.dto.response.ReviewResponse;
import com.mycom.backenddaengplace.review.exception.ReviewAlreadyExistsException;
import com.mycom.backenddaengplace.review.exception.ReviewNotFoundException;
import com.mycom.backenddaengplace.review.exception.ReviewNotOwnedException;
import com.mycom.backenddaengplace.review.repository.ReviewLikeRepository;
import com.mycom.backenddaengplace.review.repository.ReviewQueryRepository;
import com.mycom.backenddaengplace.review.repository.ReviewRepository;
import com.mycom.backenddaengplace.trait.domain.TraitTag;
import com.mycom.backenddaengplace.trait.domain.TraitTagCount;
import com.mycom.backenddaengplace.trait.repository.TraitTagCountRepository;
import com.mycom.backenddaengplace.trait.repository.TraitTagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final PlaceRepository placeRepository;
    private final ReviewQueryRepository reviewQueryRepository;
    private final MemberRepository memberRepository;
    private final TraitTagRepository traitTagRepository;
    private final TraitTagCountRepository traitTagCountRepository;
    private final ReviewLikeRepository reviewLikeRepository;
    private final S3ImageService s3ImageService;

    @Transactional
    public ReviewResponse createReview(Long placeId, ReviewRequest request, List<MultipartFile> images, Long memberId) {
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new PlaceNotFoundException(placeId));

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException(memberId));

        if (reviewRepository.existsByMemberAndPlace(member, place)) {
            throw new ReviewAlreadyExistsException(member.getId(), placeId);
        }

        // 리뷰 생성
        Review review = Review.builder()
                .member(member)
                .place(place)
                .content(request.getContent())
                .rating(request.getRating())
                .build();
        reviewRepository.save(review);

        log.info("Request trait tags: {}", request.getTraitTags());  // 요청으로 들어온 태그들

        List<TraitTag> traitTags = traitTagRepository.findByContentIn(request.getTraitTags());
        log.info("Found trait tags: {}", traitTags.stream()
                .map(TraitTag::getContent)
                .collect(Collectors.toList()));  // DB에서 찾은 태그들

        List<TraitTagCount> traitTagCounts = traitTags.stream()
                .map(traitTag -> TraitTagCount.builder()
                        .traitTag(traitTag)
                        .review(review)
                        .build())
                .toList();

        List<TraitTagCount> savedTraitTagCounts = traitTagCountRepository.saveAll(traitTagCounts);
        log.info("Saved trait tag counts: {}", savedTraitTagCounts.size());  // 저장된 태그 수

        review.setTraitTag(savedTraitTagCounts);

        // 이미지 업로드 및 MediaFile 생성
        List<MediaFile> mediaFiles = new ArrayList<>();
        if (images != null && !images.isEmpty()) {
            for (MultipartFile image : images) {
                try {
                    String imageUrl = s3ImageService.uploadImage(image, S3ImageService.REVIEW_DIR);
                    MediaFile mediaFile = MediaFile.builder()
                            .review(review)
                            .filePath(imageUrl)
                            .build();
                    mediaFiles.add(mediaFile);
                } catch (Exception e) {
                    throw new RuntimeException("이미지 업로드에 실패했습니다.", e);
                }
            }
            review.setMediaFiles(mediaFiles);
        }

        // 리뷰 응답 생성
        return ReviewResponse.from(review, 0L, false);
    }


    @Transactional
    public List<ReviewResponse> getReviews(Long placeId, Long currentMemberId) {
        if (!placeRepository.existsById(placeId)) {
            throw new PlaceNotFoundException(placeId);
        }
        return reviewRepository.findByPlaceId(placeId).stream()
                .map(review -> {
                    Long likeCount = reviewLikeRepository.countByReview(review);
                    boolean isLiked = currentMemberId != null &&
                            reviewLikeRepository.existsByReviewAndMemberId(review, currentMemberId);
                    return ReviewResponse.from(review, likeCount, isLiked);
                })
                .collect(Collectors.toList());
    }

    public ReviewResponse getReviewDetail(Long placeId, Long reviewId, Long currentMemberId) {
        if (!placeRepository.existsById(placeId)) {
            throw new PlaceNotFoundException(placeId);
        }
        Review review = reviewRepository.findByIdAndPlaceId(reviewId, placeId)
                .orElseThrow(() -> new ReviewNotFoundException(reviewId, placeId));

        Long likeCount = reviewLikeRepository.countByReview(review);
        boolean isLiked = currentMemberId != null &&
                reviewLikeRepository.existsByReviewAndMemberId(review, currentMemberId);
        return ReviewResponse.from(review, likeCount, isLiked);
    }

    @Transactional
    public void deleteReview(Long reviewId, Long memberId) {
        // placeId와 함께 리뷰 조회
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException(reviewId, null));

        if (!review.getMember().getId().equals(memberId)) {
            throw new ReviewNotOwnedException(memberId, reviewId);
        }

        // 이미지 삭제
        List<MediaFile> mediaFiles = review.getMediaFiles();
        if (mediaFiles != null && !mediaFiles.isEmpty()) {
            for (MediaFile mediaFile : mediaFiles) {
                try {
                    if (mediaFile.getFilePath() != null && !mediaFile.getFilePath().isEmpty()) {
                        s3ImageService.deleteImage(mediaFile.getFilePath());
                    }
                } catch (Exception e) {
                    log.error("Failed to delete image from S3: {}", mediaFile.getFilePath(), e);
                }
            }
        }

        reviewRepository.delete(review);
    }

    public List<PopularReviewResponse> getPopularReviews() {
        return reviewQueryRepository.findPopularReviews().stream()
                .map(PopularReviewResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateReview(Long reviewId, ReviewRequest request, Long memberId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException(reviewId, null));


        if (!review.getMember().getId().equals(memberId)) {
            throw new ReviewNotOwnedException(memberId, reviewId);
        }

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
