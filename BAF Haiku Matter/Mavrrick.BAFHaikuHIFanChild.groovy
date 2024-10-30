/*
	BAF Haiku H/I Fan - Child

	Copyright 2023 Hubitat Inc.  All Rights Reserved

	2024-10-25 0.0.1 mavrrick
		-initial pub
    
    Built for Haiku H/I Series Fan from Big Ass Fans

*/

import groovy.transform.Field

@Field static final String   DEVICE_TYPE = 'MATTER_FAN'

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
import hubitat.helper.HexUtils

metadata {
    definition (name: "BAF Haiku H/I Fan - Child", namespace: "Mavrrick", author: "Mavrrick") {
        capability "Actuator"
        capability "Switch"
//        capability 'SwitchLevel'        
//        capability "ColorTemperature"
//        capability "ChangeLevel"
//        capability 'Light'        
        capability "Configuration"
        capability "FanControl"
//        capability "TemperatureMeasurement"
//        capability "PresenceSensor"
        capability "Initialize"
        capability "Refresh"
        
        command "setSpeed", [[name: "Fan speed*",type:"ENUM", description:"Fan speed to set", constraints: getFanLevel.collect {k,v -> k}]]
        command "airDirection", [[name: "Air Direction*",type:"ENUM", description:"Direction fan is moving", constraints: airDirection]]
        command "fanAuto"
        command "whoosh", [[name: "Whoosh Mode*",type:"ENUM", description:"Whoosh Mode on/off", constraints: whooshOptions]]
        
        attribute "Airflow_Direction", "string"
        attribute "Fan_Mode", "string"
        attribute "windMode", "string"
        
//        fingerprint endpointId:"06", inClusters:"0003,001D,0406", outClusters:"", model:"Haiku H/I Series", manufacturer:"Big Ass Fans", controllerType:"MAT"

    }
    preferences {
        input(name:"cycleInterval", type:"number", title:"Number of seconds between cycles", defaultValue:30)
        input(name:"logEnable", type:"bool", title:"Enable debug logging", defaultValue:false)
        input(name:"txtEnable", type:"bool", title:"Enable descriptionText logging", defaultValue:true)
    }
}

//parsers
void parse(String description) {
    Map descMap = matter.parseDescriptionAsMap(description)
    if (logEnable) log.debug "descMap:${descMap}"
    switch (descMap.cluster) {        
        case "0006" :
            if (descMap.attrId == "0000") { //switch
                sendSwitchEvent(descMap.value)
            }
            break
        case "0000" :
            if (descMap.attrId == "4000") { //software build
                updateDataValue("softwareBuild",descMap.value ?: "unknown")
            }
            break
/*        case '0008' :   // LevelControl
            if (descMap.attrId == '0000') { //current level
                sendLevelEvent(descMap.value)
            }
            else {
                if (logEnable) log.debug "skipped level, attribute:${descMap.attrId}, value:${descMap.value}"
            }
//            gatherAttributesValuesInfo(descMap, LevelControlClusterAttributes)
            break */
        case "0202" :
            if (descMap.attrId == "0000") { //fan speed
                sendFanModeEvent(descMap.value)
                if (logEnable) log.debug "parse(): Fan Event - Fan Speed ${descMap.value}"
            } else if (descMap.attrId == "0001") { //fan speed mode
                if (logEnable) log.debug "parse(): Fan Event - Fan speed mode ${descMap.value}"
            } else if (descMap.attrId == "0002") { //fan speed Percent Setting
                sendSpeedEvent(descMap.value) 
                if (logEnable) log.debug "parse(): Fan Event - Fan Speed  Percent ${descMap.value}"
            } else  if (descMap.attrId == "0003") { //fan speed Percent current
                if (logEnable) log.debug "parse(): Fan Event - Fan Speed Percent Curent ${descMap.value}"
            } else if (descMap.attrId == "0004") { //fan speed max (Don't expect to actually ever return in parse
                if (logEnable) log.debug "parse(): Fan Event - Fan Speed Max speed ${descMap.value}"
            } else if (descMap.attrId == "0005") { //fan speed setting
                if (logEnable) log.debug "parse(): Fan Event - Fan Speed setting ${descMap.value}"
            } else if (descMap.attrId == "0006") { //fan speed setting current
                if (logEnable) log.debug "parse(): Fan Event - Fan Speed current ${descMap.value}"
            } else if (descMap.attrId == "000A") { //WindSetting current
                sendWindMode(descMap.value)
                if (logEnable) log.debug "parse(): Fan Event - Wind Speed setting ${descMap.value}"
            } else  if (descMap.attrId == "000B") { //Airflow Direction
                if (logEnable) log.debug "parse(): Fan Event - Airflow Direction setting ${descMap.value}"
                sendFanDirEvent(descMap.value)
            } else {
                if (logEnable) log.debug "parse: skipped fan, attribute:${descMap.attrId}, value:${descMap.value}"
            }
//            gatherAttributesValuesInfo(descMap, FanClusterAttributes)
            break
/*        case '0300' :   // ColorControl
            if (descMap.attrId == '0000') { //hue                
                sendHueEvent(descMap.value)
            } else if (descMap.attrId == '0001') { //saturation
                sendSaturationEvent(descMap.value)
            }
            else if (descMap.attrId == '0007') { //color temperature
                sendCTEvent(descMap.value)
            }
            else if (descMap.attrId == '0008') { //color mode
                if (logEnable) log.debug "parse: skipped color mode:${descMap}"
            }
            else {
                if (logEnable) log.debug "parse: skipped color, attribute:${descMap.attrId}, value:${descMap.value}"
            }
//            gatherAttributesValuesInfo(descMap, ColorControlClusterAttributes)
            break
        case '0402' :   // Temp Sensor
            if (logEnable) log.debug "parse: Temp Sensor Information, attribute:${descMap.attrId}, value:${descMap.value}"
            sendTempEvent(descMap.value)              
            break  
        case '0406' :   // Occupancy Sensor
                if (logEnable) log.debug "parse: Occupancy sensor Information, attribute:${descMap.attrId}, value:${descMap.value}"
            sendOccupancyEvent(descMap.value)
            break */
        default :
            if (logEnable) {
                log.debug "skipped:${descMap}"
            }
    }
}

