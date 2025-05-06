# GuluTurn

GuluTurn is a modular Android app for roulette-style restaurant recommendation.  
It uses semantic filtering to avoid unwanted suggestions and introduces interactive decision-making.

## Modules

- `presentation/` – UI and user interaction
- `semantic/` – Tag generation from user-provided reasons (via OpenAI)
- `restaurant/` – Data source (mock or external)
- `filtering/` – Rule-based filtering logic
- `roulette/` – Roulette-style selection interface

## Status

This project is under active development.  
Requires an OpenAI API key in `local.properties` to run.

For development workflow and architecture, see [CONTRIBUTING.md](CONTRIBUTING.md).
