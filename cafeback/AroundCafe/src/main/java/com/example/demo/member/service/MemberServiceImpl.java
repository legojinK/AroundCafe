package com.example.demo.member.service;

import com.example.demo.member.entity.Member;
import com.example.demo.member.entity.MemberRole;
import com.example.demo.member.entity.MemberRoleType;
import com.example.demo.member.repository.MemberRepository;
import com.example.demo.common.exception.ResourceNotFoundException;
import com.example.demo.member.repository.MemberRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

    private MemberRepository memberRepository;
    private MemberRoleRepository memberRoleRepository;

    @Override
    @Transactional
    public Member findByMemNo(Long memNo) throws ResourceNotFoundException {
        return memberRepository.findByMemNo(memNo)
                .orElseThrow(() -> new ResourceNotFoundException("Member", "Mem_No", String.valueOf(memNo)));
    }

    @Override
    @Transactional
    public Member findByMemId(String memId) throws UsernameNotFoundException {
        return memberRepository.findByMemId(memId)
                .orElseThrow(() -> new UsernameNotFoundException("Member Not Found with member Id : " + memId));
    }

    @Override
    @Transactional
    public Member findByMemNick(String memNick) throws ResourceNotFoundException {
        return memberRepository.findByMemNick(memNick)
                .orElseThrow(() -> new ResourceNotFoundException("Member", "Mem_Nick", memNick));
    }

    //회원 가입, 수정 등
    @Override
    public Member save(Member member) {
        return memberRepository.save(member);
    }

    //회원 탈퇴
    @Override
    public void deleteByMemNo(Long memNo) {
        memberRepository.deleteById(memNo);
    }

    @Override
    public Boolean existsByMemId(String memId){
        return memberRepository.existsByMemId(memId);
    }

    public List<Member> findMembersByRoleType(MemberRoleType name) {
        List<MemberRole> memberRoles = memberRoleRepository.findAllByName(name);
        List<Member> members = new ArrayList<>();
        for (MemberRole memberRole : memberRoles) {
            Member member = memberRole.getMember();
            members.add(member);
        }
        return members;
    }
}
