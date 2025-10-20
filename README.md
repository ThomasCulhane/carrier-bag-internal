# carrier-bag-internal — Docker image with nginx + React frontend + Node backend

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
