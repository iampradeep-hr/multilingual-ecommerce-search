# Mila? — Multilingual E-commerce Search

An Angular storefront backed by Spring Boot, MongoDB, Kafka, Typesense, and local Ollama/Gemma. The Compose stack runs the frontend, API, database, event broker, search engine, model server, and model download helper in containers.

## Run the full stack with Podman

Install Podman and a Compose provider on Ubuntu. The configured Compose provider is used by `podman compose`; run these commands from the project root:

```bash
cp .env.example .env
podman compose up --build
```

`.env.example` contains local-development values only. Before starting the stack, edit `.env` and set your own values for `MONGO_ROOT_USERNAME`, `MONGO_ROOT_PASSWORD`, `JALDI_ADMIN_USERNAME`, `JALDI_ADMIN_PASSWORD`, and `TYPESENSE_API_KEY`. For example, generate URL-safe passwords and a Typesense key with:

```bash
openssl rand -hex 24
```

Run the command separately for each password/key, then paste each result into the matching variable in `.env`. Use URL-safe characters for `MONGO_ROOT_PASSWORD` because Compose places it in the MongoDB connection URI. `.env` is ignored by Git; commit only `.env.example`, and never put real credentials in source files or commit them. To confirm the local file is ignored, run `git check-ignore -v .env`. The example values are intentionally convenient for local development and are not suitable for a shared machine or deployment.

Open [http://localhost:4200](http://localhost:4200). The first launch builds the app images, downloads the `gemma3:4b` model into a persistent volume, and starts Kafka and Typesense; allow several minutes and multiple gigabytes of free disk space. Gemma runs on CPU by default and may generate slowly. New admin products are accepted into Kafka, saved by the ingest worker, then sent to the index worker for Typesense. Existing MongoDB products are reindexed at backend startup. Typesense indexes product names, brands, categories, and all generated/manual multilingual tags. The Kaggle notebook and public tunnel are not needed for this setup.

`podman compose down` stops the services without deleting MongoDB data, Kafka data, the Typesense index, or the downloaded model.

The demo search recognizes Hinglish and Hindi product tags. For example, searching `machar kaat rahe hai baba` returns the Mortein mosquito repellent products.

## Admin

Open [http://localhost:4200/admin](http://localhost:4200/admin) and sign in with the admin credentials set by `JALDI_ADMIN_USERNAME` and `JALDI_ADMIN_PASSWORD` in `.env`. The unmodified example file uses:

- Username: `admin`
- Password: `jaldi123`

The admin area includes an overview, product catalog, and a two-column add-product form. Add search tags by language (English, Hinglish, Hindi, Marathi, Bengali, Assamese, Gujarati, Punjabi, Odia, Tamil, Telugu, Kannada, Malayalam, typos, and AI words); press Enter to add each tag. On save, known rule-based translations are merged with the supplied tags, so a product whose name includes “milk” also gets common `doodh`/Hindi and regional-language aliases for search. “Quick tags (rules)” suggests tags and common translations for a few grocery terms. “Generate with AI” sends the product to Gemma 3 through the backend and local Ollama container, requesting tags in Marathi, Gujarati, Punjabi, Tamil, Telugu, Kannada, and Malayalam as well. Admin API endpoints use HTTP Basic authentication.

## API

- `GET /api/health`
- `GET /api/categories`
- `GET /api/search?q=milk&category=All%20items&sort=popular`
- `GET /api/locations/resolve?pincode=400001`
- `GET /api/admin/session` (admin)
- `GET /api/admin/products` (admin)
- `POST /api/admin/products` (admin; responds `202 Accepted` when queued through Kafka)
- `DELETE /api/admin/products/{productId}` (admin; queues removal and Typesense cleanup through Kafka)
- `POST /api/admin/products/preview-tags` (admin, rules-based; returns language-keyed tag suggestions)
- `POST /api/admin/products/generate-tags` (admin, Gemma 3 via Ollama)

The demo PIN-code lookup recognizes `400001`, `560001`, `110001`, `600001`, and `700001`. Any other six-digit PIN code uses a generic neighbourhood store. Cart contents are kept in the browser; shopper login, checkout, payment processing, and database persistence are not enabled.

The 38-product starter catalog is seeded idempotently into MongoDB at startup: missing starter entries are added without overwriting existing products. This project’s catalog is separate from the reference tag generator’s `product-kb.json`, which provides product-language aliases rather than a store inventory. Product aliases and Hindi/Hinglish terms are stored with each item and indexed for Typesense search. Products added by an admin persist in the `mongo-data` Docker volume; the model is persisted in `ollama-data`. Ollama is available to the backend at `http://ollama:11434` and to local tools at `http://localhost:11434`; the `ollama-models` helper exits successfully after pulling the model, while the Ollama server stays running. Cart contents remain in the browser. The service can also use a remote tag bridge by setting `GEMMA_PROVIDER=remote`, but the default Compose setup uses local Ollama. Admin credentials use HTTP Basic authentication; all values in `.env.example` are for local development only.
