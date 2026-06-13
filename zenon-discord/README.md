# Zenon Discord (YUKI-01 / 유키)

Zenon 서버 **중앙제어** 디스코드 봇. RPG·포로몬·공지·역할·이벤트·운영자 명령어를
관리하는 운영 허브다. 실제 게임 로직은 RPG 플러그인/포로몬 서버가 담당하고,
봇은 명령어·권한·알림·조회·운영자 UI 만 담당한다.

스택: Python 3.12 / discord.py 2.3

## 구조

```
main.py            엔트리포인트 (EXTENSIONS 모듈 로드)
core/              config(.env 로드) · permissions(권한/알림 역할 정책)
integrations/      rpg_api(구현) · poromon_api(스텁)
modules/           common · rpg · roles · poromon(스텁) · event(스텁) · admin(스텁)
docs/              설계/명세 문서
```

도메인별 모듈 분리: RPG/포로몬은 서로 import 하지 않는다. 자세한 작업 규칙은
[`CLAUDE.md`](CLAUDE.md) 참조.

## 봇 이름 정책

- 문서·README·embed 기본 설명·사용자 안내 문구의 기본 표기는 **YUKI-01**(한국어 별칭 **유키**)다.
- **코드 내부 봇 클래스/개념 명칭은 `YukiBot` 으로 통일**한다.
- 실제 Discord 에 노출되는 봇 계정 이름은 **Discord Developer Portal 의 Application/Bot 이름** 또는
  **서버 닉네임**에서 관리한다. 코드에서 봇 표시명을 강제로 바꾸는 로직은 두지 않는다.
- 운영자는 서버별 닉네임을 `유키` · `ユキ` · `YUKI-01` 등으로 자유롭게 설정할 수 있다.

## 실행

```bash
cp .env.example .env     # 값 채우기 (토큰·역할 ID 등)
pip install -r requirements.txt
python main.py
```

`.env`(토큰·비밀키·역할 ID)는 절대 커밋하지 않는다.
