/*
	Altitude Fan - Child

	Copyright 2023 Hubitat Inc.  All Rights Reserved

	2024-10-25 0.0.1 mavrrick
		-initial pub
    
    Built for Haiku H/I Series Fan from Big Ass Fans

*/

import groovy.transform.Field

@Field static final String   DEVICE_TYPE = 'MATTER_FAN'
@Field static final String  IMPORT_URL =  'https://raw.githubusercontent.com/Mavrrick/Matter_By_Mavrrick/refs/heads/main/Altitude%20Fan%20Matter/Mavrrick.AltitudeFanChild.groovy'


@Field Map getFanLevel = [
    "off": 0
    ,"speed 1": 1
	,"speed 2": 18
	,"speed 3": 34
    ,"speed 4": 51
	,"speed 5": 67
	,"speed 6": 84    
    ,"speed 7": 100    
]

@Field Map airDirection = [
    0: "forward"
    ,1: "reverse" 
] 

@Field Map whooshOptions = [
    0: "off"
    ,2: "on" 
]

import groovy.transform.Field

metadata {
    definition (name: "Altitude Fan - Child", namespace: "Mavrrick", author: "Mavrrick", importUrl: IMPORT_URL) {
        capability "Actuator"
        capability "Switch"        
        capability "Configuration"
        capability "FanControl"
        capability "SwitchLevel"
        capability "Initialize"
        capability "Refresh"
        
        command "setSpeed", [[name: "Fan speed*",type:"ENUM", description:"Fan speed to set", constraints: getFanLevel.collect {k,v -> k}]]
        command "airDirection", [[name: "Air Direction*",type:"ENUM", description:"Direction fan is moving", constraints: airDirection]]
        command "setLevel", [[name: "Speed %*",type:"INTEGER", description:"Percent of speed fan will run at"]]
        command "fanAuto"
        command "whoosh", [[name: "Whoosh Mode*",type:"ENUM", description:"Whoosh Mode on/off", constraints: whooshOptions]]
        
        attribute "Airflow_Direction", "string"
        attribute "Fan_Mode", "string"
        attribute "windMode", "string"
        
    }
    preferences {
        input(name:"cycleInterval", type:"number", title:"Number of seconds between cycles", defaultValue:30)
        input(name:"logEnable", type:"bool", title:"Enable debug logging", defaultValue:false)
        input(name:"txtEnable", type:"bool", title:"Enable descriptionText logging", defaultValue:true)
    }
}

//capability commands
void on() { 
    value = 4
    autoValue = intToHexStr(value)  
    if (logEnable) log.debug "Setting Fan mode to On"
    List<Map<String, String>> attributeWriteRequests = []
    attributeWriteRequests.add(matter.attributeWriteRequest(device.getDataValue("endpointId"), 0x0202, 0x0000, 0x04, autoValue ))
    attributeWriteRequests.add(matter.attributeWriteRequest(device.getDataValue("endpointId"), 0x0202, 0x0002, 0x04, state.previousSpeed ))
    String cmd = matter.writeAttributes(attributeWriteRequests)            
    parent.sendToDevice(cmd)
}

void off() { 
    value = 0
    autoValue = intToHexStr(value)  
    if (logEnable) log.debug "Setting Fan mode to Off"
    List<Map<String, String>> attributeWriteRequests = []
    attributeWriteRequests.add(matter.attributeWriteRequest(device.getDataValue("endpointId"), 0x0202, 0x0000, 0x04, autoValue ))
    String cmd = matter.writeAttributes(attributeWriteRequests)            
    parent.sendToDevice(cmd)
}

void setSpeed(fanspeed) {
    unschedule()
    if (logEnable) log.debug "Setting Fan Speed to ${fanspeed}"
    switch(fanspeed) {
        case "off":
            value = 0;
        break;
        case "speed 1":
        case "low":
            value = 1;
        break;
        case "speed 2":
            value = 18;
        break;
        case "speed 3":
        case "medium-low":
            value = 34;
        break;
        case "speed 4":
        case "medium":
            value = 51;
        break;
        case "speed 5":
        case "medium-high":
            value = 67;
        break;
        case "speed 6":
            value = 84;
        break;
        case "speed 7":
        case "high":
            value = 100;
        break; 
    }
    if (device.currentValue("Fan_Mode") == "Auto") {
//        value = 0
        autoValue = intToHexStr(0)
        speedValue = intToHexStr(value)
        if (logEnable) log.debug "Setting Fan mode from Auto to Off"
        List<Map<String, String>> attributeWriteRequests = []
        attributeWriteRequests.add(matter.attributeWriteRequest(device.getDataValue("endpointId"), 0x0202, 0x0000, 0x04, autoValue ))
        attributeWriteRequests.add(matter.attributeWriteRequest(device.getDataValue("endpointId"), 0x0202, 0x0002, 0x04, speedValue ))
        String cmd = matter.writeAttributes(attributeWriteRequests)            
        parent.sendToDevice(cmd)
    } else {
    speedValue = intToHexStr(value)  
    if (logEnable) log.debug "Setting Fan Speed percent ${fanspeed}  % ${value} value to ${speedValue}"
    List<Map<String, String>> attributeWriteRequests = []
    attributeWriteRequests.add(matter.attributeWriteRequest(device.getDataValue("endpointId"), 0x0202, 0x0002, 0x04, speedValue ))
    String cmd = matter.writeAttributes(attributeWriteRequests)            
    parent.sendToDevice(cmd)   
    }
}

