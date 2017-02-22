package org.opendaylight.iotdm.client.implementation;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
//import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
//import org.onem2m.xml.protocols.PrimitiveContent;
//import org.opendaylight.iotdm.client.Request;
//import org.opendaylight.iotdm.client.Response;
//import org.opendaylight.iotdm.client.api.Client;
//import org.opendaylight.iotdm.client.exception.Onem2mNoOperationError;
//import org.opendaylight.iotdm.client.util.Json;
//import org.opendaylight.iotdm.constant.OneM2M;

import java.math.BigInteger;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class MQTT implements Client {

    String topic        = "pahodemo/test";
    String content      = "Message from Sameer";
    int qos             = 2;
    String broker       = "tcp://localhost:1883";
    String clientId     = "JavaSample";
    MemoryPersistence persistence = new MemoryPersistence();

    try {
        MqttClient mqttClient = new MqttClient(broker, clientId, persistence);
        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(true);
        System.out.println("Connecting to broker: " + broker);
        mqttClient.connect(connOpts);
        System.out.println("Connected");
    } catch(MqttException me) {
        System.out.println("reason "+me.getReasonCode());
        System.out.println("msg "+me.getMessage());
        System.out.println("loc "+me.getLocalizedMessage());
        System.out.println("cause "+me.getCause());
        System.out.println("excep "+me);
        me.printStackTrace();
    }

    @Override
    public void start() {
        try {
            mqttClient.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        try {
            httpClient.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Response send(Request request) {

        org.eclipse.jetty.client.api.Request httpRequest = buildHttpRequest(request);
        ContentResponse contentResponse;
        try {
            contentResponse = httpRequest.send();
        } catch (Exception e) {
            throw new AssertionError(e.getMessage());
        }

        Response response = new ResponseBuilder(contentResponse).build();
        return response;
    }

    public org.eclipse.jetty.client.api.Request buildHttpRequest(Request request) {
        org.eclipse.jetty.client.api.Request httpRequest = null;
        if (request == null) return null;

        RequestHelper requestHelper = new RequestHelper(request);
        httpRequest = httpClient.newRequest(requestHelper.getHost(), requestHelper.getPort())
                .timeout(requestHelper.getTimeout(), TimeUnit.MILLISECONDS);

        requestHelper.getQuery().remove(OneM2M.Name.RESOURCE_TYPE);
        addQuery(httpRequest, requestHelper.getQuery());
        addHeader(httpRequest.getHeaders(), requestHelper.getHeader());
        httpRequest.accept(requestHelper.getAcceptMIME());
        httpRequest.path(OneM2M.Path.toToPathMapping(requestHelper.getPath()));

        switch (OneM2M.Operation.getEnum(requestHelper.getOp())) {
            case CREATE:
                httpRequest.method(CREATE_IN_HTTP);
                httpRequest.content(new StringContentProvider(requestHelper.getPayload()));
                httpRequest.header(OneM2M.Http.Header.CONTENT_TYPE, String.format("%s;%s=%s", requestHelper.getContentMIME(), OneM2M.Name.RESOURCE_TYPE, request.getRequestPrimitive().getTy()));
                break;
            case RETRIEVE:
                httpRequest.method(RETRIEVE_IN_HTTP);
                httpRequest.header(OneM2M.Http.Header.CONTENT_TYPE, requestHelper.getContentMIME());
                break;
            case UPDATE:
                httpRequest.method(UPDATE_IN_HTTP);
                httpRequest.content(new StringContentProvider(requestHelper.getPayload()));
                httpRequest.header(OneM2M.Http.Header.CONTENT_TYPE, requestHelper.getContentMIME());
                break;
            case DELETE:
                httpRequest.method(DELETE_IN_HTTP);
                httpRequest.header(OneM2M.Http.Header.CONTENT_TYPE, requestHelper.getContentMIME());
                break;
            case NOTIFY:
                httpRequest.method(NOTIFY_IN_HTTP);
                httpRequest.header(OneM2M.Http.Header.CONTENT_TYPE, requestHelper.getContentMIME());
                break;
            default:
                throw new Onem2mNoOperationError();
        }

        return httpRequest;
    }

    protected void addHeader(HttpFields httpFields, Map<String, Set<String>> map) {
        for (Map.Entry<String, Set<String>> entry : map.entrySet()) {
            String key = OneM2M.Http.Header.map(entry.getKey());
            String value = RequestHelper.concatQuery(entry.getValue());
            httpFields.add(key, value);
        }
    }

    protected void addQuery(org.eclipse.jetty.client.api.Request request, Map<String, Set<String>> map) {
        for (Map.Entry<String, Set<String>> entry : map.entrySet()) {
            String key = entry.getKey();
            String value = RequestHelper.concatQuery(entry.getValue());
            request.param(key, value);
        }
    }

    public static class ResponseBuilder {
        private Response response = null;

        public ResponseBuilder(ContentResponse contentResponse) {
            if (contentResponse == null) return;

            OneM2M.ResponseStatusCodes responseStatusCode = null;
            String requestIdentifier = null;
            PrimitiveContent primitiveContent = null;
            String to = null;
            String from = null;
            OneM2M.Time originatingTimestamp = null;
            OneM2M.Time resultExpirationTimestamp = null;
            OneM2M.StdEventCats eventCategory = null;

            HttpFields responseHeader = contentResponse.getHeaders();
            for (String key : responseHeader.getFieldNamesCollection()) {
                switch (key) {
                    case OneM2M.Http.Header.X_M2M_RSC:
                        responseStatusCode = OneM2M.ResponseStatusCodes.getEnum(BigInteger.valueOf(responseHeader.getLongField(key)));
                        break;
                    case OneM2M.Http.Header.X_M2M_RI:
                        requestIdentifier = responseHeader.get(key);
                        break;
                    case OneM2M.Http.Header.X_M2M_ORIGIN:
                        from = responseHeader.get(key);
                        break;
                    case OneM2M.Http.Header.X_M2M_OT:
                        originatingTimestamp = new OneM2M.Time(responseHeader.get(key));
                        break;
                    case OneM2M.Http.Header.X_M2M_RST:
                        resultExpirationTimestamp = new OneM2M.Time(responseHeader.get(key));
                        break;
                    case OneM2M.Http.Header.X_M2M_EC:
                        eventCategory = OneM2M.StdEventCats.getEnum(new BigInteger(responseHeader.get(key)));
                }
            }
            String content = contentResponse.getContentAsString();
            try {
                primitiveContent = Json.newInstance().fromJson(content, PrimitiveContent.class);
            } catch (Exception e) {
                System.err.println("Failed to decode primitive content: " + e);
            }

            response = new Response(responseStatusCode, requestIdentifier, primitiveContent, to, from, originatingTimestamp, resultExpirationTimestamp, eventCategory);
        }

        public Response build() {
            return response;
        }
    }