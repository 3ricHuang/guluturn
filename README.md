# GuluTurn

GuluTurn is a modular Android app for roulette-style restaurant recommendation.  
It uses semantic filtering to avoid unwanted suggestions and introduces interactive decision-making.

## Modules

### Current (Single Gradle Module: `app/`)

All code lives under `app/src/main/java/com/eric/guluturn/` with an internal package layout already anticipating future extraction:

| Package | Responsibility (current scope) |
|---------|--------------------------------|
| `common/` | Cross-cutting: constants, simple models, errors, storage (API key), utilities (tag config, embeddings) |
| `data/` | Low-level Firestore helpers & schema definition |
| `repository/` | Repository interfaces + Firestore implementations (profiles, restaurants, interaction sessions) |
| `semantic/` | OpenAI + static tag generation, request/response models, prompt templates |
| `filter/` | Filtering pipeline: pre-filters, hard filters, tag scoring, advancement selection, stateful engine, tag registry, re-ranking |
| `ui/` | Presentation layer: activities, view models, adapters, custom views (login, profile flow, roulette, theming) |

## Status

This project is under active development.
