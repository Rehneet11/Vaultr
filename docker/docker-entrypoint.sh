#!/usr/bin/env sh
set -eu

required_vars="
REDIS_URL
KAFKA_SERVER
KAFKA_GROUP_ID
KAFKA_CA_PEM
KAFKA_SVC_PEM
"

for var in $required_vars; do
  eval "value=\${$var:-}"
  if [ -z "$value" ]; then
    echo "Missing required environment variable: $var" >&2
    exit 1
  fi
done

if [ ! -r /opt/vaultr/sharding.yml ]; then
  echo "Missing readable ShardingSphere config: /opt/vaultr/sharding.yml" >&2
  exit 1
fi

exec java $JAVA_OPTS -jar /app/vaultr.jar
