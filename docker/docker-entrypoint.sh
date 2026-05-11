#!/usr/bin/env sh
set -eu

required_vars="
REDIS_URL
KAFKA_SERVER
KAFKA_GROUP_ID
KAFKA_CA_PEM
KAFKA_SVC_PEM
DB_SHARD_0_URL
DB_SHARD_0_USERNAME
DB_SHARD_0_PASSWORD
DB_SHARD_1_URL
DB_SHARD_1_USERNAME
DB_SHARD_1_PASSWORD
"

for var in $required_vars; do
  eval "value=\${$var:-}"
  if [ -z "$value" ]; then
    echo "Missing required environment variable: $var" >&2
    exit 1
  fi
done

envsubst < /app/config/sharding.yml.template > /tmp/sharding.yml

exec java $JAVA_OPTS \
  -Dspring.datasource.url=jdbc:shardingsphere:file:/tmp/sharding.yml \
  -jar /app/vaultr.jar
