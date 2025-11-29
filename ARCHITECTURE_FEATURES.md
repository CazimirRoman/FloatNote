Feature-Oriented Structure (Proposal)

- app/src/main/java/dev/cazimir/floatnote/core/
  - ui/
    - theme/
    - components/
  - data/
  - di/
  - navigation/
- app/src/main/java/dev/cazimir/floatnote/feature/
  - settings/
  - history/
  - bubble/
  - onboarding/

Migration tips:
- Move existing files into feature folders incrementally.
- Update package names and imports accordingly.
- Keep DI modules in core/di and inject into features.
- Prefer ViewModels per feature, provided via Koin.

