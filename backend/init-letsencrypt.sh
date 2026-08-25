#!/bin/bash
# EC2 서버에서 딱 한 번만 수동으로 실행하는 부트스트랩 스크립트.
# nginx가 처음 뜰 때 참조할 인증서가 아예 없으면 시작 자체가 실패하므로,
# 더미 인증서로 일단 nginx를 띄운 뒤 실제 Let's Encrypt 인증서로 교체한다.
#
# 사용법 (EC2에서, docker-compose.yml과 같은 폴더에서):
#   ./init-letsencrypt.sh
set -e

domain=samsam55.duckdns.org
email=  # 여기에 실제로 알림 받을 이메일 채우기 (비워두면 --register-unsafely-without-email 로 진행)
rsa_key_size=4096
data_path="./certbot"

if [ -d "$data_path" ]; then
  read -p "Existing data found for $domain. Continue and replace existing certificate? (y/N) " decision
  if [ "$decision" != "Y" ] && [ "$decision" != "y" ]; then
    exit
  fi
fi

mkdir -p "$data_path/conf"

echo "### Creating dummy certificate for $domain ..."
path="/etc/letsencrypt/live/$domain"
mkdir -p "$data_path/conf/live/$domain"
docker compose run --rm --entrypoint "\
  openssl req -x509 -nodes -newkey rsa:$rsa_key_size -days 1\
    -keyout '$path/privkey.pem' \
    -out '$path/fullchain.pem' \
    -subj '/CN=localhost'" certbot

echo "### Starting nginx ..."
docker compose up --force-recreate -d nginx

echo "### Deleting dummy certificate for $domain ..."
docker compose run --rm --entrypoint "\
  rm -Rf /etc/letsencrypt/live/$domain && \
  rm -Rf /etc/letsencrypt/archive/$domain && \
  rm -Rf /etc/letsencrypt/renewal/$domain.conf" certbot

echo "### Requesting Let's Encrypt certificate for $domain ..."
email_arg="--register-unsafely-without-email"
if [ -n "$email" ]; then
  email_arg="--email $email --no-eff-email"
fi

docker compose run --rm --entrypoint "\
  certbot certonly --webroot -w /var/www/certbot \
    $email_arg \
    -d $domain \
    --rsa-key-size $rsa_key_size \
    --agree-tos \
    --force-renewal" certbot

echo "### Reloading nginx ..."
docker compose exec nginx nginx -s reload