void setLevel(Integer value, Object rate=0) { //new set level to assign fan speed
    if (logEnable) log.debug "setLevel(${value})"
    speedValue = intToHexStr(value)  
    if (logEnable) log.debug "Setting Fan Speed percento ${speedValue}"
    List<Map<String, String>> attributeWriteRequests = []
    attributeWriteRequests.add(matter.attributeWriteRequest(device.getDataValue("endpointId"), 0x0202, 0x0002, 0x04, speedValue ))
    String cmd = matter.writeAttributes(attributeWriteRequests)
    parent.sendToDevice(cmd)
}

void airDirection(direction) {
    if (logEnable) log.debug "Setting Fan direction to ${direction}"  
        switch(direction) {
        case "forward":
            value = 0;
        break;
        case "reverse":
            value = 1;
        break; 
    }
    dirValue = intToHexStr(value)  
    if (logEnable) log.debug "Setting Fan Speed direction to ${direction} ${value} value to ${dirValue}"
    List<Map<String, String>> attributeWriteRequests = []
    attributeWriteRequests.add(matter.attributeWriteRequest(device.getDataValue("endpointId"), 0x0202, 0x000B, 0x04, dirValue ))
    String cmd = matter.writeAttributes(attributeWriteRequests)            
    parent.sendToDevice(cmd)
}

void fanAuto() {
    value = 5
    autoValue = intToHexStr(value)  
    if (logEnable) log.debug "Setting Fan mode  to Auto"
    List<Map<String, String>> attributeWriteRequests = []
    attributeWriteRequests.add(matter.attributeWriteRequest(device.getDataValue("endpointId"), 0x0202, 0x0000, 0x04, autoValue ))
    String cmd = matter.writeAttributes(attributeWriteRequests)            
    parent.sendToDevice(cmd)
}

void whoosh(action) {
    if (logEnable) log.debug "Setting Whoosh to ${action}"  
        switch(action) {
        case "off":
            value = 0;
        break;
        case "on":
            value = 2;
        break; 
    }
//    value = 2
    whooshValue = intToHexStr(value)  
    if (logEnable) log.debug "Setting Fan Wind Setting for Woosh to ${action}"
    List<Map<String, String>> attributeWriteRequests = []
    attributeWriteRequests.add(matter.attributeWriteRequest(device.getDataValue("endpointId"), 0x0202, 0x000A, 0x04, whooshValue ))
    String cmd = matter.writeAttributes(attributeWriteRequests)            
    parent.sendToDevice(cmd)
}

void setPrevSpeed(String value) {
    if (logEnable) log.debug "Recieved Speed setting from parent ${value}"
    if (value != '00') {
        if (logEnable) log.debug "Speed recieved is greater then 0"
        if (device.currentValue("Fan_Mode") == "Auto") {
            if (logEnable) log.debug "Ignoring new speed, fan is in Auto Mode"    
        } else {
            if (logEnable) log.debug "Storing ${value} as previous speed"
            state.previousSpeed = value
        }
    }
}

void configure() {
    log.warn "configure..."
}

//lifecycle commands
void installed() {
    log.info "installed..."
    sendEvent(name: "supportedFanSpeeds", value: groovy.json.JsonOutput.toJson(getFanLevel.collect {k,v -> k})) 
    state.previousSpeed = 0x01
}

void updated(){
    log.info "updated..."
    log.warn "debug logging is: ${logEnable == true}"
    log.warn "description logging is: ${txtEnable == true}"
    if (logEnable) runIn(1800,logsOff)
}

void initialize() {
    log.info "initialize..."
    sendEvent(name: "supportedFanSpeeds", value: groovy.json.JsonOutput.toJson(getFanLevel.collect {k,v -> k}))    
}

void refresh() {
    if (logEnable) log.debug "refresh()"
    parent.sendToDevice(refreshCmd())
}

 String refreshCmd() {
    List<Map<String, String>> attributePaths = []
    
        attributePaths.add(matter.attributePath(device.getDataValue("endpointId"), 0x0006, 0x0000))         // on/off
        attributePaths.add(matter.attributePath(device.getDataValue("endpointId"), 0x0202, 0x0000))         // FanMode
        attributePaths.add(matter.attributePath(device.getDataValue("endpointId"), 0x0202, 0x0002))         // PercentSetting
        attributePaths.add(matter.attributePath(device.getDataValue("endpointId"), 0x0202, 0x0003))         // PercentCurrent
        attributePaths.add(matter.attributePath(device.getDataValue("endpointId"), 0x0202, 0x000A))         // WindSetting
        attributePaths.add(matter.attributePath(device.getDataValue("endpointId"), 0x0202, 0x000B))         // AirflowDirectionEnum
        attributePaths.add(matter.attributePath(device.getDataValue("endpointId"), 0x0003, 0x0000))         
        attributePaths.add(matter.attributePath(device.getDataValue("endpointId"), 0x0003, 0x0001))                        // Power Configuration Cluster : Status
    
    String cmd = matter.readAttributes(attributePaths)
    return cmd
}

void logsOff(){
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable",[value:"false",type:"bool"])
}

Integer hex254ToInt100(String value) {
    return Math.round(hexStrToUnsignedInt(value) / 2.54)
}

String int100ToHex254(value) {
    return intToHexStr(Math.round(value * 2.54))
}


/*void sendToDevice(List<String> cmds, Integer delay = 300) {
    sendHubCommand(new hubitat.device.HubMultiAction(commands(cmds, delay), hubitat.device.Protocol.MATTER))
}

void sendToDevice(String cmd, Integer delay = 300) {
    sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.MATTER))
} */

List<String> commands(List<String> cmds, Integer delay = 300) {
    return delayBetween(cmds.collect { it }, delay)
}
