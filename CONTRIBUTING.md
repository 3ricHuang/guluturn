# Contributing to GuluTurn

This document outlines the contribution workflow and architectural conventions for the GuluTurn project.  
GuluTurn is a modular Android application for restaurant recommendation using semantic filtering and interactive roulette-based selection.

---

## Project Structure

```

app/
├── presentation/         # UI components and ViewModels
├── semantic/             # Tag generation from refusal reasons (OpenAI)
├── restaurant/           # Restaurant data source (mock or real)
├── filtering/            # Recommendation filtering logic
├── roulette/             # Spinner selection interface
├── review_summary/       # Review summarization logic
├── sync/                 # Shared history or cloud sync
└── shared/               # Common models, tag definitions, utilities

```

---

## Modular Responsibilities

| Module            | Purpose                                              | Status   |
|-------------------|------------------------------------------------------|----------|
| `semantic/`       | Transforms natural language into tag list using GPT  | Planned  |
| `restaurant/`     | Supplies restaurant data, initially mocked           | Planned  |
| `filtering/`      | Filters out restaurants based on tags                | Planned  |
| `presentation/`   | Handles user input and displays recommendations      | Planned  |
| `roulette/`       | Roulette interface for random selection              | Planned  |
| `review_summary/` | Extracts tags from review content                    | Optional |
| `sync/`           | Synchronizes user state across sessions              | Optional |


---

## Development Setup

1. Clone the repository:

    ```bash
    git clone https://github.com/3ricHuang/guluturn.git
    cd guluturn
    ```

2. Open the project in Android Studio.

3. Add your OpenAI API key in `local.properties`:

    ```properties
    openai.api.key=sk-xxxxxxxxxxxxxx
    ```

4. Run the application using `MainScreen.kt` as entry point.

---

## Contribution Workflow

### 1. Branch Naming

Use feature-based branches for each module or improvement.

```bash
git checkout -b feature/<module-name>-<short-description>
```

Examples:

* `feature/semantic-openai-generator`
* `feature/presentation-result-screen`
* `fix/filtering-null-check`

---

### 2. Commit Message Convention

Follow conventional commit format:

```bash
<type>: <description>
```

| Type       | Purpose                               |
|------------|---------------------------------------|
| `feat`     | New feature                           |
| `fix`      | Bug fix                               |
| `refactor` | Code refactoring (no behavior change) |
| `docs`     | Documentation update                  |
| `chore`    | Maintenance, config, or build         |

Example:

```bash
feat: add OpenAiTagGenerator with prompt injection
```

---

### 3. Pull Request Checklist

Before submitting a PR:

* [ ] Code is inside the proper module folder
* [ ] Public functions are documented (if applicable)
* [ ] New logic is covered by a test or mock example
* [ ] No API key or sensitive data is committed
* [ ] Project compiles and runs without error

---

## Contribution Guidelines

* Maintain clean separation of modules. No cross-module imports unless via interface.
* Use interfaces such as `TagGenerator` and `RestaurantRepository` for all logic interaction.
* Prefer immutable data and Kotlin idiomatic practices.
* Avoid UI logic in ViewModel and business logic in Composables.
* Group related logic and data models in their respective modules.

---

## Code Style

* Kotlin only (no Java)
* Follow Android Kotlin style guide
* Use `ViewModel`, `StateFlow`, and unidirectional data flow
* Avoid global state unless managed via DI or Singleton pattern (with justification)

---

## Issues and Discussion

* Use GitHub Issues to report bugs, propose features, or discuss implementation approaches.
* For major design changes, open a draft pull request or request feedback via issue.

---

## Licensing and Attribution

Ensure that any third-party content, API, or service used in the project complies with its respective license.
Any external dataset or service used must be clearly documented.

---

Thank you for contributing to GuluTurn.
