package org.opendaylight.iotdm.client;

import org.opendaylight.iotdm.client.implementation.MQTT;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class Main {

    public static void main(String[] args) {

        // Test Code

        System.out.println("Invoking client.java");
        MQTT testMqqt = new MQTT();
        String s = "test string";
        testMqqt.send(s);

    }
}