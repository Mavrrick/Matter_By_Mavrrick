/*
	Air Purifier Driver

	Copyright 2023 Hubitat Inc.  All Rights Reserved

	2023-11-02 2.3.7 maxwell
		-initial pub

*/

import groovy.transform.Field

@Field static final String   DEVICE_TYPE = 'MATTER_PURIFIER'

@Field Map getFanLevel = [
    "off": 0
    ,"low": 33
	,"medium": 66
	,"high": 100
    ,"auto": 101
]

import groovy.transform.Field
import hubitat.helper.HexUtils

metadata {
    definition (name: "Air Purifier Driver(Matter)", namespace: "Mavrrick", author: "Mavrrick") {
        capability "Actuator"
        capability "Switch"
        capability "Configuration"
        capability "FanControl"
        capability "Initialize"
        capability "Refresh"
        attribute "filterLife", "integer"
        attribute "airQuality", "string"
        attribute "fanMode", "string"
        
        command "setSpeed", [[name: "Fan speed*",type:"ENUM", description:"Fan speed to set", constraints: getFanLevel.collect {k,v -> k}]]
        command "auto"
        
        fingerprint endpointId:"01", inClusters:"0003,0202,0071,005B,001D", outClusters:"", model:"Air Purifier", manufacturer:"Leedarson", controllerType:"MAT"

    }
    preferences {
        input(name:"cycleInterval", type:"number", title:"Number of seconds between cycles", defaultValue:30)
        input(name:"onDefault", type:"enum", title:"Default fan mode when turned on", options: getFanLevel.collect {k,v -> k},defaultValue:"auto")
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
        case "005B" :
//            sendEvent(name:"airQualityIndex ", value:value, descriptionText:descriptionText)
            if (logEnable) log.debug  "parse: Air Quality, attribute:${descMap.attrId}, value:${descMap.value}"
            sendAirQualityEvent(descMap.value)
            break
        case "0071" :
            if (descMap.attrId == "0000") { //Filter State
//                sendEvent(name:"filterStatus ", value:value, descriptionText:descriptionText) 
                if (logEnable) log.debug  "parse: ilter State, attribute:${descMap.attrId}, value:${descMap.value}"
                sendFilterEvent(descMap.value)
            } else if (descMap.attrId == "0004") { //Last Change time
                if (logEnable) log.debug  "parse: Filter last change time, attribute:${descMap.attrId}, value:${descMap.value}"
            }
            break
        case "0202" :
            if (descMap.attrId == "0000") { //fan mode
                if (logEnable) log.debug "parse(): Fan Event - Fan Mode ${descMap.value}"
                sendModeEvent(descMap.value)
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
                if (logEnable) log.debug "parse(): Fan Event - Wind Speed setting ${descMap.value}"
            } else  if (descMap.attrId == "000B") { //Airflow Direction
                if (logEnable) log.debug "parse(): Fan Event - Airflow Direction setting ${descMap.value}"
            } else {
                if (logEnable) log.debug  "parse: skipped fan, attribute:${descMap.attrId}, value:${descMap.value}"
            }
//            gatherAttributesValuesInfo(descMap, FanClusterAttributes)
            break
        default :
            if (logEnable) {
                log.debug "skipped:${descMap}"
            }
    }
}

//events
private void sendSpeedEvent(String rawValue) {      
    Integer intValue = hexStrToUnsignedInt(rawValue)
    
    switch(intValue) {
        case 0 :
            value = "off";
        break;
        case 33:
            value = "low";
        break;
        case 66:
            value = "medium";
        break;
        case 100:
            value = "high";
        break; 
    }
    
    String descriptionText = "${device.displayName} was set to speed ${value} RawValue: ${intValue}"
    if (txtEnable) log.info descriptionText
    sendEvent(name:"speed", value:value, descriptionText:descriptionText) 
}

private void sendModeEvent(String rawValue) {      
    Integer intValue = hexStrToUnsignedInt(rawValue)
    
    switch(intValue) {
        case 0 :
            value = "Off";
        break;
        case 1:
            value = "Low";
        break;
        case 2:
            value = "medium";
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
        case 6:
            value = "Smart";
        break;
    }
    
    String descriptionText = "${device.displayName} was set to FanMode ${value} RawValue: ${intValue}"
    if (txtEnable) log.info descriptionText
    sendEvent(name:"fanMode", value:value, descriptionText:descriptionText) 
}

private void sendFilterEvent(String rawValue) {      
    Integer intValue = hexStrToUnsignedInt(rawValue) 

    String descriptionText = "${device.displayName} Filter % Life left ${intValue} RawValue: ${rawValue}"    
    if (txtEnable) log.info descriptionText
   sendEvent(name:"filterLife", value:intValue, descriptionText:descriptionText)
}

private void sendAirQualityEvent(String rawValue) {      
    Integer intValue = hexStrToUnsignedInt(rawValue) 
    
    switch(intValue) {
        case 0 :
            value = "Unknown";
        break;
        case 1 :
            value = "Good";
        break;
        case 2:
            value = "Fair";
        break;
        case 3 :
            value = "Moderate";
        break;
        case 4 :
            value = "Poor";
        break;
        case 5 :
            value = "VeryPoor";
        break;
        case 6 :
            value = "ExtremelyPoor";
        break;
    }

    String descriptionText = "${device.displayName} Air Quality Value left ${value} RawValue: ${intValue}"    
    if (txtEnable) log.info descriptionText
   sendEvent(name:"airQuality", value:value, descriptionText:descriptionText)
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
    unschedule()
    if (logEnable) log.debug "on()"
    String fanspeed = onDefault 
    setSpeed(fanspeed)    
   
}

