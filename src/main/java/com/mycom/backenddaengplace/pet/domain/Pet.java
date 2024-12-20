package com.mycom.backenddaengplace.pet.domain;


import com.mycom.backenddaengplace.common.domain.BaseEntity;
import com.mycom.backenddaengplace.member.domain.Member;
import com.mycom.backenddaengplace.member.enums.Gender;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "pet")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Pet extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pet_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "breed_type_id")
    private BreedType breedType;

    private String name;
    private LocalDateTime birthDate;
    private Boolean isNeutered;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Column(name = "weight", precision = 4, nullable = false)
    private Double weight;

    @Column(name = "profile_image_url")  // 프로필 이미지 URL 필드 추가
    private String profileImageUrl;

    @Builder
    public Pet(BreedType breedType, String name, LocalDateTime birthDate,
               Boolean isNeutered, Double weight, Gender gender, String profileImageUrl,
               Member member
    ) {
        this.breedType = breedType;
        this.name = name;
        this.birthDate = birthDate;
        this.gender = gender;
        this.weight = weight;
        this.isNeutered = isNeutered;
        this.profileImageUrl = profileImageUrl;
        this.member = member;
    }
}
