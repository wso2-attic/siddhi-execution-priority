# WSO2 Extension Siddhi Execution Priority



 PriorityStreamProcessor keeps track of the priority of events in a stream. This stream processor requires three arguments which are:
 
 - Unique key variable to identify the event
 - Priority variable which contains the priority increment
 - Timeout in constant to decrease the priority by one after the given timeout
 

 PriorityStreamProcessor injects two new attributes into the stream which are
 
 - priorityKey 
 - currentPriority
  

When an event with new unique key arrives, PriorityStreamProcessor checks the priority and if the priority is 0 the event will be sent out without being stored internally. 
If the event has a priority greater than 0, it will be stored in the stream processor and the current priority will be injected into that event.

When an event with existing priority key arrives, it will be stored as the recent event and the priority will be increased by the priority of the received event, and the priorityKey and currentPriority will be injected into the event.

After every given timeout, priority of every events will be reduced by 1 and the updated priority, will be sent out with the last known attributes of those events. It will continue until their priority reduced to 0.

When an event with existing id and a large negative priority, the output will be 0 not a negative priority.

## API Docs:

1. <a href="./api/4.0.2-SNAPSHOT">4.0.2-SNAPSHOT</a>
