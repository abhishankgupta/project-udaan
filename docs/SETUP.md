# One-time setup: test and production

This connects the GitHub repo to free hosting. Do it once, in this order. It takes about 45 minutes.

| Piece | Service | Test | Production |
| --- | --- | --- | --- |
| API (Spring Boot) | Render, free web service (Docker image) | `udaan-api-test` | `udaan-api-prod` |
| Database (PostgreSQL + PostGIS) | Neon, free plan | project `udaan-test` | project `udaan-prod` |
| Web app (React) | Cloudflare Pages, free | project `udaan-web-test` | project `udaan-web` |
| API image | GitHub Container Registry | shared: one image per commit | same image as test |

Pick regions close to India on every service: Neon **AWS Asia Pacific (Singapore)**, Render **Singapore**.

## 1. GitHub repo

1. Create the public repo `project-udaan` and push the code (Claude does this after your approval).
2. The first push runs the **Deploy** workflow. CI and **Build API image** pass; **Test** fails with "environment is not set up yet". That's expected: it means the image now exists for Render to use.
3. Make the image public so Render can pull it: GitHub → your profile → **Packages** → `udaan-api` → **Package settings** → **Change visibility** → Public.

## 2. Neon (database), once per environment

1. Sign up at neon.com (free, no card). Create project `udaan-test`, Postgres 16, region Singapore.
2. On the dashboard, open **Connect**, turn **Connection pooling off** (Flyway needs a direct connection), and copy the parameters.
3. Build the JDBC values for Render:
   - `SPRING_DATASOURCE_URL` = `jdbc:postgresql://<host>/<database>?sslmode=require`
   - `SPRING_DATASOURCE_USERNAME` = the role name
   - `SPRING_DATASOURCE_PASSWORD` = the password
4. Repeat for `udaan-prod`.

PostGIS is enabled automatically by the app's first database migration.

## 3. Render (API), once per environment

1. Sign up at render.com (free, no card). **New → Web Service → Existing image**.
2. Image URL: `ghcr.io/<your-github-username>/udaan-api:<commit-sha>` (copy the full tag from the **Build API image** job log). Name `udaan-api-test`, region Singapore, instance type **Free**.
3. Environment variables:

   | Key | Test value | Production value |
   | --- | --- | --- |
   | `SPRING_DATASOURCE_URL` | from Neon `udaan-test` | from Neon `udaan-prod` |
   | `SPRING_DATASOURCE_USERNAME` | from Neon | from Neon |
   | `SPRING_DATASOURCE_PASSWORD` | from Neon | from Neon |
   | `UDAAN_ENVIRONMENT` | `test` | `production` |
   | `UDAAN_CORS_ORIGINS` | `https://udaan-web-test.pages.dev` | `https://udaan-web.pages.dev` |

4. Health check path: `/actuator/health`.
5. **Settings → Deploy Hook**: copy the URL. It is a secret.
6. Repeat for `udaan-api-prod`.

## 4. Cloudflare Pages (web app)

1. Sign up at cloudflare.com (free). **Workers & Pages → Create → Pages → Direct Upload**, create project `udaan-web-test`, then `udaan-web`. (Uploading any placeholder file is fine; the pipeline replaces it.)
2. **My Profile → API Tokens → Create Token → "Edit Cloudflare Workers" template**, or a custom token with *Account → Cloudflare Pages → Edit*. Copy it.
3. Copy your **Account ID** from the Workers & Pages overview page.

## 5. GitHub environments and secrets

Repo → **Settings → Secrets and variables → Actions**:

- Repository secrets: `CLOUDFLARE_API_TOKEN`, `CLOUDFLARE_ACCOUNT_ID`.

Repo → **Settings → Environments**, create `test` and `production`:

| Name | Kind | `test` | `production` |
| --- | --- | --- | --- |
| `RENDER_DEPLOY_HOOK` | secret | test deploy hook URL | prod deploy hook URL |
| `API_URL` | variable | `https://udaan-api-test.onrender.com` | `https://udaan-api-prod.onrender.com` |
| `WEB_URL` | variable | `https://udaan-web-test.pages.dev` | `https://udaan-web.pages.dev` |
| `CF_PAGES_PROJECT` | variable | `udaan-web-test` | `udaan-web` |

On `production` only: tick **Required reviewers**, add yourself, and under **Deployment branches** allow only `main`. This is the gate: production never deploys until you click **Approve**.

Use the exact URLs Render and Cloudflare show you if they differ from the defaults above.

## 6. First real deploy

Actions → **Deploy** → open the failed run → **Re-run all jobs**. Test deploys and passes its smoke test; Production then shows **Waiting for review**.

## Day-to-day flow

1. Claude builds a change on a branch and shows it to you.
2. You approve; Claude pushes and merges to `main`.
3. GitHub Actions runs CI, builds the image, deploys to **test**, and checks it.
4. You check the test site. If it's good, open the run in GitHub Actions and click **Review deployments → Approve**.
5. Production gets the identical image and web build.

## Limits to know

- Render free services sleep after 15 minutes idle; the first request then takes about a minute. Before the pilot launches, move `udaan-api-prod` to the paid Starter instance to keep it awake.
- Neon free projects suspend if they exceed the monthly compute or 0.5 GB storage allowance until the next month. Watch usage in the Neon dashboard.
