package com.learnway.member.controller;

import com.learnway.member.domain.Member;
import com.learnway.member.dto.MemberNoteUpdateDTO;
import com.learnway.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/member")
public class MemberRestController {

    private final MemberService memberService;
    private final Path uploadPath = Paths.get("/uploads/"); // 외부 경로

    // ID 중복 체크
    @GetMapping("/check-id")
    public ResponseEntity<String> checkId(@RequestParam("username") String username) {
        if (username.length() < 4 || username.length() > 20) {
            return ResponseEntity.badRequest().body("아이디는 4자 이상, 20자 이하로 입력해주세요.");
        }

        if (memberService.isUsernameTaken(username)) {
            return ResponseEntity.ok("이미 사용 중인 ID입니다.");
        }

        return ResponseEntity.ok("사용 가능한 ID입니다.");
    }

    // 외부 경로 사용 - 현재 사용 안함
    @GetMapping("/uploads/{filename}")
    public ResponseEntity<Resource> serveFile(@PathVariable("filename") String filename) {
        try {
            Path file = uploadPath.resolve(filename);
            Resource resource = new UrlResource(file.toUri());

            if (resource.exists() || resource.isReadable()) {
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    // 멤버 이름으로 검색 (어드민) - 페이지네이션 추가
    @GetMapping("/searchMembers")
    public Page<Member> searchMembers(@RequestParam("name") String name,
                                      @RequestParam(value = "page", defaultValue = "0") int page,
                                      @RequestParam(value = "size", defaultValue = "10") int size) {
        return memberService.searchMembersByName(name, page, size);
    }

    // 전체 멤버 조회(어드민) - 페이지네이션 추가
    @GetMapping("/members")
    public Page<Member> getMembers(@RequestParam(value = "page", defaultValue = "0") int page,
                                   @RequestParam(value = "size", defaultValue = "10") int size) {
        return memberService.findAllMembers(page, size);
    }

    // 멤버 비고란 업데이트 (어드민)
    @PostMapping("/updateMemberNote")
    public ResponseEntity<?> updateMemberNote(@RequestBody MemberNoteUpdateDTO memberNoteUpdateDTO) {
        try {
            memberService.updateMemberNote(memberNoteUpdateDTO.getId(), memberNoteUpdateDTO.getNote());
            return ResponseEntity.ok(Collections.singletonMap("success", true));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("success", false));
        }
    }
}