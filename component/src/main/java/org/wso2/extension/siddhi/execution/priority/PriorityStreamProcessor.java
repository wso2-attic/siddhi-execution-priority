/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.extension.siddhi.execution.priority;

import io.siddhi.annotation.Example;
import io.siddhi.annotation.Extension;
import io.siddhi.annotation.Parameter;
import io.siddhi.annotation.ReturnAttribute;
import io.siddhi.annotation.util.DataType;
import io.siddhi.core.config.SiddhiAppContext;
import io.siddhi.core.config.SiddhiQueryContext;
import io.siddhi.core.event.ComplexEventChunk;
import io.siddhi.core.event.stream.MetaStreamEvent;
import io.siddhi.core.event.stream.StreamEvent;
import io.siddhi.core.event.stream.StreamEventCloner;
import io.siddhi.core.event.stream.holder.StreamEventClonerHolder;
import io.siddhi.core.event.stream.populater.ComplexEventPopulater;
import io.siddhi.core.executor.ConstantExpressionExecutor;
import io.siddhi.core.executor.ExpressionExecutor;
import io.siddhi.core.executor.VariableExpressionExecutor;
import io.siddhi.core.query.processor.ProcessingMode;
import io.siddhi.core.query.processor.Processor;
import io.siddhi.core.query.processor.SchedulingProcessor;
import io.siddhi.core.query.processor.stream.StreamProcessor;
import io.siddhi.core.util.Scheduler;
import io.siddhi.core.util.config.ConfigReader;
import io.siddhi.core.util.snapshot.state.State;
import io.siddhi.core.util.snapshot.state.StateFactory;
import io.siddhi.query.api.definition.AbstractDefinition;
import io.siddhi.query.api.definition.Attribute;
import io.siddhi.query.api.exception.SiddhiAppValidationException;
import org.apache.log4j.Logger;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import static org.wso2.extension.siddhi.execution.priority.PriorityStreamProcessor.ATTRIBUTE_CURRENT_PRIORITY;
import static org.wso2.extension.siddhi.execution.priority.PriorityStreamProcessor.ATTRIBUTE_PRIORITY_KEY;

/**
 * PriorityStreamProcessor keeps track of the priority of events in a stream. This stream processor
 * requires three arguments which are:
 * - Unique key variable to identify the event
 * - Priority variable which contains the priority increment
 * - Timeout in constant to decrease the priority by one after the given timeout
 * <p>
 * PriorityStreamProcessor injects two new attributes into the stream which are
 * - priorityKey (see the constant {@link PriorityStreamProcessor#ATTRIBUTE_PRIORITY_KEY})
 * - currentPriority (see the constant {@link PriorityStreamProcessor#ATTRIBUTE_CURRENT_PRIORITY})
 * <p>
 * When an event with new unique key arrives, PriorityStreamProcessor checks the priority and if
 * the priority is 0 the event will be sent out without being stored internally. If the event has
 * a priority greater than 0, it will be stored in the stream processor and the current priority
 * will be injected into that event.
 * <p>
 * When an event with existing priority key arrives, it will be stored as the recent event and the
 * priority will be increased by the priority of the received event, and the priorityKey and
 * currentPriority will be injected into the event.
 * <p>
 * After every given timeout, priority of every events will be reduced by 1 and the updated priority,
 * will be sent out with the last known attributes of those events. It will continue until their
 * priority reduced to 0.
 * <p>
 * When an event with existing id and a large negative priority, the output will be 0 not a negative priority.
 */

