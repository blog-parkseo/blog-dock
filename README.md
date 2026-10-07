# blog-dock

티스토리형 블로그 서비스. [GitHub Spec Kit](https://github.com/github/spec-kit)으로 "명세 → 계획 → 작업 → 구현" 순서로 만든다.

## 어디에 무엇이 있나

| 위치 | 내용 |
| --- | --- |
| [`specs/001-tistory-blog/spec.md`](specs/001-tistory-blog/spec.md) | 기능명세서 (71개 기능, 기능 ID는 팀 통합 명세서와 같음) |
| [`specs/001-tistory-blog/checklists/requirements.md`](specs/001-tistory-blog/checklists/requirements.md) | 명세 품질 체크리스트 |
| [`.specify/memory/constitution.md`](.specify/memory/constitution.md) | 프로젝트 원칙 (모든 단계에서 지킬 규칙) |
| `.specify/templates/`, `.specify/scripts/` | Spec Kit 템플릿과 스크립트 |
| `.claude/skills/speckit-*` | Claude Code에서 쓰는 Spec Kit 명령 |

## 다음 단계 (Claude Code에서 순서대로 입력)

1. `/speckit-clarify` — 명세에서 애매한 부분을 질문받고 답을 반영한다 (선택)
2. `/speckit-plan` — 기술 스택을 알려 주면 설계 문서(plan, data-model 등)를 만든다
3. `/speckit-tasks` — 기능 ID별 작업 목록(tasks.md)을 만든다
4. `/speckit-implement` — 작업 목록 순서대로 구현한다

명세를 고치고 싶으면 `spec.md`를 직접 고치거나 `/speckit-specify`로 다시 써도 된다.
