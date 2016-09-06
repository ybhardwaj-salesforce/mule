/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.el.context;

import java.io.Serializable;
import java.util.Map;

import javax.activation.DataHandler;

import org.mule.runtime.api.message.Attributes;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.message.Correlation;

/**
 * Exposes information about the current Mule message:
 *
 * <li> <b>correlationId</b>         <i>The message correlationId.</i>
 * <li> <b>correlationSequence</b>   <i>The message correlation sequence number.</i>
 * <li> <b>correlationGroupSize</b>  <i>The message correlation group size.</i>
 * <li><b>dataType</b> <i>The message data type (org.mule.runtime.core.api.transformer.DataType).</i>
 * <li><b>replyTo</b> <i>The message reply to destination. (mutable)</i>
 * <li><b>payload</b> <i>The message payload (mutable). You can also use message.payloadAs(Class clazz). Note: If the message
 * payload is NullPayload, this method will return null (from 3.4)</i>
 * <li><b>inboundProperties</b> <i>Map of inbound message properties (immutable).</i>
 * <li><b>outboundProperties</b> <i>Map of outbound message properties.</i>
 * <li><b>inboundAttachements</b> <i>Map of inbound message attachments (immutable).</i>
 * <li><b>outboundAttachements</b> <i>Map of outbound message attachments.</i>
 */
public class MessageContext {

  private MuleEvent event;
  private MuleEvent.Builder eventBuilder;
  private MuleContext muleContext;

  public MessageContext(MuleEvent event, MuleEvent.Builder eventBuilder, MuleContext muleContext) {
    this.event = event;
    this.eventBuilder = eventBuilder;
    this.muleContext = muleContext;
  }

  public String getCorrelationId() {
    return event.getCorrelationId();
  }

  public int getCorrelationSequence() {
    return event.getCorrelation().getSequence().orElse(-1);
  }

  public int getCorrelationGroupSize() {
    return event.getCorrelation().getSequence().orElse(-1);
  }

  public DataType getDataType() {
    return event.getMessage().getDataType();
  }

  public Object getPayload() {
    return event.getMessage().getPayload();
  }

  /**
   * Obtains the payload of the current message transformed to the given #type.
   *
   * @param type the java type the payload is to be transformed to
   * @return the transformed payload
   * @throws TransformerException
   */
  public <T> T payloadAs(Class<T> type) throws TransformerException {
    eventBuilder.message(muleContext.getTransformationService().transform(event.getMessage(), DataType.fromType(type)));
    event = eventBuilder.build();
    return (T) event.getMessage().getPayload();
  }

  /**
   * Obtains the payload of the current message transformed to the given #dataType.
   *
   * @param dataType the DatType to transform the current message payload to
   * @return the transformed payload
   * @throws TransformerException if there is an error during transformation
   */
  public Object payloadAs(DataType dataType) throws TransformerException {
    eventBuilder.message(muleContext.getTransformationService().transform(event.getMessage(), dataType));
    event = eventBuilder.build();
    return event.getMessage().getPayload();
  }

  public void setPayload(Object payload) {
    eventBuilder.message(MuleMessage.builder(event.getMessage()).payload(payload).build());
    event = eventBuilder.build();

  }

  public Map<String, Serializable> getInboundProperties() {
    return new InboundPropertiesMapContext(event);
  }

  public Map<String, Serializable> getOutboundProperties() {
    return new OutboundPropertiesMapContext(event, eventBuilder);
  }

  public Map<String, DataHandler> getInboundAttachments() {
    return new InboundAttachmentMapContext(event);
  }

  public Map<String, DataHandler> getOutboundAttachments() {
    return new OutboundAttachmentMapContext(event, eventBuilder);
  }

  public Attributes getAttributes() {
    return event.getMessage().getAttributes();
  }

  @Override
  public String toString() {
    return event.getMessage().toString();
  }
}
