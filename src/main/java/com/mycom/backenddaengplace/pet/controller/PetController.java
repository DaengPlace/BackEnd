package com.mycom.backenddaengplace.pet.controller;

import com.mycom.backenddaengplace.common.dto.ApiResponse;
import com.mycom.backenddaengplace.pet.dto.request.PetRegisterRequest;
import com.mycom.backenddaengplace.pet.dto.response.BreedTypeResponse;
import com.mycom.backenddaengplace.pet.dto.response.PetRegisterResponse;
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
    public ResponseEntity<ApiResponse<PetRegisterResponse>> registerPet(
            @Valid @RequestBody PetRegisterRequest request) {
        log.debug("반려견 등록 요청: {}", request);
        PetRegisterResponse response = petService.registerPet(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("반려견이 등록되었습니다.", response));
    }

    @GetMapping("/breed-types")
    public ResponseEntity<ApiResponse<List<BreedTypeResponse>>> getBreedTypes() {
        List<BreedTypeResponse> response = petService.getAllBreedTypes();
        return ResponseEntity.ok(ApiResponse.success("견종 목록 조회 성공", response));
    }
}
