build-frontend-local:
	cd frontend && npm install && npm run build

start-frontend-local: build-frontend-local
	cd frontend && npm run start

build:
	docker-compose build

run: build
	docker-compose up