echo 'Create Kafka Messages'
counter=0
while true
do
  echo 'hero-'$counter':{"name": "hero-'$counter'", "coolFactor": '$(( RANDOM % 100 ))'}'
  ((counter++))
  sleep 2
done | kafka-console-producer.sh \
      --bootstrap-server kafka:9092 --topic super-heros --property parse.key=true --property key.separator=: