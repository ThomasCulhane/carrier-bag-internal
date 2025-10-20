# carrier-bag-internal — Docker image with nginx + React frontend + Node backend

This repository contains a multi-stage Dockerfile that builds a React frontend (in `frontend/`) and a Node backend (in `backend/`) and produces a final image based on `nginx:stable-alpine` that serves the frontend and proxies `/api` requests to the backend running inside the same container.

Assumptions
- `frontend/` is a standard Create React App or similar with `npm run build` producing `build/` output.
- `frontend/package.json` and optional `yarn.lock` or `package-lock.json` present.
- `backend/` is a Node app with `npm start` or an `index.js` entrypoint. The backend listens on port 3000 by default.

Build the image (from repository root)

```powershell
docker build -t carrier-bag-app -f .\dockerfile .
```

Run the container (map port 80)

```powershell
docker run --rm -p 80:80 --name carrier-bag carrier-bag-app
```

If your backend uses a different start command or port, update `backend/package.json` scripts or set `BACKEND_PORT` environment variable when running the container.

Notes
- This image runs both the Node backend and nginx in one container for simplicity. For production, consider splitting the backend into a separate service (recommended) and using nginx as a reverse proxy in front of them.
