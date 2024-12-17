package com.mycom.backenddaengplace.member.service;

import com.mycom.backenddaengplace.common.service.S3ImageService;
import com.mycom.backenddaengplace.member.domain.Member;
import com.mycom.backenddaengplace.member.dto.request.MemberRegisterRequest;
import com.mycom.backenddaengplace.member.dto.request.MemberUpdateRequest;
import com.mycom.backenddaengplace.member.dto.response.BaseMemberResponse;
import com.mycom.backenddaengplace.member.dto.response.DuplicateCheckResponse;
import com.mycom.backenddaengplace.member.exception.MemberNotFoundException;
import com.mycom.backenddaengplace.member.repository.MemberRepository;
import com.mycom.backenddaengplace.pet.exception.InvalidBirthDateException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class MemberService {

    private final MemberRepository memberRepository;
    private final S3ImageService s3ImageService;

    public BaseMemberResponse getMember(Long memberId) {
        return BaseMemberResponse.from(memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException(memberId)));
    }

    @Transactional
    public BaseMemberResponse registerMember(MemberRegisterRequest request, MultipartFile file) {
        // 카카오 이메일로 기존 회원 조회
        Member existingMember = memberRepository.findByEmail(request.getEmail())
                .orElse(null);

        String imageUrl = null;
        if (file != null && !file.isEmpty()) {
            imageUrl = s3ImageService.uploadImage(file, S3ImageService.USER_PROFILE_DIR);
        }

        if (existingMember != null) {
            // 기존 회원이 있다면 프로필 정보 업데이트
            return updateExistingMember(existingMember, request, imageUrl);
        } else {
            // 신규 회원이라면 새로 생성
            return createNewMember(request, imageUrl);
        }
    }

    private BaseMemberResponse updateExistingMember(Member existingMember, MemberRegisterRequest request, String imageUrl) {
        LocalDateTime birthDate = parseBirthDate(request.getBirthDate());

        // 기존 이미지가 있고 새 이미지가 업로드된 경우 기존 이미지 삭제
        if (imageUrl != null && existingMember.getProfileImageUrl() != null) {
            s3ImageService.deleteImage(existingMember.getProfileImageUrl());
        }


        existingMember.setName(request.getName());
        existingMember.setNickname(request.getNickname());
        existingMember.setBirthDate(birthDate);
        existingMember.setGender(request.getGender());
        existingMember.setState(request.getState());
        existingMember.setCity(request.getCity());
        if (imageUrl != null) {
            existingMember.setProfileImageUrl(imageUrl);
        }

        Member savedMember = memberRepository.save(existingMember);
        return BaseMemberResponse.from(savedMember);
    }

    private BaseMemberResponse createNewMember(MemberRegisterRequest request, String imageUrl) {
        LocalDateTime birthDate = parseBirthDate(request.getBirthDate());

        Member member = Member.builder()
                .email(request.getEmail())
                .name(request.getName())
                .nickname(request.getNickname())
                .birthDate(birthDate)
                .gender(request.getGender())
                .state(request.getState())
                .city(request.getCity())
                .profileImageUrl(imageUrl)
                .locationStatus(false)
                .build();

        Member savedMember = memberRepository.save(member);
        return BaseMemberResponse.from(savedMember);
    }

    @Transactional
    public BaseMemberResponse reviseMember(MemberUpdateRequest request, MultipartFile file, Long memberId) {
        Member updatedMember = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException(memberId));

        LocalDateTime birthDate = parseBirthDate(request.getBirthDate());

        // 새 이미지가 업로드된 경우
        if (file != null && !file.isEmpty()) {
            // 기존 이미지가 있다면 삭제
            if (updatedMember.getProfileImageUrl() != null) {
                s3ImageService.deleteImage(updatedMember.getProfileImageUrl());
            }
            // 새 이미지 업로드
            String imageUrl = s3ImageService.uploadImage(file, S3ImageService.USER_PROFILE_DIR);
            updatedMember.setProfileImageUrl(imageUrl);
        }
        // request의 profileImageUrl은 무시

        updatedMember.setNickname(request.getNickname());
        updatedMember.setGender(request.getGender());
        updatedMember.setState(request.getState());
        updatedMember.setCity(request.getCity());
        updatedMember.setBirthDate(birthDate);

        Member savedMember = memberRepository.save(updatedMember);  // 명시적으로 저장
        return BaseMemberResponse.from(savedMember);
    }

    private LocalDateTime parseBirthDate(String birthDate) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
            return LocalDate.parse(birthDate, formatter).atStartOfDay();
        } catch (DateTimeParseException e) {
            throw new InvalidBirthDateException();
        }
    }

    @Transactional
    public BaseMemberResponse logicalDeleteMember(Long memberId) {
        Member deletedMember = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException(memberId));

        deletedMember.setIsDeleted(true);
        deletedMember.setDeletedAt(LocalDateTime.now());
        memberRepository.save(deletedMember);

        return BaseMemberResponse.from(deletedMember);
    }

    @Transactional
    public DuplicateCheckResponse checkDuplicateNickname(String nickname) {
        boolean isValid = memberRepository.existsByNickname(nickname);
        return DuplicateCheckResponse.from(!isValid);
    }
}