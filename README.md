siddhi-execution-priority
======================================

The **siddhi-execution-priority extension** is an extension to <a target="_blank" href="https://wso2.github.io/siddhi">Siddhi</a> that keeps track of the priority of events in a stream.

Find some useful links below:

* <a target="_blank" href="https://github.com/wso2-extensions/siddhi-execution-priority">Source code</a>
* <a target="_blank" href="https://github.com/wso2-extensions/siddhi-execution-priority/releases">Releases</a>
* <a target="_blank" href="https://github.com/wso2-extensions/siddhi-execution-priority/issues">Issue tracker</a>

## Latest API Docs 

Latest API Docs is <a target="_blank" href="https://wso2-extensions.github.io/siddhi-execution-priority/api/5.0.1">5.0.1</a>.

## How to use 

**Using the extension in <a target="_blank" href="https://github.com/wso2/product-sp">WSO2 Stream Processor</a>**

* You can use this extension in the latest <a target="_blank" href="https://github.com/wso2/product-sp/releases">WSO2 Stream Processor</a> that is a part of <a target="_blank" href="http://wso2.com/analytics?utm_source=gitanalytics&utm_campaign=gitanalytics_Jul17">WSO2 Analytics</a> offering, with editor, debugger and simulation support. 

* This extension is shipped by default with WSO2 Stream Processor, if you wish to use an alternative version of this extension you can replace the component <a target="_blank" href="https://github.com/wso2-extensions/siddhi-execution-priority/releases">jar</a> that can be found in the `<STREAM_PROCESSOR_HOME>/lib` directory.

**Using the extension as a <a target="_blank" href="https://wso2.github.io/siddhi/documentation/running-as-a-java-library">java library</a>**

* This extension can be added as a maven dependency along with other Siddhi dependencies to your project.

```
     <dependency>
        <groupId>org.wso2.extension.siddhi.execution.priority</groupId>
        <artifactId>siddhi-execution-priority</artifactId>
        <version>x.x.x</version>
     </dependency>
```

## Jenkins Build Status

---

|  Branch | Build Status |
| :------ |:------------ | 
| master  | [![Build Status](https://wso2.org/jenkins/job/siddhi/job/siddhi-execution-priority/badge/icon)](https://wso2.org/jenkins/job/siddhi/job/siddhi-execution-priority/) |

---

## Features

* <a target="_blank" href="https://wso2-extensions.github.io/siddhi-execution-priority/api/5.0.1/#time-stream-processor">time</a> *(<a target="_blank" href="http://siddhi.io/en/v5.1/docs/query-guide/#stream-processor">Stream Processor</a>)*<br> <div style="padding-left: 1em;"><p>The PriorityStreamProcessor keeps track of the priority of events in a stream. When an event with a new unique key arrives, PriorityStreamProcessor checks the priority and if the priority is 0 the event is sent out without being stored internally. If the event has a priority greater than 0, it is stored in the stream processor and the current priority is injected into that event.  When an event with an existing priority key arrives, it is stored as the recent event and the priority is increased by the priority of the received event, and the priorityKey and the  currentPriority is injected into the event. After every given timeout, priority of every event is reduced by 1 and the updated priority is sent out with the last known attributes of those events. It continues until their priorities reduce to 0.</p></div>

## How to Contribute
 
  * Please report issues at <a target="_blank" href="https://github.com/wso2-extensions/siddhi-execution-priority/issues">GitHub Issue Tracker</a>.
  
  * Send your contributions as pull requests to <a target="_blank" href="https://github.com/wso2-extensions/siddhi-execution-priority/tree/master">master branch</a>. 
 
## Contact us 

 * Post your questions with the <a target="_blank" href="http://stackoverflow.com/search?q=siddhi">"Siddhi"</a> tag in <a target="_blank" href="http://stackoverflow.com/search?q=siddhi">Stackoverflow</a>. 
 
 * Siddhi developers can be contacted via the mailing lists:
 
    Developers List   : [dev@wso2.org](mailto:dev@wso2.org)
    
    Architecture List : [architecture@wso2.org](mailto:architecture@wso2.org)
 
## Support 

* We are committed to ensuring support for this extension in production. Our unique approach ensures that all support leverages our open development methodology and is provided by the very same engineers who build the technology. 

* For more details and to take advantage of this unique opportunity contact us via <a target="_blank" href="http://wso2.com/support?utm_source=gitanalytics&utm_campaign=gitanalytics_Jul17">http://wso2.com/support/</a>. 

