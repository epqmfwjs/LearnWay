package com.learnway.notice.service;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;

import com.learnway.global.exceptions.DataNotExeption;
import com.learnway.member.domain.Member;
import com.learnway.notice.domain.Notice;
import com.learnway.notice.domain.NoticeRepository;
import com.learnway.notice.dto.NoticeDto;

@Service
public class NoticeService {
	
	@Value("C:\\learway\\img\\notice")
	private String uploadPath;
	
	@Autowired
	NoticeRepository noticeRepository;
	
	//페이지 처리
	public Page<Notice> noticeList(Pageable pageable) {		
		return noticeRepository.findAllByOrderByCreateDateDesc(pageable);
	}
	//검색 페이지 처리
	public Page<Notice> noticeSearchList(Pageable pageable,String keyword) {		
		return noticeRepository.findByNoticeTitleContainingOrderByCreateDateDesc(pageable,keyword);
	}
	//우선 공지사항 페이지 처리
	public Page<Notice> priNoticeList(Pageable pageable) {		
		return noticeRepository.findByPriorityTrueOrderByCreateDateDesc(pageable);
	}


	
	//글쓰기
	public void write(NoticeDto dto,Optional<Member> member) {
		
		if(member.isPresent()) {
			
			Member savemember=member.get();
			Notice notice = new Notice();
			notice.setNoticeId(dto.getNoticeId());
			notice.setNoticeTitle(dto.getNoticeTitle());
			notice.setNoticeContent(dto.getNoticeContent());
			notice.setNoticeImgPath(dto.getNoticeImgPath());
			notice.setNoticeImgUname(dto.getNoticeImgUname());
			notice.setCreateDate(LocalDateTime.now());
			notice.setPriority(dto.isPriority());
			notice.setCategory(dto.getCategory());
			notice.setMemberId(member.get().getMemberId());
			
			noticeRepository.save(notice);
		}
		
	}
	
	//글수정
	public void rewrite(NoticeDto dto) {
		
		Notice notice = new Notice();
		notice.setNoticeId(dto.getNoticeId());
		notice.setNoticeTitle(dto.getNoticeTitle());
		notice.setNoticeContent(dto.getNoticeContent());
		notice.setNoticeImgPath(dto.getNoticeImgPath());
		notice.setNoticeImgUname(dto.getNoticeImgUname());
		notice.setPriority(dto.isPriority());
		
		noticeRepository.save(notice);
		
	}
	
	//글 상세페이지
	public NoticeDto findDetail(Long noticeId) throws DataNotExeption {
		
		Optional<Notice> onotice = noticeRepository.findById(noticeId);
		if(onotice.isPresent()) {
			Notice notice = onotice.get();
			NoticeDto dto = convertDto(notice);
			dto.setPreNotice(noticeRepository.findPreNotice(noticeId));
			dto.setNextNotice(noticeRepository.findNextNotice(noticeId));
			return dto;
		}else {
			throw new DataNotExeption("notice not found");
		}
	}
	
	//글 삭제
	public void delete(NoticeDto dto) {
		noticeRepository.deleteById(dto.getNoticeId());
	}

	//날짜 폴더 생성
	public String makeFolder() {
		
		String str = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
		String folderPath = str;
		
		//make folder 
		File uploadPathFolder = new File(uploadPath,folderPath);
		
		if(uploadPathFolder.exists()==false) {
			uploadPathFolder.mkdirs();
		}
		
		return folderPath;
	}
	
	
	//엔터티 -> dto
	private NoticeDto convertDto(Notice notice) {
		NoticeDto dto = new NoticeDto();
		dto.setNoticeId(notice.getNoticeId());
		dto.setCreateDate(notice.getCreateDate());
		dto.setNoticeTitle(notice.getNoticeTitle());
		dto.setNoticeContent(notice.getNoticeContent());
		dto.setNoticeImgPath(notice.getNoticeImgPath());
		dto.setNoticeImgUname(notice.getNoticeImgUname());
		
		return dto;
	}
	
	//카테고리
	public Page<Notice> noticeCategoryList(Pageable pageable, String category) {
		return noticeRepository.findByCategoryContainingOrderByCreateDateDesc(category,pageable);
	}


}
