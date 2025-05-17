package com.example.siaga;
    
import android.util.Log;
import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.MqttGlobalPublishFilter;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.function.Consumer;

public class HiveMqttManager {
    private static final String TAG = "HiveMqttManager";
    private final Mqtt3AsyncClient client;

    public HiveMqttManager(String brokerUri, String clientId) {
        client = MqttClient.builder()
                .useMqttVersion3()
                .identifier(clientId)
                .serverHost(brokerUri)
                .serverPort(8883)
                .sslWithDefaultConfig()
                .buildAsync();
    }

    public HiveMqttManager(String brokerUri, int port, String clientId, String username, String password) {
        client = MqttClient.builder()
                .useMqttVersion3()
                .identifier(clientId)
                .serverHost(brokerUri)
                .serverPort(port)
                .sslWithDefaultConfig()
                .simpleAuth()
                    .username(username)
                    .password(password.getBytes(StandardCharsets.UTF_8))
                    .applySimpleAuth()
                .buildAsync();
    }

    public void connect(Runnable onSuccess, Consumer<Throwable> onFailure) {
        client.connect()
                .whenComplete((connAck, throwable) -> {
                    if (throwable == null) {
                        Log.i(TAG, "Connected to HiveMQ broker");
                        if (onSuccess != null) onSuccess.run();
                    } else {
                        Log.e(TAG, "Failed to connect: " + throwable.getMessage());
                        if (onFailure != null) onFailure.accept(throwable);
                    }
                });
    }

    public void subscribe(String topic, Consumer<String> onMessage) {
        client.subscribeWith()
                .topicFilter(topic)
                .callback(publish -> {
                    String payload = new String(publish.getPayloadAsBytes(), StandardCharsets.UTF_8);
                    Log.i(TAG, "Received message on " + topic + ": " + payload);
                    if (onMessage != null) onMessage.accept(payload);
                })
                .send();
    }

    public void publish(String topic, String payload) {
        client.publishWith()
                .topic(topic)
                .payload(payload.getBytes(StandardCharsets.UTF_8))
                .send();
    }

    public void disconnect() {
        client.disconnect();
    }
}