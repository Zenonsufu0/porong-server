package com.poro.rpg.boss.party;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class PartyManager {

    public record Party(UUID leaderId, String leaderName, List<UUID> members) {
        public boolean isFull()         { return members.size() >= 3; }
        public boolean isMember(UUID u) { return members.contains(u); }
        public int     size()           { return members.size(); }
    }

    private final Map<UUID, Party> byLeader       = new ConcurrentHashMap<>();
    private final Map<UUID, UUID>  memberToLeader = new ConcurrentHashMap<>();

    /** true = 생성 성공. 이미 파티가 있으면 false. */
    public boolean createParty(UUID leaderId, String leaderName) {
        if (memberToLeader.containsKey(leaderId)) return false;
        List<UUID> members = new ArrayList<>(List.of(leaderId));
        byLeader.put(leaderId, new Party(leaderId, leaderName, Collections.unmodifiableList(members)));
        memberToLeader.put(leaderId, leaderId);
        return true;
    }

    /** true = 참가 성공. */
    public boolean joinParty(UUID leaderId, UUID memberId) {
        if (memberToLeader.containsKey(memberId)) return false;
        Party party = byLeader.get(leaderId);
        if (party == null || party.isFull()) return false;
        List<UUID> newMembers = new ArrayList<>(party.members());
        newMembers.add(memberId);
        byLeader.put(leaderId, new Party(party.leaderId(), party.leaderName(), Collections.unmodifiableList(newMembers)));
        memberToLeader.put(memberId, leaderId);
        return true;
    }

    /** 탈퇴(멤버) 또는 해산(리더). */
    public void leaveParty(UUID memberId) {
        UUID leaderId = memberToLeader.remove(memberId);
        if (leaderId == null) return;
        if (leaderId.equals(memberId)) {
            Party party = byLeader.remove(leaderId);
            if (party != null) party.members().forEach(memberToLeader::remove);
        } else {
            Party party = byLeader.get(leaderId);
            if (party != null) {
                List<UUID> newMembers = new ArrayList<>(party.members());
                newMembers.remove(memberId);
                byLeader.put(leaderId, new Party(party.leaderId(), party.leaderName(),
                        Collections.unmodifiableList(newMembers)));
            }
        }
    }

    public Optional<Party> findParty(UUID memberId) {
        UUID leaderId = memberToLeader.get(memberId);
        return leaderId == null ? Optional.empty() : Optional.ofNullable(byLeader.get(leaderId));
    }

    /** 정원 미달(합류 가능한) 파티 목록. */
    public List<Party> openParties() {
        return byLeader.values().stream().filter(p -> !p.isFull()).toList();
    }
}
