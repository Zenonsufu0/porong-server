package com.poro.rpg.quest.engine;

import com.poro.rpg.common.registry.master.QuestMasterRegistry;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public final class QuestChainService {
    private final QuestMasterRegistry questMasterRegistry;
    private final QuestChainBranchRegistry branchRegistry;

    public QuestChainService(
            QuestMasterRegistry questMasterRegistry,
            QuestChainBranchRegistry branchRegistry
    ) {
        this.questMasterRegistry = Objects.requireNonNull(questMasterRegistry, "questMasterRegistry");
        this.branchRegistry = Objects.requireNonNull(branchRegistry, "branchRegistry");
    }

    public List<String> resolveNextQuestIds(String completedQuestId) {
        Set<String> resolved = new LinkedHashSet<>();

        questMasterRegistry.find(completedQuestId).ifPresent(quest -> {
            String nextQuestId = normalize(quest.nextQuestId());
            if (!nextQuestId.isBlank() && !"none".equals(nextQuestId) && !"-".equals(nextQuestId)) {
                resolved.add(nextQuestId);
            }
        });

        for (QuestChainBranchRule rule : branchRegistry.branches(completedQuestId)) {
            String nextQuestId = normalize(rule.branchQuestId());
            if (!nextQuestId.isBlank() && !"none".equals(nextQuestId) && !"-".equals(nextQuestId)) {
                resolved.add(nextQuestId);
            }
        }

        return List.copyOf(new ArrayList<>(resolved));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
