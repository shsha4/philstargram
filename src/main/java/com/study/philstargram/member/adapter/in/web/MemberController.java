package com.study.philstargram.member.adapter.in.web;

import com.study.philstargram.common.response.ApiResponse;
import com.study.philstargram.member.application.GetMemberUseCase;
import com.study.philstargram.member.application.SignUpMemberCommand;
import com.study.philstargram.member.application.SignUpMemberUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/members")
public class MemberController {

    private final SignUpMemberUseCase signUpMemberUseCase;
    private final GetMemberUseCase getMemberUseCase;

    public MemberController(SignUpMemberUseCase signUpMemberUseCase, GetMemberUseCase getMemberUseCase) {
        this.signUpMemberUseCase = signUpMemberUseCase;
        this.getMemberUseCase = getMemberUseCase;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MemberResponse>> signUp(@Valid @RequestBody SignUpMemberRequest request) {
        var result = signUpMemberUseCase.execute(new SignUpMemberCommand(request.email(), request.nickname(), request.bio()));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(MemberResponse.from(result)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MemberResponse>> getMember(@PathVariable Long id) {
        var result = getMemberUseCase.execute(id);
        return ResponseEntity.ok(ApiResponse.success(MemberResponse.from(result)));
    }
}
