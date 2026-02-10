---
name: 🛠 Requirements
about: Use this to define a new function or feature.
title: '[REQ-###-###]: '
labels: enhancement
assignees: ''

---

## 1. Requirement Description
* **Goal:** The (server/client) must/shall ____
* **Server State:**  
- [ ] Startup
- [ ] Stopped
- [ ] Running
- [ ] Blocked (MC is in the process of booting up or closing)


## 2. Implementation Strategy
[Write briefly how you think this functionality should be implemented, in plain english]


## 3. Dependencies
> [!CAUTION]
> Starting this issue before the following requirements have been implemented will require code stubs to test:
1. [List any blocking requirements]


## 4. The Contract (Naming & Signature)
*The developer must implement the code exactly as named below to ensure system compatibility.*

* **Class Name(s):** `ManagerClassName`
* **Input Logic:** [What data is coming in?]
* **Output Logic:** [What data is coming out?]
  
**Proposed Signature(s):**
Java
```java
/**
 * @description [Brief description of the method's purpose]
 * @param type paramName - [Description]
 * @return ReturnType - [Description]
 */
public ReturnType methodName(ParameterType paramName) {
    // Implementation details
}
```
(This will have to be expanded to include typescript syntax)

## 5. Technical Logic Flow

*Define the step-by-step internal logic the programmer must follow:*
1. Validation:           [e.g., Check if input path is valid or if server is already running]
2. Core Process:         [e.g., Execute Java ProcessBuilder or Update internal State]
3. Data Transformation:  [e.g., Convert raw byte stream to UTF-8 String]
4. Conclusion:           [e.g., Return result or throw specific Exception]


## 6. Testing 

*These tests must be created and failing BEFORE writing the implementation:*

- [ ] Unit
- [ ] Integration
- [ ] System
- [ ] User Experience

- ex. [Positive] Given that the server is stopped, when the client sends a change startup parameter command, then the startup parameters should be updated.
- ex. [Exception] Given that the server is running, when the client sends a command to start the server, the command is ignored and a response is sent back.
- 


## 7. Definition of Done

* [ ] Naming: Method names and Class names match Section 3 exactly.
* [ ] Pre-requisites: All prerequisites are verified as merged in main.
* [ ] Logic: Implementation follows the steps in Section 4.
* [ ] Verification: Tests pass for all cases in Section 5 (for Unit/Integration).
* [ ] Documentation: Documentation block comments are present and accurate.
* [ ] Quality: Code passes the linter/checkstyle without warnings.
