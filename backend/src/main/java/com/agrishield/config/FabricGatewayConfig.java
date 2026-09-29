package com.agrishield.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
@Slf4j
public class FabricGatewayConfig {

    @Value("${agrishield.fabric.enabled:false}")
    private boolean fabricEnabled;

    @Value("${agrishield.fabric.peer-endpoint:localhost:7051}")
    private String peerEndpoint;

    @Value("${agrishield.fabric.channel:farmchannel}")
    private String channelName;

    @Value("${agrishield.fabric.chaincode:mrl-amu-contract}")
    private String chaincodeName;

    @Value("${agrishield.fabric.msp-id:Org1MSP}")
    private String mspId;
}
