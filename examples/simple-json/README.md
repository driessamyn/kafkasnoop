# KafkaSnoop Example

## Simple JSON

Example configuration with simple text messages, JSON for example.

You need [docker compose](https://docs.docker.com/compose/) to run the example.

In this directory, run `docker compose up`

This will start Kafka and publish JSON messages to the `super-hero` topic. 
You can see the test topic on: `http://localhost:8080/api` and messages on `http://localhost:8080/api/super-hero`
or use the WebSocket to see new messages streaming in: `ws://localhost:8080/ws/super-heros`

> Tip: use a web socket browser extension such as 
> [WebSocket Weasel](https://addons.mozilla.org/en-GB/firefox/addon/websocket-weasel/)