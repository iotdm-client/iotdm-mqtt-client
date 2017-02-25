package org.opendaylight.iotdm.client.api;

import org.opendaylight.iotdm.client.Request;

public interface Client {
    /**
     * Start client
     */
    public void start();

    /**
     * Stop client
     */
    public void stop();

    /**
     * Send request and get response
     *
     * @param request
     * @return
     */
    public Response send(Request request);
}
