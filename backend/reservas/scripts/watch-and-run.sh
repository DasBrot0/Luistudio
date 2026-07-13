#!/bin/sh
set -u

watch_sources() {
  previous_fingerprint=''

  while true; do
    fingerprint="$(find src -type f -printf '%T@:%s:%p\n' | sort | sha256sum)"

    if [ -n "$previous_fingerprint" ] && [ "$fingerprint" != "$previous_fingerprint" ]; then
      echo "Cambios detectados en backend: recompilando..."
      ./mvnw -q -DskipTests compile || echo "La compilación falló; se conservará la versión anterior hasta corregir el error."
    fi

    previous_fingerprint="$fingerprint"
    sleep "${BACKEND_WATCH_INTERVAL:-1}"
  done
}

watch_sources &
watcher_pid=$!

./mvnw spring-boot:run &
app_pid=$!

trap 'kill "$watcher_pid" "$app_pid" 2>/dev/null; wait "$app_pid" 2>/dev/null; exit 0' INT TERM
wait "$app_pid"
