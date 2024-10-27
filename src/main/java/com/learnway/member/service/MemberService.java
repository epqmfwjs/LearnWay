package com.learnway.member.service;

import com.learnway.consult.domain.ConsultantRepository;
import com.learnway.member.domain.*;
import com.learnway.member.dto.JoinDTO;
import com.learnway.member.dto.MemberUpdateDTO;
import com.learnway.member.dto.TargetUniDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

// 멤버 관련 서비스 클래스
@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final ConsultantRepository consultantRepository;
    private final TargetUniRepository targetUniRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;  // 비밀번호 암호화 저장
    // private final S3ImageService s3ImageService; // S3 AWS
    private static final String UPLOAD_DIR = "/upload/";
    private static final String DEFAULT_IMAGE_PATH = "/img/member/member-default.png"; // 기본 이미지 경로

    // ID 중복 체크 (컨설턴트까지 같이 비교)
    public boolean isUsernameTaken(String username) {
        return memberRepository.findByMemberId(username).isPresent()
                || consultantRepository.findByConsultantId(username).isPresent();
    }

    // 파일 확장자 추출 유틸리티 메소드
    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return ""; // 확장자가 없는 경우
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }

    // 회원 가입
    public void joinMember(JoinDTO joinDTO) throws IOException {
        // ID 중복 체크
        if (memberRepository.findByMemberId(joinDTO.getUsername()).isPresent()) {
            throw new IllegalArgumentException("이미 존재하는 ID입니다.");
        }
        // 비밀번호 확인
        if (!joinDTO.getPassword().equals(joinDTO.getConfirmPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }


        // AWS S3 이미지 저장 후 경로 저장
/*        String imagePath;
        try {
            if (joinDTO.getImage() != null && !joinDTO.getImage().isEmpty()) {
                // 새로운 S3 업로드 로직 추가
                imagePath = s3ImageService.upload(joinDTO.getImage(), "images/member/");
            } else {
                // 이미지가 없을 경우 기본 이미지 경로 설정
                imagePath = "/img/member/member-default.png";
            }
        } catch (S3Exception e) {
            throw new IllegalStateException("이미지 저장에 실패했습니다.", e);
        }*/

        // 홈서버 배포용 이미지 저장 후 경로 저장
        MultipartFile imageFile = joinDTO.getImage();
        String imagePath; // imagePath를 String으로 선언

        if (imageFile != null && !imageFile.isEmpty()) {
            // 업로드된 이미지가 있을 경우 처리
            String originalFileName = StringUtils.cleanPath(imageFile.getOriginalFilename());
            String fileExtension = getFileExtension(originalFileName);
            String fileName = UUID.randomUUID() + "." + fileExtension;

            // 파일 저장 경로 설정
            Path filePath = Paths.get(UPLOAD_DIR + fileName);

            // 디렉토리 생성 (없을 경우)
            File dir = new File(UPLOAD_DIR);
            if (!dir.exists()) {
                dir.mkdirs();  // 경로가 존재하지 않으면 생성
            }

            // 이미지 파일 저장
            Files.copy(imageFile.getInputStream(), filePath);

            // 파일명 저장 (이미지가 있으면 업로드한 이미지 경로를 사용)
            imagePath = fileName; // 업로드된 이미지 경로 저장
        } else {
            // 이미지가 없을 경우 기본 이미지 경로 사용
            imagePath = DEFAULT_IMAGE_PATH;
        }

        Member member = Member.builder()
                .memberId(joinDTO.getUsername())             // ID
                .memberPw(bCryptPasswordEncoder.encode(joinDTO.getPassword())) // 비밀번호 : 암호화
                .memberName(joinDTO.getName())               // 이름
                .memberBirth(LocalDate.parse(joinDTO.getBirth(), DateTimeFormatter.ofPattern("yyyy-MM-dd"))) // 생년월일 : 날짜 포맷 설정
                .memberPhone(joinDTO.getPhone())             // 연락처
                .memberTelecom(MemberTelecom.valueOf(joinDTO.getTelecom())) // 통신사 : ENUM
                .memberRole(MemberRole.ROLE_USER)                           // 멤버 권한 : ENUM
                .memberEmail(joinDTO.getEmail())             // 이메일
                .memberGender(MemberGender.valueOf(joinDTO.getGender()))    // 성별 : ENUM
                .memberSchool(joinDTO.getSchool())           // 학교
                .memberGrade(joinDTO.getGrade())             // 학년
                .memberAddress(joinDTO.getAddress())         // 주소
                .memberDetailadd(joinDTO.getDetailAddress()) // 나머지 주소
                .memberImage(imagePath)                      // 이미지 경로 저장
                .build();
        memberRepository.save(member);

        if (joinDTO.getTargetUni() != null) {
            joinDTO.getTargetUni().forEach(targetUniDTO -> {
                if (targetUniDTO.getCollegeName() != null && !targetUniDTO.getCollegeName().isEmpty()) {
                    TargetUni targetUni = TargetUni.builder()
                            .uniName(targetUniDTO.getCollegeName())
                            .uniRank(targetUniDTO.getRank())
                            .member(member)
                            .build();
                    targetUniRepository.save(targetUni);
                }
            });
        }
        System.out.println("회원가입 완료!");
    }

    // 수정폼 : 멤버 정보 불러오기
    public MemberUpdateDTO getMemberInfo(String username) {
        Member member = memberRepository.findByMemberId(username)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
        // 1지망, 2지망, 3지망 대학 불러오기
        List<TargetUniDTO> targetUnis = member.getTargetUnis().stream()
                .map(targetUni -> {
                    TargetUniDTO dto = new TargetUniDTO();
                    dto.setCollegeName(targetUni.getUniName());
                    dto.setRank(targetUni.getUniRank()); // rank : 해당 부분은 뷰에서 hidden
                    return dto;
                })
                .collect(Collectors.toList());

        return MemberUpdateDTO.builder()
                .memberId(member.getMemberId())
                .memberName(member.getMemberName())
                .memberBirth(member.getMemberBirth().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .memberPhone(member.getMemberPhone())
                .memberTelecom(member.getMemberTelecom().name())
                .memberEmail(member.getMemberEmail())
                .memberGender(member.getMemberGender().name())
                .memberSchool(member.getMemberSchool())
                .memberGrade(member.getMemberGrade())
                .memberAddress(member.getMemberAddress())
                .memberDetailadd(member.getMemberDetailadd())
                .memberImage(member.getMemberImage()) // 기존 이미지 URL을 설정
                .targetUnis(targetUnis)
                .build();
    }

    // 멤버 정보 수정
    public void updateMemberInfo(String username, MemberUpdateDTO memberUpdateDTO) throws IOException {
        Member member = memberRepository.findByMemberId(username)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
        // 비밀번호 경우 입력 시에만 일치 여부 확인
        if (memberUpdateDTO.getPassword() != null && !memberUpdateDTO.getPassword().isEmpty()) {
            if (!memberUpdateDTO.getPassword().equals(memberUpdateDTO.getConfirmPassword())) {
                throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
            }
            member = member.toBuilder()
                    .memberPw(bCryptPasswordEncoder.encode(memberUpdateDTO.getPassword()))
                    .build();
        }

/*        // AWS S3 이미지 변경
        String imagePath = member.getMemberImage();
        if (memberUpdateDTO.getNewMemberImage() != null && !memberUpdateDTO.getNewMemberImage().isEmpty()) {
            try {
                String oldImagePath = imagePath; // 이전 이미지 경로 저장
                // 새로운 S3 업로드 로직
                imagePath = s3ImageService.upload(memberUpdateDTO.getNewMemberImage(), "images/member/");
                // 새로운 S3 삭제 로직 (기본 이미지 아닐 경우 삭제
                if(!DEFAULT_IMAGE_PATH.equals(oldImagePath)){
                s3ImageService.deleteImageFromS3(oldImagePath);
                }
            } catch (S3Exception e) {
                e.printStackTrace();
                throw new IllegalStateException("이미지 저장에 실패했습니다.", e);
            }
        }*/

        MultipartFile imgReal = memberUpdateDTO.getNewMemberImage(); // 신규 이미지
        String imagePath = member.getMemberImage(); // 기존 이미지 경로

        // 신규 이미지가 있는 경우 처리
        if (imgReal != null && !imgReal.isEmpty()) {
            // 기존 이미지 삭제 (기본 이미지가 아닌 경우만 삭제)
            if (imagePath != null && !imagePath.equals(DEFAULT_IMAGE_PATH)) {
                Path oldFilePath = Paths.get(UPLOAD_DIR + imagePath);
                try {
                    Files.deleteIfExists(oldFilePath);  // 기존 이미지 파일 삭제
                } catch (IOException e) {
                    // 삭제 실패 시 처리 (로그 출력 또는 예외 처리)
                    System.out.println("삭제실패");
                    e.printStackTrace();
                }
            }

            // 업로드된 이미지가 있을 경우 처리
            String originalFileName = StringUtils.cleanPath(imgReal.getOriginalFilename());
            String fileExtension = getFileExtension(originalFileName);
            String fileName = UUID.randomUUID() + "." + fileExtension;

            // 파일 저장 경로 설정
            Path filePath = Paths.get(UPLOAD_DIR + fileName);

            // 디렉토리 생성 (없을 경우)
            File dir = new File(UPLOAD_DIR);
            if (!dir.exists()) {
                dir.mkdirs();  // 경로가 존재하지 않으면 생성
            }

            // 이미지 파일 저장
            Files.copy(imgReal.getInputStream(), filePath);

            // 신규 이미지 파일 경로로 설정
            imagePath = fileName;
        }



        member = member.toBuilder()
                .memberName(memberUpdateDTO.getMemberName())
                .memberBirth(LocalDate.parse(memberUpdateDTO.getMemberBirth(), DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .memberPhone(memberUpdateDTO.getMemberPhone())
                .memberTelecom(MemberTelecom.valueOf(memberUpdateDTO.getMemberTelecom()))
                .memberEmail(memberUpdateDTO.getMemberEmail())
                .memberGender(MemberGender.valueOf(memberUpdateDTO.getMemberGender()))
                .memberSchool(memberUpdateDTO.getMemberSchool())
                .memberGrade(memberUpdateDTO.getMemberGrade())
                .memberAddress(memberUpdateDTO.getMemberAddress())
                .memberDetailadd(memberUpdateDTO.getMemberDetailadd())
                .memberImage(imagePath)
                .build();
        memberRepository.save(member);
        // 목표 대학 업데이트
        List<TargetUni> currentTargetUnis = member.getTargetUnis();
        // 목표 대학 컬럼 갯수 3개
        for (int i = 0; i < 3; i++) {
            TargetUniDTO targetUniDTO = memberUpdateDTO.getTargetUnis().get(i);
            // null 여부 확인 후 변경
            if (targetUniDTO.getCollegeName() != null && !targetUniDTO.getCollegeName().isEmpty()) {
                TargetUni targetUni = currentTargetUnis.stream()
                        .filter(t -> t.getUniRank().equals(targetUniDTO.getRank())) // 랭크 1,2,3 에 맞춰 변경
                        .findFirst()
                        .orElse(TargetUni.builder()
                                .uniRank(targetUniDTO.getRank())
                                .member(member)
                                .build());
                targetUni = targetUni.toBuilder()
                        .uniName(targetUniDTO.getCollegeName())
                        .build();
                targetUniRepository.save(targetUni);
            }
        }
        // 현재 세션의 사용자 정보를 업데이트
        CustomUserDetails updatedUserDetails = new CustomUserDetails(member);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(updatedUserDetails, null, updatedUserDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
// 해당 부분은 S3 이용 안할 경우 사용되는 이미지 관련 메서드
    // // 이미지 생성 메서드
    // private String saveImage(MultipartFile image) throws IOException {
    //     if (image == null || image.isEmpty()) {
    //         return "/img/member/member-default.png"; // 기본 이미지 경로
    //     }
    //     // 중복 문제 해결 : 현재시간을 파일 이름에 추가
    //     String filename = System.currentTimeMillis() + "_" + image.getOriginalFilename();
    //     Path imagePath = Paths.get(uploadPath, filename);
    //     Files.createDirectories(imagePath.getParent());
    //     Files.copy(image.getInputStream(), imagePath);

    //     return filename;
    // }

    // // 이미지 삭제 메서드
    // private void deleteImage(String imagePath) {
    //     if (imagePath != null && !imagePath.equals("/img/member/member-default.png")) {
    //         try {
    //             Path filePath = Paths.get(uploadPath).resolve(imagePath.replace("/uploads/", ""));
    //             Files.deleteIfExists(filePath);
    //         } catch (IOException e) {
    //             e.printStackTrace();
    //         }
    //     }
    // }

    // 어드민
    public Page<Member> findAllMembers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        return memberRepository.findAll(pageable);
    }

    public Page<Member> searchMembersByName(String name, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        return memberRepository.findByMemberNameContainingIgnoreCase(name, pageable);
    }

    public void updateMemberNote(Long id, String note) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
        member = member.toBuilder()
                .memberNote(note)
                .build();
        memberRepository.save(member);
    }
}