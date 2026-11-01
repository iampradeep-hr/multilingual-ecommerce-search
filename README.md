# Jaldi quick-commerce storefront

An Angular storefront backed by Spring Boot, MongoDB, and local Ollama/Gemma. The Compose stack runs the frontend, API, database, model server, and model download helper in containers.

## Run the full stack with Docker

Install Docker Engine/Desktop with the Docker Compose plugin, then run these commands from the project root:

```bash
cp .env.example .env
docker compose up --build
```

Open [http://localhost:4200](http://localhost:4200). The first launch builds the app images and downloads the `gemma3:4b` model into a persistent Docker volume; allow several minutes and multiple gigabytes of free disk space. Gemma runs on CPU by default and may generate slowly. The Kaggle notebook and public tunnel are not needed for this setup.

`docker compose down` stops the services without deleting MongoDB data or the downloaded model. The initial local-development credentials are in `.env.example`; change them in `.env` before use on a shared machine. Do not commit `.env`.

## Admin

Open [http://localhost:4200/admin](http://localhost:4200/admin) and sign in with the local demo credentials:

- Username: `admin`
- Password: `jaldi123`

The admin area includes an overview, product catalog, and a two-column add-product form. Add search tags by language (English, Hinglish, Hindi, Marathi, Bengali, Assamese, Gujarati, Punjabi, Odia, Tamil, Telugu, Kannada, Malayalam, Urdu, typos, and AI words); press Enter to add each tag. “Quick tags (rules)” suggests tags and common translations for a few grocery terms. “Generate with AI” sends the product to Gemma 3 through the backend and local Ollama container. Admin API endpoints use HTTP Basic authentication.

## API

- `GET /api/health`
- `GET /api/categories`
- `GET /api/search?q=milk&category=All%20items&sort=popular`
- `GET /api/locations/resolve?pincode=400001`
- `GET /api/admin/session` (admin)
- `GET /api/admin/products` (admin)
- `POST /api/admin/products` (admin)
- `POST /api/admin/products/preview-tags` (admin, rules-based; returns language-keyed tag suggestions)
- `POST /api/admin/products/generate-tags` (admin, Gemma 3 via Ollama)

The demo PIN-code lookup recognizes `400001`, `560001`, `110001`, `600001`, and `700001`. Any other six-digit PIN code uses a generic neighbourhood store. Cart contents are kept in the browser; shopper login, checkout, payment processing, and database persistence are not enabled.

The sample catalog is seeded into MongoDB on first startup. Products added by an admin persist in the `mongo-data` Docker volume; the model is persisted in `ollama-data`. Cart contents remain in the browser. The service can also use a remote tag bridge by setting `GEMMA_PROVIDER=remote`, but the default Compose setup uses local Ollama. Admin credentials use HTTP Basic authentication; these Compose defaults are only for local development.
