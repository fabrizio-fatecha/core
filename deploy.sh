#!/bin/sh
set -eu

HOST="${DEPLOY_HOST:?DEPLOY_HOST is required}"
KEY="${DEPLOY_KEY:-$HOME/.ssh/core-ec2}"
SSH_OPTS="-i $KEY -o StrictHostKeyChecking=accept-new"

docker build --platform linux/arm64 -t core-api:latest .
docker save core-api:latest | gzip | ssh $SSH_OPTS "ubuntu@$HOST" 'gunzip | docker load'

rsync -az --delete --inplace -e "ssh $SSH_OPTS" docker-compose.yaml docker-compose.prod.yaml nginx db "ubuntu@$HOST:core/"

ssh $SSH_OPTS "ubuntu@$HOST" 'cd core && test -f .env || (umask 077 && printf "POSTGRES_PASSWORD=%s\nAPP_DB_RO_PASSWORD=%s\nHTTP_PORT=80\n" "$(openssl rand -hex 24)" "$(openssl rand -hex 24)" > .env)'

ssh $SSH_OPTS "ubuntu@$HOST" 'cd core && docker compose up -d --no-build && docker compose exec -T nginx nginx -s reload'
