package com.mycom.backenddaengplace.pet.controller;

import com.mycom.backenddaengplace.common.dto.ApiResponse;
import com.mycom.backenddaengplace.pet.dto.request.BasePetRequest;
import com.mycom.backenddaengplace.pet.dto.response.*;
import com.mycom.backenddaengplace.pet.service.PetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/member/pets")
@RequiredArgsConstructor
@Slf4j
public class PetController {
    private final PetService petService;

    @PostMapping
    public ResponseEntity<ApiResponse<BasePetResponse>> registerPet(
            @Valid @RequestBody BasePetRequest request) {
        log.debug("반려견 등록 요청: {}", request);
        BasePetResponse response = petService.registerPet(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("반려견이 등록되었습니다.", response));
    }

    @GetMapping("/breed-type")
    public ResponseEntity<ApiResponse<List<BreedTypeResponse>>> getBreedTypes() {
        List<BreedTypeResponse> response = petService.getAllBreedTypes();
        return ResponseEntity.ok(ApiResponse.success("견종 목록 조회 성공", response));
    }

    @GetMapping("/breed-type/search")
    public ResponseEntity<ApiResponse<List<BreedTypeResponse>>> searchBreedTypes(
            @RequestParam String keyword) {
        List<BreedTypeResponse> response = petService.searchBreedTypes(keyword);
        return ResponseEntity.ok(ApiResponse.success("견종 검색 성공", response));
    }

    @GetMapping("/{petId}")
    public ResponseEntity<ApiResponse<BasePetResponse>> getPet(@PathVariable Long petId) {
        BasePetResponse response = petService.getPet(petId);
        return ResponseEntity.ok(ApiResponse.success("반려견 조회 성공", response));
    }

    @PostMapping("/{petId}")
    public ResponseEntity<ApiResponse<BasePetResponse>> revisePet(
            @Valid @RequestBody BasePetRequest request, @PathVariable Long petId
    ) {
        BasePetResponse response = petService.revisePet(request, petId);
        return ResponseEntity.ok(ApiResponse.success("반려견 수정 성공", response));
    }

    @DeleteMapping("/{petId}")
    public ResponseEntity<ApiResponse<PetDeleteResponse>> deletePet(
            @PathVariable Long petId
    ) {
        PetDeleteResponse response = petService.deletePet(petId);
        return ResponseEntity.ok(ApiResponse.success("반려견 삭제 성공", response));
    }
}