@Extension(
        name = "time",
        namespace = "priority",
        description = "The PriorityStreamProcessor keeps track of the priority of events in a stream."
                       + " When an event with a new unique key arrives, PriorityStreamProcessor checks the priority"
                       + " and if the priority is 0 the event is sent out without being stored internally."
                       + " If the event has a priority greater than 0, it is stored in the stream processor"
                       + " and the current priority is injected into that event. "
                       + " When an event with an existing priority key arrives, it is stored as"
                       + " the recent event and the priority is increased by the priority of the received event,"
                       + " and the priorityKey and the "
                       + " currentPriority is injected into the event. After every given timeout,"
                       + " priority of every event is reduced by 1 and the updated priority"
                       + " is sent out with the last known attributes of those events."
                       + " It continues until their"
                       + " priorities reduce to 0."
        ,
        parameters = {
                @Parameter(name = "unique.key",
                        description = "The unique key variable to identify the event.",
                        type = {DataType.STRING, DataType.DOUBLE, DataType.FLOAT, DataType.INT, DataType.LONG,
                                DataType.OBJECT}),
                @Parameter(name = "priority",
                        description = "The variable that contains the priority increment.",
                        type = {DataType.INT, DataType.LONG}),
                @Parameter(name = "timeout.constant",
                        description = "The constant value to decrease the priority by one, after the given timeout.",
                        type = {DataType.INT, DataType.LONG})

        },
        returnAttributes = {
                @ReturnAttribute(
                        name = ATTRIBUTE_PRIORITY_KEY,
                        description = "The key for which the priority is calculated.",
                        type = {DataType.STRING}
                ),
                @ReturnAttribute(
                        name = ATTRIBUTE_CURRENT_PRIORITY,
                        description = "The current priority associated to the given key",
                        type = {DataType.STRING}
                )
        },
        examples = @Example(description = "This keeps track of the priority of events in a stream and injects " +
                "the priority key and the current priority to the output event.",
                syntax = "time(symbol, priority, 1 sec)")
)
public class PriorityStreamProcessor extends StreamProcessor<PriorityStreamProcessor.ExtensionState>
        implements SchedulingProcessor {

    /**
     * Logger to log events of the class
     */
    private static final Logger log = Logger.getLogger(PriorityStreamProcessor.class);

    /**
     * First attribute name injected by PriorityStreamProcessor into the output stream event.
     */
    public static final String ATTRIBUTE_PRIORITY_KEY = "priorityKey";

    /**
     * Second attribute name injected by PriorityStreamProcessor into the output stream event.
     */
    public static final String ATTRIBUTE_CURRENT_PRIORITY = "currentPriority";

    /**
     * Time to wait for a new event arrival before reducing the priority.
     */
    private long timeInMilliSeconds;

    /**
     * Scheduler used to trigger events for every timeInMilliSeconds.
     */
    private Scheduler scheduler;

    /**
     * SiddhiAppContext of Siddhi.
     */
    private SiddhiAppContext siddhiAppContext;

    /**
     * Timestamp of the last event received by PriorityStreamProcessor.
     */
    private volatile long lastTimestamp = Long.MIN_VALUE;

    /**
     * VariableExpressionExecutor of the unique event key variable in the event attributes.
     */
    private VariableExpressionExecutor keyExpressionExecutor;

    /**
     * VariableExpressionExecutor of the priority variable in the event attributes.
     */
    private VariableExpressionExecutor priorityExpressionExecutor;

    /**
     * A queue to maintain the events in the arrival order.
     * It provides circular buffer implementation.
     */
    private final Queue<Object> keyBuffer = new ArrayDeque<Object>();

    private List<Attribute> attributes = new ArrayList<Attribute>();

    /**
     * Initialize the PriorityStreamProcessor.
     *
     * @param inputDefinition              Input Definition
     * @param attributeExpressionExecutors Array of AttributeExpressionExecutor
     * @param siddhiQueryContext           siddhiQueryContext of Siddhi
     * @return list of new attributes injected by PriorityStreamProcessor
     */
    @Override
    protected StateFactory<ExtensionState> init(MetaStreamEvent metaStreamEvent, AbstractDefinition inputDefinition,
                                                ExpressionExecutor[] attributeExpressionExecutors,
                                                ConfigReader configReader,
                                                StreamEventClonerHolder streamEventClonerHolder,
                                                boolean outputExpectsExpiredEvents, boolean findToBeExecuted,
                                                SiddhiQueryContext siddhiQueryContext) {
        this.siddhiAppContext = siddhiQueryContext.getSiddhiAppContext();

        // PriorityStreamProcessor requires 3 parameters: uniqueKey, priority, time
        if (attributeExpressionExecutors.length == 3) {

            // First parameter: uniqueKey. Must be a variable of any type
            if (attributeExpressionExecutors[0] instanceof VariableExpressionExecutor) {
                this.keyExpressionExecutor = (VariableExpressionExecutor) attributeExpressionExecutors[0];
            } else {
                throw new UnsupportedOperationException("First parameter of priority stream processor must be a " +
                        "variable but found " + attributeExpressionExecutors[0].getClass().getCanonicalName());
            }

            // Second parameter: priority. Must be a variable of INT or LONG
            if (attributeExpressionExecutors[1] instanceof VariableExpressionExecutor) {
                Attribute.Type type = attributeExpressionExecutors[1].getReturnType();
                if (type != Attribute.Type.INT && type != Attribute.Type.LONG) {
                    throw new SiddhiAppValidationException("Second parameter of priority stream processor should be " +
                            "either int or long, but found " + type);
                }
                this.priorityExpressionExecutor = (VariableExpressionExecutor) attributeExpressionExecutors[1];
            } else {
                throw new UnsupportedOperationException("Second parameter of priority stream processor must be a " +
                        "variable but found " + attributeExpressionExecutors[1].getClass().getCanonicalName());
            }

            // Third parameter: time. Must be a constant of INT or LONG
            if (attributeExpressionExecutors[2] instanceof ConstantExpressionExecutor) {
                Attribute.Type type = attributeExpressionExecutors[2].getReturnType();
                if (type == Attribute.Type.INT) {
                    this.timeInMilliSeconds = (Integer) ((ConstantExpressionExecutor)
                            attributeExpressionExecutors[2]).getValue();
                } else if (type == Attribute.Type.LONG) {
                    this.timeInMilliSeconds = (Long) ((ConstantExpressionExecutor)
                            attributeExpressionExecutors[2]).getValue();
                } else {
                    throw new SiddhiAppValidationException("Third parameter of priority stream processor should be " +
                            "either int or long, but found " + type);
                }
            } else {
                throw new SiddhiAppValidationException("Third parameter of priority stream processor must be a " +
                        "variable but found " + attributeExpressionExecutors[2].getClass().getCanonicalName());
            }
        } else {
            // Invalid number of arguments
            throw new UnsupportedOperationException("Invalid number of arguments passed to priority stream fin. " +
                    "Required 3, but found " + attributeExpressionExecutors.length);
        }

        attributes.add(new Attribute(ATTRIBUTE_PRIORITY_KEY, keyExpressionExecutor.getAttribute().getType()));
        attributes.add(new Attribute(ATTRIBUTE_CURRENT_PRIORITY, priorityExpressionExecutor.getAttribute().getType()));

        return () -> new ExtensionState();

    }

    /**
     * Process events received by PriorityStreamProcessor.
     *
     * @param streamEventChunk      the event chunk that need to be processed
     * @param nextProcessor         the next processor to which the success events need to be passed
     * @param streamEventCloner     helps to clone the incoming event for local storage or modification
     * @param complexEventPopulater helps to populate the events with the resultant attributes
     */
    @Override
    protected void process(ComplexEventChunk<StreamEvent> streamEventChunk, Processor nextProcessor,
                           StreamEventCloner streamEventCloner, ComplexEventPopulater complexEventPopulater,
                           ExtensionState state) {
        synchronized (this) {

            while (streamEventChunk.hasNext()) {
                StreamEvent streamEvent = streamEventChunk.next();

                // Timer event sent by scheduler
                long currentTime = siddhiAppContext.getTimestampGenerator().currentTime();
                if (streamEvent.getType() == StreamEvent.Type.TIMER) {
                    while (!this.keyBuffer.isEmpty()) {
                        // Get the first the key from the queue
                        Object key = this.keyBuffer.peek();

                        // Get the event holder
                        EventHolder eventHolder = state.eventHolderMap.get(key);
                        long timeDiff = eventHolder.getTimestamp() + timeInMilliSeconds - currentTime;
                        if (timeDiff <= 0) {
                            // Remove the first key from the queue
                            final Object poll = this.keyBuffer.poll();
                            if (null == poll) {
                                if (log.isDebugEnabled()) {
                                    log.debug("The Queue is empty. There are no events to remove.");
                                }
                            }

                            // Decrease the priority
                            eventHolder.decreasePriority(currentTime);
                            if (eventHolder.isExpired()) {
                                state.eventHolderMap.remove(key);
                            } else {
                                // If not expired, add the key to the end of the queue
                                boolean isAdded = this.keyBuffer.offer(key);
                                if (!isAdded) {
                                    log.error("Task of adding '" + key + "' to the"
                                            + "queue couldn't be complete.");
                                }
                            }
                            streamEventChunk.insertBeforeCurrent(eventHolder.copyStreamEvent());
                        } else {
                            break;
                        }

                    }
                }

                if (streamEvent.getType() == StreamEvent.Type.CURRENT) {
                    // Current event received by the processor
                    StreamEvent clonedEvent = streamEventCloner.copyStreamEvent(streamEvent);

                    // Unique key of the event
                    Object key = this.keyExpressionExecutor.execute(streamEvent);
                    EventHolder eventHolder = state.eventHolderMap.get(key);

                    if (eventHolder == null) {
                        // Create a new event holder
                        eventHolder = new EventHolder(key, clonedEvent);

                        if (!eventHolder.isExpired()) {
                            // Event with a priority higher than 0
                            state.eventHolderMap.put(key, eventHolder);

                            // Add the key into the queue
                            boolean isAdded = this.keyBuffer.offer(key);
                            if (!isAdded) {
                                log.error("Task of adding '" + key + "' to the"
                                        + "queue couldn't be complete.");
                            }
                        }

                    } else {
                        // Set the recent event and increase the priority
                        eventHolder.setEvent(clonedEvent);
                        if (eventHolder.isExpired()) {
                            state.eventHolderMap.remove(key);
                            this.keyBuffer.remove(key);
                        }
                    }

                    // Add priorityKey and currentPriority to current stream
                    complexEventPopulater.populateComplexEvent(streamEvent, new Object[]{key,
                            eventHolder.getPriority()});

                }

                // Schedule the next alert if this is a new event and there are events in the event holder map
                if (lastTimestamp < currentTime && !state.eventHolderMap.isEmpty()) {
                    scheduler.notifyAt(currentTime + timeInMilliSeconds);
                    lastTimestamp = currentTime;
                }
            }
        }
        nextProcessor.process(streamEventChunk);
    }

    @Override
    public void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    @Override
    public Scheduler getScheduler() {
        return this.scheduler;
    }


    @Override
    public void start() {
        //Do nothing
    }

    @Override
    public void stop() {
        //Do nothing
    }

    @Override
    public List<Attribute> getReturnAttributes() {
        return attributes;
    }

    @Override
    public ProcessingMode getProcessingMode() {
        return ProcessingMode.BATCH;
    }

    class ExtensionState extends State {

        /**
         * Keep track of events with priority higher than zero.
         */
        private Map<Object, EventHolder> eventHolderMap = new HashMap<Object, EventHolder>();

        @Override
        public boolean canDestroy() {
            return false;
        }

        @Override
        public Map<String, Object> snapshot() {
            synchronized (PriorityStreamProcessor.this) {
                Map<String, Object> currentStateMap = new HashMap<String, Object>();
                currentStateMap.put("eventHolderMap", eventHolderMap);
                return currentStateMap;
            }
        }

        @Override
        public void restore(Map<String, Object> map) {
            synchronized (PriorityStreamProcessor.this) {
                eventHolderMap = (Map<Object, EventHolder>) map.get("eventHolderMap");
            }
        }
    }

    /**
     * This class holds events and performs utility operations on the stored event.
     */
    private class EventHolder {
        /**
         * Unique priority key of the event.
         */
        private Object key;

        /**
         * Current priority of the event.
         */
        private Object priority;

        /**
         * The latest event arrived with the given unique priority key.
         */
        private StreamEvent event;

        /**
         * Create an EventHolder instance.
         *
         * @param key   unique priority key
         * @param event Stream Event
         */
        public EventHolder(Object key, StreamEvent event) {
            if (key == null) {
                throw new IllegalArgumentException("Priority unique key '" +
                        keyExpressionExecutor.getAttribute().getName() + "' cannot be null");
            }
            this.priority = priorityExpressionExecutor.execute(event);
            if (this.priority == null) {
                throw new IllegalArgumentException("Priority value '" +
                        priorityExpressionExecutor.getAttribute().getName() + "' cannot be null, " +
                        "but event with priority key " + key + " contains a null priority");
            }
            this.key = key;
            this.event = event;
        }

        /**
         * Current priority of the event.
         *
         * @return the current priority
         */
        public Object getPriority() {
            return priority;
        }

        /**
         * Timestamp of the latest event.
         *
         * @return the timestamp in milliseconds
         */
        public long getTimestamp() {
            return event.getTimestamp();
        }

        /**
         * Decrease the priority of the event by 1 and update the timestamp.
         *
         * @param currentTime Current time in milliseconds
         */
        public void decreasePriority(long currentTime) {
            // PriorityStreamProcessor accepts only INT and LONG priority values.
            if (priority instanceof Integer) {
                priority = ((Integer) priority) - 1;
            } else {
                priority = ((Long) priority) - 1;
            }
            this.event.setTimestamp(currentTime);
        }

        /**
         * If the current priority is 0, the event is expired.
         *
         * @return true if current priority is 0
         */
        public boolean isExpired() {
            boolean expired;
            // PriorityStreamProcessor accepts only INT and LONG priority values.
            if (priority instanceof Integer) {
                expired = ((Integer) priority).intValue() <= 0;
            } else {
                // According to the validation in init, type can be either INT or LONG
                expired = ((Long) priority).longValue() <= 0L;
            }
            return expired;
        }

        /**
         * Clone the underlying event, inject the priority key and current priority and return the cloned event.
         *
         * @return cloned event
         */
        public StreamEvent copyStreamEvent() {
            StreamEvent clonedEvent = streamEventClonerHolder.getStreamEventCloner().copyStreamEvent(event);
            complexEventPopulater.populateComplexEvent(clonedEvent, new Object[]{key, priority});
            clonedEvent.setTimestamp(siddhiAppContext.getTimestampGenerator().currentTime());
            return clonedEvent;
        }

        /**
         * Set the event and increase the priority by the the priority of the new event.
         *
         * @param event Stream Event
         */
        public void setEvent(StreamEvent event) {
            this.event = event;

            if (priority instanceof Integer) {
                priority = ((Integer) priority) + ((Integer) priorityExpressionExecutor.execute(event));
                if (((Integer) priority) < 0) {
                    priority = 0;
                }
            } else {
                // According to the validation in init, type can be either INT or LONG
                priority = ((Long) priority) + ((Long) priorityExpressionExecutor.execute(event));
                if (((Long) priority) < 0) {
                    priority = 0L;
                }
            }
        }
    }
}