//events
private void sendFanModeEvent(String rawValue) {      
    Integer intValue = hexStrToUnsignedInt(rawValue) 
    
    switch(intValue) {
        case 0 :
            value = "off";
        break;
        case 1:
            value = "Low";
        break;
        case 2:
            value = "Medium";
        break;
        case 3:
            value = "High";
        break;
        case 4:
            value = "On";
        break;
        case 5:
            value = "Auto";
        break;
    }
    
    String descriptionText = "${device.displayName} Mode was set to ${value}"
    if (txtEnable) log.info descriptionText
    sendEvent(name:"Fan_Mode", value:value, descriptionText:descriptionText)
}

private void sendSpeedEvent(String rawValue) { 
    Integer intValue = hexStrToUnsignedInt(rawValue) 
    
    switch(intValue) {
        case 0 :
            value = "off";
        break;
        case 1:
            value = "speed 1";
        break;
        case 18:
            value = "speed 2";
        break;
        case 34:
            value = "speed 3";
        break;
        case 51:
            value = "speed 4";
        break;
        case 67:
            value = "speed 5";
        break;
        case 84:
            value = "speed 6";
        break;
        case 100:
            value = "speed 7";
        break; 
    }
    
    String descriptionText = "${device.displayName} was set to speed ${value}"
    if (txtEnable) log.info descriptionText
    sendEvent(name:"speed", value:value, descriptionText:descriptionText)
}

private void sendFanDirEvent(String rawValue) {
    if (rawValue == "00") {
        String descriptionText = "${device.displayName} airFlow direction is forward direction"
        if (txtEnable) log.info descriptionText
        sendEvent(name:"Airflow_Direction", value:"forward", descriptionText:descriptionText)
    } else if (rawValue == "01"){
        String descriptionText = "${device.displayName} airFlow direction is reverse direction"
        if (txtEnable) log.info descriptionText
        sendEvent(name:"Airflow_Direction", value:"reverse", descriptionText:descriptionText)
    }
}

private void sendWindMode(String rawValue) {
    if (rawValue == "00") {
        String descriptionText = "${device.displayName} wind mode is Normal"
        if (txtEnable) log.info descriptionText
        sendEvent(name:"windMode", value:"Normal", descriptionText:descriptionText)
    } else if (rawValue == "02"){
        String descriptionText = "${device.displayName} wind mode is whoosh"
        if (txtEnable) log.info descriptionText
        sendEvent(name:"windMode", value:"Whoosh", descriptionText:descriptionText)
    }
}

private void sendSwitchEvent(String rawValue) {
    String value = rawValue == "01" ? "on" : "off"
    if (device.currentValue("switch") == value) return
    String descriptionText = "${device.displayName} was turned ${value}"
    if (txtEnable) log.info descriptionText
    sendEvent(name:"switch", value:value, descriptionText:descriptionText)
}

//capability commands
void on() { 
    value = 4
    autoValue = intToHexStr(value)  
    if (logEnable) log.debug "Setting Fan mode  to Auto"
    List<Map<String, String>> attributeWriteRequests = []
    attributeWriteRequests.add(matter.attributeWriteRequest(device.getDataValue("endpointId"), 0x0202, 0x0000, 0x04, autoValue ))
    String cmd = matter.writeAttributes(attributeWriteRequests)            
    parent.sendToDevice(cmd)
}

void off() { 
    value = 0
    autoValue = intToHexStr(value)  
    if (logEnable) log.debug "Setting Fan mode  to Auto"
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

/* void cycleSpeed() {
    cycleChange()
}

void cycleChange() {
    Integer randomSpeed = Math.abs(new Random().nextInt() % 12) + 1
    String newSpeed = "speed "+randomSpeed
    setSpeed(newSpeed)
    runIn(cycleInterval, cycleChange)
    
} */



void configure() {
    log.warn "configure..."
/*    sendToDevice(cleanSubscribeCmd())
    sendToDevice(subscribeCmd())
    sendToDevice(matter.getMatterEndpoints())
    unschedule() */
}

//lifecycle commands
void updated(){
    log.info "updated..."
    log.warn "debug logging is: ${logEnable == true}"
    log.warn "description logging is: ${txtEnable == true}"
    if (logEnable) runIn(1800,logsOff)
}

void initialize() {
    log.info "initialize..."
    sendEvent(name: "supportedFanSpeeds", value: groovy.json.JsonOutput.toJson(getFanLevel.collect {k,v -> k}))    
//    initializeVars(fullInit = true)
//    sendToDevice(cleanSubscribeCmd())
//    sendToDevice(subscribeCmd())
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