void off() {
    unschedule()
    if (logEnable) log.debug "off()"
    fanspeed = "off"
    setSpeed(fanspeed)
}

void setSpeed(fanspeed) {
    unschedule()
    if (logEnable) log.debug "Setting Fan Speed to ${fanspeed}"
    switch(fanspeed) {
        case "off":
            value = 0;
        break;
        case "low":
            value = 33;
        break;
        case "medium":
            value = 66;
        break;
        case "high":
            value = 100;
        break; 
        case "auto":
            value = 101;
        break; 
    }
    if (value == 101) {
        if (logEnable) {log.debug ("setSpeed(): Auto mode has been selected")};
        auto()
    } else {
        speedValue = intToHexStr(value)  
        if (logEnable) log.debug "Setting Fan Speed percent ${fanspeed}  % ${value} value to ${speedValue}"
        List<Map<String, String>> attributeWriteRequests = []
        attributeWriteRequests.add(matter.attributeWriteRequest(device.endpointId, 0x0202, 0x0002, 0x04, speedValue ))
        String cmd = matter.writeAttributes(attributeWriteRequests)            
        sendToDevice(cmd)
    }
}

void auto() {

    if (logEnable) log.debug "Setting Fan Mode to Auto"
    value = 5 // 5 is Auto 6 is smart
    modeValue = intToHexStr(value)  
    List<Map<String, String>> attributeWriteRequests = []
    attributeWriteRequests.add(matter.attributeWriteRequest(device.endpointId, 0x0202, 0x0000, 0x04, modeValue ))
    String cmd = matter.writeAttributes(attributeWriteRequests)            
    sendToDevice(cmd)
}

void cycleSpeed() {
    cycleChange()
}

void cycleChange() {
    Integer randomSpeed = Math.abs(new Random().nextInt() % 12) + 1
    String newSpeed = "speed "+randomSpeed
    setSpeed(newSpeed)
    runIn(cycleInterval, cycleChange)
    
}


void configure() {
    log.warn "configure..."
    sendToDevice(subscribeCmd())
    unschedule()
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
//    initializeVars(fullInit = true)
    sendToDevice(subscribeCmd())
}

void refresh() {
    if (logEnable) log.debug "refresh()"
    sendToDevice(refreshCmd())
}

String refreshCmd() {
    List<Map<String, String>> attributePaths = []
    
        attributePaths.add(matter.attributePath(device.endpointId, 0x0006, 0x0000))         // on/off
        attributePaths.add(matter.attributePath(device.endpointId, 0x0202, 0x0000))         // FanMode
        attributePaths.add(matter.attributePath(device.endpointId, 0x0202, 0x0002))         // PercentSetting
        attributePaths.add(matter.attributePath(device.endpointId, 0x0202, 0x0003))         // PercentCurrent
        attributePaths.add(matter.attributePath(device.endpointId, 0x0202, 0x000A))         // WindSetting
        attributePaths.add(matter.attributePath(device.endpointId, 0x0202, 0x000B))         // AirflowDirectionEnum
        attributePaths.add(matter.attributePath(device.endpointId, 0x0003, 0x0000))         
        attributePaths.add(matter.attributePath(device.endpointId, 0x0003, 0x0001))         // Power Configuration Cluster : Status
        attributePaths.add(matter.attributePath(device.endpointId, 0x005B, 0x0000)) 
        attributePaths.add(matter.attributePath(device.endpointId, 0x0071, 0x0000))
    
    String cmd = matter.readAttributes(attributePaths)
    return cmd
}

String subscribeCmd() {
    List<Map<String, String>> attributePaths = []
    String cmd = ''
    
        attributePaths.add(matter.attributePath(0x01, 0x0006, 0x00))
        attributePaths.add(matter.attributePath(0x01, 0x0202, 0x00))
        attributePaths.add(matter.attributePath(0x01, 0x0202, 0x02))
        attributePaths.add(matter.attributePath(0x01, 0x0202, 0x03))
        attributePaths.add(matter.attributePath(0x01, 0x0202, 0x0A))
        attributePaths.add(matter.attributePath(0x01, 0x0202, 0x0B))
        attributePaths.add(matter.attributePath(0x01, 0x005B, 0x00))
        attributePaths.add(matter.attributePath(0x01, 0x0071, 0x00))
        attributePaths.add(matter.attributePath(0x01, 0x0071, 0x04))
        cmd = matter.subscribe(0, 0xFFFF, attributePaths)

    return cmd
}

// Helper methods

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


void sendToDevice(List<String> cmds, Integer delay = 300) {
    sendHubCommand(new hubitat.device.HubMultiAction(commands(cmds, delay), hubitat.device.Protocol.MATTER))
}

void sendToDevice(String cmd, Integer delay = 300) {
    sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.MATTER))
}

List<String> commands(List<String> cmds, Integer delay = 300) {
    return delayBetween(cmds.collect { it }, delay)
}
