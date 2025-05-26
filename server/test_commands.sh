#!/bin/bash

COMMANDS=(
  "sleep 2"
  "sleep 2"
  "sleep 4"
  "echo a"
  "echo b"
  "echo c"
)

URL="http://localhost:8080/run-process?cmd="

timestamp() {
  date +"%H:%M:%S"
}

echo "== TESTE: Enviar sequencial, ler resposta async =="

for CMD in "${COMMANDS[@]}"; do
  (
    START=$(timestamp)
    echo "[$START] Enviando comando: '$CMD'"
    RESPONSE=$(curl -s "${URL}$(echo $CMD | sed 's/ /+/g')")
    END=$(timestamp)
    echo "[$END] RESPOSTA para '$CMD':"
    echo "$RESPONSE"
    echo "----------------------------------------"
  ) &
  sleep 0.1  # para garantir ordem de envio
done

wait

echo "== FIM DO TESTE =="
