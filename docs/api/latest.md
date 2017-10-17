# API Docs - v4.0.3

## Priority

### time *<a target="_blank" href="https://wso2.github.io/siddhi/documentation/siddhi-4.0/#stream-processor">(Stream Processor)</a>*

<p style="word-wrap: break-word">The PriorityStreamProcessor keeps track of the priority of events in a stream. When an event with new unique key arrives, PriorityStreamProcessor checks the priority and if the priority is 0 the event will be sent out without being stored internally. If the event has a priority greater than 0, it will be stored in the stream processor and the current priority will be injected into that event.  When an event with existing priority key arrives, it will be stored as the recent event and the priority will be increased by the priority of the received event, and the priorityKey and  currentPriority will be injected into the event. After every given timeout, priority of every events will be reduced by 1 and the updated priority will be sent out with the last known attributes of those events. It will continue until their priority reduced to 0.</p>

<span id="syntax" class="md-typeset" style="display: block; font-weight: bold;">Syntax</span>
```
priority:time(<STRING|DOUBLE|FLOAT|INT|LONG|OBJECT> unique.key, <INT|LONG> priority, <INT|LONG> timeout.constant)
```

<span id="query-parameters" class="md-typeset" style="display: block; color: rgba(0, 0, 0, 0.54); font-size: 12.8px; font-weight: bold;">QUERY PARAMETERS</span>
<table>
    <tr>
        <th>Name</th>
        <th style="min-width: 20em">Description</th>
        <th>Default Value</th>
        <th>Possible Data Types</th>
        <th>Optional</th>
        <th>Dynamic</th>
    </tr>
    <tr>
        <td style="vertical-align: top">unique.key</td>
        <td style="vertical-align: top; word-wrap: break-word">The unique key variable to identify the event.</td>
        <td style="vertical-align: top"></td>
        <td style="vertical-align: top">STRING<br>DOUBLE<br>FLOAT<br>INT<br>LONG<br>OBJECT</td>
        <td style="vertical-align: top">No</td>
        <td style="vertical-align: top">No</td>
    </tr>
    <tr>
        <td style="vertical-align: top">priority</td>
        <td style="vertical-align: top; word-wrap: break-word">Variable that contains the priority increment.</td>
        <td style="vertical-align: top"></td>
        <td style="vertical-align: top">INT<br>LONG</td>
        <td style="vertical-align: top">No</td>
        <td style="vertical-align: top">No</td>
    </tr>
    <tr>
        <td style="vertical-align: top">timeout.constant</td>
        <td style="vertical-align: top; word-wrap: break-word">The constant value to decrease the priority by one after the given timeout.</td>
        <td style="vertical-align: top"></td>
        <td style="vertical-align: top">INT<br>LONG</td>
        <td style="vertical-align: top">No</td>
        <td style="vertical-align: top">No</td>
    </tr>
</table>

<span id="examples" class="md-typeset" style="display: block; font-weight: bold;">Examples</span>
<span id="example-1" class="md-typeset" style="display: block; color: rgba(0, 0, 0, 0.54); font-size: 12.8px; font-weight: bold;">EXAMPLE 1</span>
```
time(symbol, priority, 1 sec)
```
<p style="word-wrap: break-word">This keeps track of the priority of events in a stream and injects the priority key and the current priority to the output event.</p>

