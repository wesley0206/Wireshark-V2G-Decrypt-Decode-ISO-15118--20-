/*******************************************************************************
 *  The MIT License (MIT)
 *
 *  Copyright (c) 2015 - 2019  Dr. Marc Mültin (V2G Clarity)
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 *******************************************************************************/
package com.v2gclarity;

import com.v2gclarity.risev2g.shared.enumerations.GlobalValues;
import com.v2gclarity.risev2g.shared.misc.V2GTPMessage;
import com.v2gclarity.risev2g.shared.utils.ByteUtils;
import com.v2gclarity.risev2g.shared.v2gMessages.SECCDiscoveryReq;
import com.v2gclarity.risev2g.shared.v2gMessages.SECCDiscoveryRes;

import java.util.Arrays;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

//V2GCommunicationSessionSECC or EVCC
public class processmessage {
    private V2GTPMessage v2gtpmessage;
    private Logger logger = LogManager.getLogger(this.getClass().getSimpleName()); 
    private decodeHexString dh = new decodeHexString();

	public void processSDPMessage(String incomingMessage) {
		setV2gTpMessage(new V2GTPMessage(ByteUtils.toByteArrayFromHexString(incomingMessage)));
		
		//Test
		try {
			if (isV2GTPMessageValid(getV2gTpMessage())) {
			    if(Arrays.equals(getV2gTpMessage().getPayloadType(),
					GlobalValues.V2GTP_PAYLOAD_TYPE_SDP_REQUEST_MESSAGE.getByteArrayValue())) {
						SECCDiscoveryReq seccDiscoveryReq = new SECCDiscoveryReq(getV2gTpMessage().getPayload());
						getLogger().debug("Processing SECCDiscoveryReq");
						//Decode Security byte
						byte securitySetting = seccDiscoveryReq.getSecurity();
						if(Byte.compare(securitySetting, (byte) 0x00) == 0) {
							getLogger().info("SECCDiscoveryReq security setting: secured with TLS");
						}
						else if(Byte.compare(securitySetting, (byte) 0x10) == 0) {
							getLogger().info("SECCDiscoveryReq security setting: no transport layer security");
						}
						else getLogger().error("SECCDiscoveryReq security setting to reserved value");
						//Decode Transport Protocol byte
						byte transportProtocol = seccDiscoveryReq.getTransportProtocol();
						if(Byte.compare(transportProtocol, (byte) 0x00) == 0) {
							getLogger().info("SECCDiscoveryReq transport protocol setting: TCP\n");
						}
						else if(Byte.compare(transportProtocol, (byte) 0x10) == 0) {
							getLogger().info("SECCDiscoveryReq transport protocol setting: UDP\n");
						}
						else getLogger().error("SECCDiscoveryReq transport protocol setting to reserved value\n");
				}
				else if(Arrays.equals(getV2gTpMessage().getPayloadType(),
					GlobalValues.V2GTP_PAYLOAD_TYPE_SDP_RESPONSE_MESSAGE.getByteArrayValue())) {
						SECCDiscoveryRes seccDiscoveryRes = new SECCDiscoveryRes(getV2gTpMessage().getPayload());
						getLogger().debug("Processing SECCDiscoveryRes");
						//Decode SECC IP Address
						getLogger().info("SECC IP Address: " + ByteUtils.toHexString(seccDiscoveryRes.getSeccIPAddress()).toLowerCase());
						//Decode SECC port number
						getLogger().info("SECC port number: " + ByteUtils.toIntFromByteArray(seccDiscoveryRes.getSeccPort()));
						//Decode Security byte
						byte securitySetting = seccDiscoveryRes.getSecurity();
						if(Byte.compare(securitySetting, (byte) 0x00) == 0) {
							getLogger().info("SECCDiscoveryRes security setting: secured with TLS");
						}
						else if(Byte.compare(securitySetting, (byte) 0x10) == 0) {
							getLogger().info("SECCDiscoveryRes security setting: no transport layer security");
						}
						else getLogger().error("SECCDiscoveryRes security setting to reserved value");
						//Decode Transport Protocol byte
						byte transportProtocol = seccDiscoveryRes.getTransportProtocol();
						if(Byte.compare(transportProtocol, (byte) 0x00) == 0) {
							getLogger().info("SECCDiscoveryRes transport protocol setting: TCP\n");
						}
						else if(Byte.compare(transportProtocol, (byte) 0x10) == 0) {
							getLogger().info("SECCDiscoveryRes transport protocol setting: UDP\n");
						}
						else getLogger().error("SECCDiscoveryRes transport protocol setting to reserved value\n");
				}
				else {
					getLogger().error("UDP DatagramPacket could not be identified\n");
				}
				

			} else {
				getLogger().warn("Incoming UDP DatagramPacket could not be identified as a V2GTP message");
			}
		} catch (NullPointerException e) {
			getLogger().error("NullPointerException occurred while processing UDP DatagramPacket", e);
		}
		System.out.println();
	}

