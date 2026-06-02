package com.poro.rpg.boss.party;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 보스 파티 관리 (in-memory). 파티 v2 (DL-126):
 * 생성 시 대상 보스·제목·최대 인원(1~3)을 지정하고, 멤버별 준비 상태와 started 플래그를 가진다.
 */
public final class PartyManager {

    /** 파티 — 보스·제목·최대인원은 불변, 멤버·준비·started는 가변. 접근자 이름은 기존 record와 호환. */
    public static final class Party {
        private final UUID leaderId;
        private final String leaderName;
        private final String bossId;
        private final String title;
        private final int maxSize;
        private final List<UUID> members = new ArrayList<>();
        private final Set<UUID> ready = ConcurrentHashMap.newKeySet();
        private volatile boolean started = false;

        Party(UUID leaderId, String leaderName, String bossId, String title, int maxSize) {
            this.leaderId = leaderId;
            this.leaderName = leaderName;
            this.bossId = bossId;
            this.title = title;
            this.maxSize = Math.max(1, Math.min(3, maxSize));
            this.members.add(leaderId);
        }

        public UUID leaderId()        { return leaderId; }
        public String leaderName()    { return leaderName; }
        public String bossId()        { return bossId; }
        public String title()         { return title; }
        public int maxSize()          { return maxSize; }
        public List<UUID> members()   { return Collections.unmodifiableList(members); }
        public int size()             { return members.size(); }
        public boolean isFull()       { return members.size() >= maxSize; }
        public boolean isMember(UUID u) { return members.contains(u); }
        public boolean isLeader(UUID u) { return leaderId.equals(u); }
        public boolean isReady(UUID u)  { return ready.contains(u); }
        public boolean started()        { return started; }

        /** 리더 제외 전원 준비 완료(솔로면 true). 입장 가능 조건. */
        public boolean allMembersReady() {
            for (UUID m : members) {
                if (!m.equals(leaderId) && !ready.contains(m)) return false;
            }
            return true;
        }
    }

    private final Map<UUID, Party> byLeader       = new ConcurrentHashMap<>();
    private final Map<UUID, UUID>  memberToLeader = new ConcurrentHashMap<>();

    /** 파티 생성. 이미 파티가 있으면 false. bossId/title/maxSize 지정(v2). */
    public boolean createParty(UUID leaderId, String leaderName, String bossId, String title, int maxSize) {
        if (memberToLeader.containsKey(leaderId)) return false;
        Party party = new Party(leaderId, leaderName, bossId, title, maxSize);
        byLeader.put(leaderId, party);
        memberToLeader.put(leaderId, leaderId);
        return true;
    }

    /** 참가. 정원 초과·이미 파티중·started면 false. */
    public boolean joinParty(UUID leaderId, UUID memberId) {
        if (memberToLeader.containsKey(memberId)) return false;
        Party party = byLeader.get(leaderId);
        if (party == null || party.isFull() || party.started) return false;
        party.members.add(memberId);
        memberToLeader.put(memberId, leaderId);
        return true;
    }

    /** 탈퇴(멤버) 또는 해산(리더). 리더 해산 시 전원 정리. */
    public void leaveParty(UUID memberId) {
        UUID leaderId = memberToLeader.remove(memberId);
        if (leaderId == null) return;
        if (leaderId.equals(memberId)) {
            Party party = byLeader.remove(leaderId);
            if (party != null) party.members.forEach(memberToLeader::remove);
        } else {
            Party party = byLeader.get(leaderId);
            if (party != null) {
                party.members.remove(memberId);
                party.ready.remove(memberId);
            }
        }
    }

    /** 리더가 멤버를 추방. 리더 자신·미존재 멤버는 무시, 성공 시 true. */
    public boolean kick(UUID leaderId, UUID memberId) {
        if (leaderId.equals(memberId)) return false;
        Party party = byLeader.get(leaderId);
        if (party == null || !party.members.contains(memberId)) return false;
        party.members.remove(memberId);
        party.ready.remove(memberId);
        memberToLeader.remove(memberId);
        return true;
    }

    /** 멤버 준비 상태 토글. 현재 상태 반환(true=준비됨). 리더는 토글 불가. */
    public boolean toggleReady(UUID memberId) {
        UUID leaderId = memberToLeader.get(memberId);
        if (leaderId == null) return false;
        Party party = byLeader.get(leaderId);
        if (party == null || party.isLeader(memberId)) return false;
        if (party.ready.contains(memberId)) { party.ready.remove(memberId); return false; }
        party.ready.add(memberId);
        return true;
    }

    /** 보스룸 입장 처리 — started 마킹(파티 목록에서 숨김). */
    public void markStarted(UUID leaderId) {
        Party party = byLeader.get(leaderId);
        if (party != null) party.started = true;
    }

    public Optional<Party> findParty(UUID memberId) {
        UUID leaderId = memberToLeader.get(memberId);
        return leaderId == null ? Optional.empty() : Optional.ofNullable(byLeader.get(leaderId));
    }

    /** 합류 가능한(정원 미달·미시작) 파티 목록. */
    public List<Party> openParties() {
        return byLeader.values().stream().filter(p -> !p.isFull() && !p.started).toList();
    }
}
