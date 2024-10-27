package com.learnway.member.controller;

import com.learnway.member.domain.Member;
import com.learnway.member.dto.JoinDTO;
import com.learnway.member.dto.MemberUpdateDTO;
import com.learnway.member.dto.TargetUniDTO;
import com.learnway.member.service.CustomUserDetails;
import com.learnway.member.service.EmailService;
import com.learnway.member.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;


@Controller
@RequiredArgsConstructor
@RequestMapping("/member")
public class MemberController {

    private final MemberService memberService;
    private final EmailService emailService; // 이메일 검증 확인 로직

    // Get 요청 시 회원가입 폼 리턴
    @GetMapping("/join")
    public String join(Model model) {
        model.addAttribute("joinDTO", new JoinDTO());
        return "member/join";
    }

    // Post 요청 시 회원 가입 처리
    @PostMapping("/join")
    public String join(@Valid @ModelAttribute JoinDTO joinDTO,
                       BindingResult bindingResult,
                       Model model) throws IOException {
        // 유효성 검사 진행 후 에러 메세지 전달
        if (bindingResult.hasErrors()) {
            for (FieldError error : bindingResult.getFieldErrors()) {
                model.addAttribute(error.getField() + "Error", error.getDefaultMessage());
            }
            model.addAttribute("joinDTO", joinDTO);
            return "member/join";
        }
        // Password / ConfirmPassword 일치 여부 확인
        if (!joinDTO.getPassword().equals(joinDTO.getConfirmPassword())) {
            model.addAttribute("passwordError", "비밀번호가 일치하지 않습니다.");
            return "member/join";
        }
        memberService.joinMember(joinDTO);
        return "redirect:/member/joinSuccess"; // 회원 가입 성공 시 joinSuccess 에서 3초 후 로그인 폼으로 이동
    }

    // 이메일 인증 팝업
    @GetMapping("/emailVerification")
    public String emailVerification() {
        return "member/emailVerification";
    }
    // 회원 가입 성공 페이지
    @GetMapping("/joinSuccess")
    public String joinSuccess() {
        return "member/joinSuccess";
    }

    // 수정 폼 조회
    @GetMapping("/update")
    public String updateForm(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        MemberUpdateDTO memberUpdateDTO = memberService.getMemberInfo(userDetails.getUsername());
        // 목표대학은 3개
        while (memberUpdateDTO.getTargetUnis().size() < 3) {
            memberUpdateDTO.getTargetUnis().add(new TargetUniDTO());
        }
        model.addAttribute("memberUpdateDTO", memberUpdateDTO);
        return "member/update";
    }

    // 회원 정보 수정 처리
    @PostMapping("/update")
    public String update(@AuthenticationPrincipal CustomUserDetails userDetails,
                         @Valid @ModelAttribute MemberUpdateDTO memberUpdateDTO,
                         BindingResult bindingResult,
                         Model model) {
        if (bindingResult.hasErrors()) {
            for (FieldError error : bindingResult.getFieldErrors()) {
                model.addAttribute(error.getField() + "Error", error.getDefaultMessage());
            }
            return "member/update";
        }
        try {
            memberService.updateMemberInfo(userDetails.getUsername(), memberUpdateDTO);
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "member/update";
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return "redirect:/loginOk"; // 수정 성공 시 loginOk 페이지로 리다이렉트
    }

    // 전체 멤버 조회(어드민)
    @GetMapping("/members")
    public String showMembers(Model model, @RequestParam(value = "page", defaultValue = "0") int page, @RequestParam(value = "size", defaultValue = "5") int size) {
        Page<Member> membersPage = memberService.findAllMembers(page, size);
        model.addAttribute("members", membersPage.getContent());
        model.addAttribute("totalPages", membersPage.getTotalPages());
        model.addAttribute("currentPage", membersPage.getNumber());
        return "admin/members";
    }
}