    public void processIncomingMessage(Object incomingMessage) {
		setV2gTpMessage(new V2GTPMessage((byte[]) incomingMessage)); 
		
		if (isV2GTPMessageValid(getV2gTpMessage()) &&
			Arrays.equals(getV2gTpMessage().getPayloadType(), GlobalValues.ISO_15118_2_V2GTP_PAYLOAD_TYPE_EXI_ENCODED_V2G_MESSAGE.getByteArrayValue())) {

            getLogger().info("Processing SupportedAppProtocol");
            dh.exiToSuppAppProtocolMsg(getV2gTpMessage().getPayload());

		} 
		else if (isV2GTPMessageValid(getV2gTpMessage())){
			getLogger().info("Processing V2GTPMessage");
			dh.exiToV2gMsgV20(getV2gTpMessage().getPayload());
		}
	}

    public synchronized boolean isV2GTPMessageValid(V2GTPMessage v2gTpMessage) {
		if (isVersionAndInversionFieldCorrect(v2gTpMessage) && 
			isPayloadTypeCorrect(v2gTpMessage) && 
			isPayloadLengthCorrect(v2gTpMessage)) 
			return true;
		return false;
	}
	
	public synchronized boolean isVersionAndInversionFieldCorrect(V2GTPMessage v2gTpMessage) {
		if (v2gTpMessage.getProtocolVersion() != GlobalValues.V2GTP_VERSION_1_IS.getByteValue()) {
			getLogger().error("Protocol version (" + ByteUtils.toStringFromByte(v2gTpMessage.getProtocolVersion()) + 
							  ") is not supported!");
			return false;
		}
		
		if (v2gTpMessage.getInverseProtocolVersion() != (byte) (v2gTpMessage.getProtocolVersion() ^ 0xFF)) {
			getLogger().error("Inverse protocol version (" + ByteUtils.toStringFromByte(v2gTpMessage.getInverseProtocolVersion()) + 
							  ") does not match the inverse value of the protocol version (" + v2gTpMessage.getProtocolVersion() + ")!");
			return false;
		}
		
		//for debug
		//System.out.println("Protocol Version: " + ByteUtils.toStringFromByte(v2gTpMessage.getProtocolVersion()));
		//System.out.println("Inverse protocol Version: " + ByteUtils.toStringFromByte(v2gTpMessage.getInverseProtocolVersion()));
		
		return true;
	}
	
	public synchronized boolean isPayloadTypeCorrect(V2GTPMessage v2gTpMessage) {
		byte[] payloadType = v2gTpMessage.getPayloadType();

		if (Arrays.equals(payloadType, GlobalValues.ISO_15118_2_V2GTP_PAYLOAD_TYPE_EXI_ENCODED_V2G_MESSAGE.getByteArrayValue()) ||
			Arrays.equals(payloadType, GlobalValues.ISO_15118_20_V2GTP_PAYLOAD_TYPE_EXI_ENCODED_V2G_MESSAGE.getByteArrayValue()) ||
			Arrays.equals(payloadType, GlobalValues.ISO_15118_20_AC_V2GTP_PAYLOAD_TYPE_EXI_ENCODED_V2G_MESSAGE.getByteArrayValue())||
			Arrays.equals(payloadType, GlobalValues.ISO_15118_20_DC_V2GTP_PAYLOAD_TYPE_EXI_ENCODED_V2G_MESSAGE.getByteArrayValue())||
			Arrays.equals(payloadType, GlobalValues.V2GTP_PAYLOAD_TYPE_SDP_REQUEST_MESSAGE.getByteArrayValue())||
			Arrays.equals(payloadType, GlobalValues.V2GTP_PAYLOAD_TYPE_SDP_RESPONSE_MESSAGE.getByteArrayValue())) return true;
		
		getLogger().error("Payload type not supported! Proposed payload type: " + ByteUtils.toStringFromByteArray(v2gTpMessage.getPayloadType()));
		
		return false;
	}
	
	public synchronized boolean isPayloadLengthCorrect(V2GTPMessage v2gTpMessage) {
		if (ByteUtils.toLongFromByteArray(v2gTpMessage.getPayloadLength()) > GlobalValues.V2GTP_HEADER_MAX_PAYLOAD_LENGTH.getLongValue() ||
			ByteUtils.toLongFromByteArray(v2gTpMessage.getPayloadLength()) < 0L) {
			getLogger().error("Payload length (" + ByteUtils.toLongFromByteArray(v2gTpMessage.getPayloadLength()) + 
							  " bytes) not supported! Must be between 0 and " + 
							  GlobalValues.V2GTP_HEADER_MAX_PAYLOAD_LENGTH.getLongValue() + " bytes");
			return false;
		}
		
		int payLoadLengthField = ByteUtils.toIntFromByteArray(v2gTpMessage.getPayloadLength());
		if (v2gTpMessage.getPayload().length != payLoadLengthField) {
			getLogger().error("Length of payload (" + v2gTpMessage.getPayload().length + " bytes) does not match value of " +
							  "field payloadLength (" + payLoadLengthField + " bytes)");
			return false;
		}
		
		return true;
	}
 
    public Logger getLogger() {
		return logger;
	}
    public V2GTPMessage getV2gTpMessage() {
        return v2gtpmessage;
    }
    public void setV2gTpMessage(V2GTPMessage v2gtpmessage) {
		this.v2gtpmessage = v2gtpmessage;
    }
}
