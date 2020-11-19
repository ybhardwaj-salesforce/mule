/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.rx;

import static java.lang.Boolean.getBoolean;
import static org.mule.runtime.api.util.MuleSystemProperties.MULE_PRINT_STACK_TRACES_ON_DROP;
import static org.slf4j.LoggerFactory.getLogger;
import static reactor.core.publisher.Flux.create;
import static reactor.util.context.Context.empty;

import org.mule.runtime.core.internal.exception.MessagingException;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.slf4j.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

/**
 * Utility class for using with {@link Flux#create(Consumer)}.
 *
 * @param <T> The type of values in the flux
 */
public class FluxSinkRecorder<T> implements Consumer<FluxSink<T>> {

  private volatile FluxSink<T> fluxSink;

  // If a fluxSink as not yet been accepted, events are buffered until one is accepted
  private final List<Runnable> bufferedEvents = new ArrayList<Runnable>();
  private static final Logger LOGGER = getLogger(FluxSinkRecorder.class);

  private static final boolean PRINT_STACK_TRACES_ON_DROP = getBoolean(MULE_PRINT_STACK_TRACES_ON_DROP);
  private volatile String completionStackTrace = null;
  private volatile String acceptStackTrace = null;

  public Flux<T> flux() {
    return create(this)
        .subscriberContext(ctx -> empty());
  }

  @Override
  public void accept(FluxSink<T> fluxSink) {
    synchronized (this) {
      if (PRINT_STACK_TRACES_ON_DROP) {
        acceptStackTrace = getStackTraceAsString();
      }
      this.fluxSink = fluxSink;
      bufferedEvents.forEach(Runnable::run);
    }
  }

  public synchronized FluxSink<T> getFluxSink() {
    return fluxSink;
  }

  public void next(T response) {
    boolean present = true;
    synchronized (this) {
      if (PRINT_STACK_TRACES_ON_DROP && completionStackTrace != null) {
        LOGGER.warn("Event will be dropped {}\nCompletion StackTrace:\n{}\nAccept StackTrace:\n{}", response,
                    completionStackTrace, acceptStackTrace);
      }
      if (fluxSink == null) {
        present = false;
        bufferedEvents.add(() -> fluxSink.next(response));
      }
    }

    if (present) {
      fluxSink.next(response);
    }
  }

  public void error(MessagingException error) {
    boolean present = true;
    synchronized (this) {
      if (PRINT_STACK_TRACES_ON_DROP) {
        completionStackTrace = getStackTraceAsString();
      }
      if (fluxSink == null) {
        present = false;
        bufferedEvents.add(() -> fluxSink.error(error));
      }
    }

    if (present) {
      fluxSink.error(error);
    }
  }

  public void complete() {
    boolean present = true;
    synchronized (this) {
      if (PRINT_STACK_TRACES_ON_DROP) {
        completionStackTrace = getStackTraceAsString();
      }
      if (fluxSink == null) {
        present = false;
        bufferedEvents.add(() -> fluxSink.complete());
      }
    }

    if (present) {
      fluxSink.complete();
    }
  }

  private String getStackTraceAsString() {
    StringBuilder sb = new StringBuilder();
    StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
    for (StackTraceElement element : stackTrace) {
      sb.append('\t').append(element).append('\n');
    }
    return sb.toString();
  }
}
