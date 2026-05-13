---
name: tech-review
description: 기획안이나 구현안을 기술 관점에서 짧게 검토할 때 사용
argument-hint: [검토할 내용]
disable-model-invocation: true
effort: low
---

# tech-review

검토 대상: $ARGUMENTS

목표:
- 먼저 현재 초안/구현안을 짧게 요약한다.
- 가능하면 연결된 codex MCP를 활용해 기술 리스크를 검토한다.
- codex 결과를 그대로 복붙하지 말고 포로 서버 기준에 맞춰 다시 정리한다.

반드시 확인:
- 기존 포로 서버 기준과 충돌 여부
- 구현 난이도
- 빠진 요구사항
- 데이터 구조/API 영향
- 테스트 포인트
- 1차 구현 마일스톤

출력 형식:
1. 초안 요약
2. 기술 리스크
3. 빠진 요구사항
4. 데이터/API 영향
5. 테스트 포인트
6. 마일스톤
7. 최종 추천안
