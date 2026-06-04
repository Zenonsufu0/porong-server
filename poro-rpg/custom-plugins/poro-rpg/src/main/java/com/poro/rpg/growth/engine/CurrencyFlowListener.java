package com.poro.rpg.growth.engine;

/**
 * 지갑 통화 변동 리스너 — 골드 인플레이션/싱크 판단용 (INBOX-004 #2 / DL-080).
 * <p>실제 게임플레이 변동(addCurrency/consumeCurrency)에서만 발생한다. 로드/복원은 restoreCurrency로 우회하여
 * 제외된다. 환불은 inflow로 잡혀 직전 outflow와 net 상쇄, 경매 이체도 buyer/seller가 net 상쇄되므로
 * 발행량 vs 소각량(net)이 정확하다.</p>
 */
@FunctionalInterface
public interface CurrencyFlowListener {
    /**
     * @param userId   플레이어 UUID 문자열
     * @param inflow   true=획득(inflow), false=소모(outflow)
     * @param currency 정규화된 통화 코드 ('gold' 등)
     * @param amount   변동량 (양수)
     */
    void onFlow(String userId, boolean inflow, String currency, long amount);
}
