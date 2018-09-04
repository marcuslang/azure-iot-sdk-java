/*
 *  Copyright (c) Microsoft. All rights reserved.
 *  Licensed under the MIT license. See LICENSE file in the project root for full license information.
 */

package tests.integration.com.microsoft.azure.sdk.iot.serviceclient;

import com.microsoft.azure.sdk.iot.service.*;
import com.microsoft.azure.sdk.iot.service.exceptions.IotHubException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static com.microsoft.azure.sdk.iot.service.Tools.*;
import static org.junit.Assert.assertEquals;
import static tests.integration.com.microsoft.azure.sdk.iot.helpers.Tools.retrieveEnvironmentVariableValue;

public class ServiceClientIT
{
    private static String connectionStringEnvVarName = "IOTHUB_CONNECTION_STRING";
    private static String connectionString = "";
    private static String deviceId = "java-service-client-e2e-test";
    private static String content = "abcdefghijklmnopqrstuvwxyz1234567890";

    //connection string to server with untrustworthy certificates. Service should throw an exception when connecting to it
    private static String IOT_HUB_CONNECTION_STRING_ENV_VAR_NAME = "IOTHUB_DEVICE_CONN_STRING_INVALIDCERT";

    @Before
    public void setUp()
    {
        Map<String, String> env = System.getenv();
        for (String envName : env.keySet())
        {
            if (envName.equals(connectionStringEnvVarName.toString()))
            {
                connectionString = env.get(envName);
            }
        }

        String uuid = UUID.randomUUID().toString();
        deviceId = deviceId.concat("-" + uuid);
    }

    @Test
    @Ignore
    public void service_client_e2e_ampqs() throws Exception
    {
        IotHubServiceClientProtocol protocol = IotHubServiceClientProtocol.AMQPS;

        if (isNullOrEmpty(connectionString))
        {
            throw new IllegalArgumentException("Environment variable is not set: " + connectionStringEnvVarName);
        }

        // Arrange

        // We remove and recreate the device for a clean start
        RegistryManager registryManager = RegistryManager.createFromConnectionString(connectionString);

        try
        {
            registryManager.removeDevice(deviceId);
        }
        catch (IOException|IotHubException e)
        {
        }

        Device deviceAdded = Device.createFromId(deviceId, null, null);
        registryManager.addDevice(deviceAdded);

        Device deviceGetBefore = registryManager.getDevice(deviceId);

        // Act

        // Create service client
        ServiceClient serviceClient = ServiceClient.createFromConnectionString(connectionString, protocol);
        CompletableFuture<Void> futureOpen = serviceClient.openAsync();
        futureOpen.get();

        Message message = new Message(content.getBytes());

        CompletableFuture<Void> completableFuture = serviceClient.sendAsync(deviceId, message);
        completableFuture.get();

        Device deviceGetAfter = registryManager.getDevice(deviceId);
        CompletableFuture<Void> futureClose = serviceClient.closeAsync();
        futureClose.get();

        registryManager.removeDevice(deviceId);

        // Assert
        assertEquals(deviceGetBefore.getDeviceId(), deviceGetAfter.getDeviceId());
        assertEquals(0, deviceGetBefore.getCloudToDeviceMessageCount());
        assertEquals(1, deviceGetAfter.getCloudToDeviceMessageCount());

        registryManager.close();
    }

    @Test
    @Ignore
    public void service_client_e2e_amqps_ws() throws Exception
    {
        IotHubServiceClientProtocol protocol = IotHubServiceClientProtocol.AMQPS_WS;

        if (isNullOrEmpty(connectionString))
        {
            throw new IllegalArgumentException("Environment variable is not set: " + connectionStringEnvVarName);
        }

        // Arrange

        // We remove and recreate the device for a clean start
        RegistryManager registryManager = RegistryManager.createFromConnectionString(connectionString);

        try
        {
            registryManager.removeDevice(deviceId);
        } catch (IOException|IotHubException e)
        {
        }

        Device deviceAdded = Device.createFromId(deviceId, null, null);
        registryManager.addDevice(deviceAdded);

        Device deviceGetBefore = registryManager.getDevice(deviceId);

        // Act

        // Create service client
        ServiceClient serviceClient = ServiceClient.createFromConnectionString(connectionString, protocol);
        CompletableFuture<Void> futureOpen = serviceClient.openAsync();
        futureOpen.get();

        Message message = new Message(content.getBytes());

        CompletableFuture<Void> completableFuture = serviceClient.sendAsync(deviceId, message);
        completableFuture.get();

        Device deviceGetAfter = registryManager.getDevice(deviceId);
        CompletableFuture<Void> futureClose = serviceClient.closeAsync();
        futureClose.get();

        registryManager.removeDevice(deviceId);

        // Assert
        assertEquals(deviceGetBefore.getDeviceId(), deviceGetAfter.getDeviceId());
        assertEquals(0, deviceGetBefore.getCloudToDeviceMessageCount());
        assertEquals(1, deviceGetAfter.getCloudToDeviceMessageCount());

        registryManager.close();
    }

    @Test
    public void serviceclientValidatesRemoteCertificate() throws IOException
    {
        String invalidCertificateServerConnectionString = retrieveEnvironmentVariableValue(IOT_HUB_CONNECTION_STRING_ENV_VAR_NAME);
        
        ServiceClient serviceClient = ServiceClient.createFromConnectionString(invalidCertificateServerConnectionString, IotHubServiceClientProtocol.AMQPS);

        try
        {
            serviceClient.open();
            serviceClient.send("11", new Message("adsf"));
        }
        catch (IotHubException e)
        {
            e.printStackTrace();
        }
    }
}
