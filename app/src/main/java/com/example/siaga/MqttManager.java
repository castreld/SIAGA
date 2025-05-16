package com.example.siaga;
import android.content.Context;
import android.util.Log;
import java.util.UUID;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class MqttManager {

    private static final String TAG = "MqttManager";
    private MqttAndroidClient mqttClient;
    private String brokerUri;
    private String clientId;
    private Context context;
    private MqttCallbackHandler callbackHandler;

    public MqttManager(Context context, String brokerUri, String clientId, MqttCallbackHandler callbackHandler) {
        this.context = context;
        this.brokerUri = brokerUri;
        this.clientId = clientId;
        this.callbackHandler = callbackHandler;
        connect();
    }

    private void connect() {
        mqttClient = new MqttAndroidClient(context, brokerUri, clientId);
        mqttClient.setCallback(new MqttCallbackHandlerImpl());

        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setUserName("SiagaCluster");
        mqttConnectOptions.setPassword("PVU9KineFU.r7H8".toCharArray());
        mqttConnectOptions.setCleanSession(true);

        try {
            mqttClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.i(TAG, "Connected to HiveMQ broker: " + brokerUri);
                    if (callbackHandler != null) {
                        callbackHandler.onConnectSuccess();
                    }
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.e(TAG, "Failed to connect to HiveMQ broker: " + brokerUri + " - " + exception.getMessage());
                    if (callbackHandler != null) {
                        callbackHandler.onConnectFailure(exception);
                    }
                }
            });
        } catch (MqttException e) {
            Log.e(TAG, "Error during MQTT connection: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public interface MqttCallbackHandler {
        void onConnectSuccess();
        void onConnectFailure(Throwable exception);
        void onConnectionLost(Throwable cause);
        void onMessageReceived(String topic, MqttMessage message);
        void onDeliveryComplete(IMqttDeliveryToken token);
    }

    private class MqttCallbackHandlerImpl implements org.eclipse.paho.client.mqttv3.MqttCallback {
        @Override
        public void connectionLost(Throwable cause) {
            Log.w(TAG, "Connection to HiveMQ broker lost: " + cause.getMessage());
            if (callbackHandler != null) {
                callbackHandler.onConnectionLost(cause);
            }
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            Log.i(TAG, "Received message on topic '" + topic + "': " + new String(message.getPayload()));
            if (callbackHandler != null) {
                callbackHandler.onMessageReceived(topic, message);
            }
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
            Log.d(TAG, "Message delivery complete: " + token.getMessageId());
            if (callbackHandler != null) {
                callbackHandler.onDeliveryComplete(token);
            }
        }
    }

    public void subscribe(String topic) {
        try {
            mqttClient.subscribe(topic, 0, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.i(TAG, "Subscribed to topic: " + topic);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.e(TAG, "Failed to subscribe to topic '" + topic + "': " + exception.getMessage());
                }
            });
        } catch (MqttException e) {
            Log.e(TAG, "Error subscribing to topic '" + topic + "': " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void unsubscribe(String topic) {
        try {
            mqttClient.unsubscribe(topic, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.i(TAG, "Unsubscribed from topic: " + topic);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.e(TAG, "Failed to unsubscribe from topic '" + topic + "': " + exception.getMessage());
                }
            });
        } catch (MqttException e) {
            Log.e(TAG, "Error unsubscribing from topic '" + topic + "': " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void publish(String topic, String payload) {
        MqttMessage message = new MqttMessage(payload.getBytes());
        try {
            mqttClient.publish(topic, message);
            Log.i(TAG, "Published message '" + payload + "' to topic '" + topic + "'");
        } catch (MqttException e) {
            Log.e(TAG, "Error publishing message to topic '" + topic + "': " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void disconnect() {
        try {
            mqttClient.disconnect();
            Log.i(TAG, "Disconnected from HiveMQ broker");
        } catch (MqttException e) {
            Log.e(TAG, "Error disconnecting from HiveMQ broker: " + e.getMessage());
            e.printStackTrace();
        }
    }
}