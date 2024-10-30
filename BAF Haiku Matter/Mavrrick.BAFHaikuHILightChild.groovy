/*
	BAF Haiku H/I Light - Child

	Copyright 2023 Hubitat Inc.  All Rights Reserved

	2024-10-25 0.0.1 mavrrick
		-initial pub
    
    Built for Haiku H/I Series Fan from Big Ass Fans

*/

import groovy.transform.Field

import groovy.transform.Field
import hubitat.helper.HexUtils

metadata {
    definition (name: "BAF Haiku H/I Light - Child", namespace: "Mavrrick", author: "Mavrrick") {
        capability "Actuator"
        capability "Switch"
        capability 'SwitchLevel'        
        capability "ColorTemperature"
        capability "ChangeLevel"
        capability 'Light'        
        capability "Configuration"
        capability "Initialize"
        capability "Refresh"
        
    }
    preferences {
        input(name:"logEnable", type:"bool", title:"Enable debug logging", defaultValue:false)
        input(name:"txtEnable", type:"bool", title:"Enable descriptionText logging", defaultValue:true)
    }
}


//capability commands
void on() { 
    String cmd = matter.invoke(device.endpointId, 0x0006, 0x0001)
    parent.sendToDevice(cmd)
}

void off() { 
    String cmd = matter.invoke(device.endpointId, 0x0006, 0x0000)
    parent.sendToDevice(cmd)
}


//// Level control commands related to Light Devices
void setLevel(Object value, Object rate=0) { //new set level routine to enable immediate change
    if (logEnable) log.debug "setLevel(${value}, ${rate})"
    Integer newLevel = value
    Integer transitiontime2 = rate
    newLevel2 = int100ToHex254(newLevel)
    transition = HexUtils.integerToHexString(transitiontime2,2)
    
    String cmds 
    if (device.currentValue("switch") == "on"){
        List<Map<String, String>> cmdFields = []
        cmdFields.add(matter.cmdField(0x04, 0x00, newLevel2)) 
        cmdFields.add(matter.cmdField(0x05, 0x01, transition))
        log.debug "Endpoint: ${device.endpointId} commands: ${cmdFields}"   
        cmds = matter.invoke(device.endpointId, 0x0008, 0x0000, cmdFields)   
    } else {
        List<Map<String, String>> cmdFields = []
        cmdFields.add(matter.cmdField(0x04, 0x00, newLevel2)) 
        cmdFields.add(matter.cmdField(0x05, 0x01, transition))
        log.debug "Endpoint: ${device.endpointId} commands: ${cmdFields}"
        cmds = matter.invoke(device.endpointId, 0x0008, 0x0004, cmdFields) 
    }
    parent.sendToDevice(cmds)
}

void startLevelChange(direction) {
    if (logEnable) log.debug "Start level change in ${direction}"  
        switch(direction) {
        case "up":
            value = 0;
        break;
        case "down":
            value = 1;
        break; 
    }
    dirValue = intToHexStr(value)
    rateValue = intToHexStr(10)
    if (logEnable) log.debug "Sending command to change in ${direction} with value ${dirValue}"
    List<Map<String, String>> cmdFields = []
    cmdFields.add(matter.cmdField(0x04, 0x00, dirValue))
    cmdFields.add(matter.cmdField(0x04, 0x01, rateValue))
    cmds = matter.invoke(device.endpointId, 0x0008, 0x0001, cmdFields)            
    parent.sendToDevice(cmds)
}

void stopLevelChange() {
    if (logEnable) log.debug "Stoping level change"  
    dirValue = intToHexStr(value)  
    if (logEnable) log.debug "Sending command to change in ${direction} with value ${action}"
    
    List<Map<String, String>> cmdFields = []
//    cmdFields.add(matter.cmdField(0x04, 0x00, dirValue))
    cmds = matter.invoke(device.endpointId, 0x0008, 0x0003)            
    parent.sendToDevice(cmds)
}

///// Color Control Commands related to Light control devices

void setHue(Object value) {
    if (logEnable) log.debug "setHue(${value})"
    
    Integer intHue = value
    newHue = int100ToHex254(intHue)
    direction = intToHexStr(1)
    Integer transitionTime2 = (transitionTime ?: 1).toInteger()
    transition = HexUtils.integerToHexString(transitionTime2,2)
    
        String cmds 
    if (device.currentValue("switch") == "on"){
        List<Map<String, String>> cmdFields = []
        cmdFields.add(matter.cmdField(0x04, 0x00, newHue)) 
        cmdFields.add(matter.cmdField(0x04, 0x01, direction))        
        cmdFields.add(matter.cmdField(0x05, 0x02, transition))
        if (logEnable) log.debug "Endpoint: ${device.endpointId} commands: ${cmdFields}"   
        cmds = matter.invoke(device.endpointId, 0x0300, 0x0000, cmdFields)   
    } else {
        on()
        List<Map<String, String>> cmdFields = []
        cmdFields.add(matter.cmdField(0x04, 0x00, newHue)) 
        cmdFields.add(matter.cmdField(0x04, 0x01, direction)) 
        cmdFields.add(matter.cmdField(0x05, 0x02, transition))
        if (logEnable) log.debug "Endpoint: ${device.endpointId} commands: ${cmdFields}"
        cmds = matter.invoke(device.endpointId, 0x0300, 0x0000, cmdFields) 
    } 
    parent.sendToDevice(cmds)
    sendEvent(name: "colorMode", value: "RGB")
}

void setSaturation(Object value) {
    if (logEnable) log.debug "setHue(${value})"
    
    Integer intSat = value
    newSat = int100ToHex254(intSat)
    direction = intToHexStr(1)
    Integer transitionTime2 = (transitionTime ?: 1).toInteger()
    transition = HexUtils.integerToHexString(transitionTime2,2)
    
        String cmds 
    if (device.currentValue("switch") == "on"){
        List<Map<String, String>> cmdFields = []
        cmdFields.add(matter.cmdField(0x04, 0x00, newSat)) 
        cmdFields.add(matter.cmdField(0x04, 0x01, direction))        
        cmdFields.add(matter.cmdField(0x05, 0x02, transition))
        if (logEnable) log.debug "Endpoint: ${device.endpointId} commands: ${cmdFields}"   
        cmds = matter.invoke(device.endpointId, 0x0300, 0x0003, cmdFields)   
    } else {
        on()
        List<Map<String, String>> cmdFields = []
        cmdFields.add(matter.cmdField(0x04, 0x00, newSat)) 
        cmdFields.add(matter.cmdField(0x04, 0x01, direction)) 
        cmdFields.add(matter.cmdField(0x05, 0x02, transition))
        if (logEnable) log.debug "Endpoint: ${device.endpointId} commands: ${cmdFields}"
        cmds = matter.invoke(device.endpointId, 0x0300, 0x0003, cmdFields) 
    } 
    parent.sendToDevice(cmds)
    sendEvent(name: "colorMode", value: "RGB")
}

void setHueSat(Object hue, Object sat) {
    if (logEnable) log.debug "setHueSat(${hue}, ${sat})"
    
    Integer intHue = hue
    Integer intSat = sat
    newHue = int100ToHex254(intHue)
    newSat = int100ToHex254(intSat)
    
    Integer transitionTime2 = (transitionTime ?: 1).toInteger()
    transition = HexUtils.integerToHexString(transitionTime2,2)
    
        String cmds 
    if (device.currentValue("switch") == "on"){
        List<Map<String, String>> cmdFields = []
        cmdFields.add(matter.cmdField(0x04, 0x00, newHue)) 
        cmdFields.add(matter.cmdField(0x04, 0x01, newSat))        
        cmdFields.add(matter.cmdField(0x05, 0x02, transition))
        if (logEnable) log.debug "Endpoint: ${device.endpointId} commands: ${cmdFields}"   
        cmds = matter.invoke(device.endpointId, 0x0300, 0x0006, cmdFields)   
    } else {
        on()
        List<Map<String, String>> cmdFields = []
        cmdFields.add(matter.cmdField(0x04, 0x00, newHue)) 
        cmdFields.add(matter.cmdField(0x04, 0x01, newSat)) 
        cmdFields.add(matter.cmdField(0x05, 0x02, transition))
        if (logEnable) log.debug "Endpoint: ${device.endpointId} commands: ${cmdFields}"
        cmds = matter.invoke(device.endpointId, 0x0300, 0x0006, cmdFields) 
    } 
    parent.sendToDevice(cmds)
    sendEvent(name: "colorMode", value: "RGB")
}

void setColorTemperature(colortemperature, level=null, transitionTime=0) { // New method with Invoke instead of Hubitat calls
    if (colortemperature < 2700)  {colortemperature =  2700}
    if (colortemperature > 6500)  {colortemperature =  6500}
    Integer mired = Math.round(1000000/colortemperature)
    Integer transitiontime2 = transitionTime 
    ctValue = zigbee.swapOctets(HexUtils.integerToHexString(mired, 2))
    transition = HexUtils.integerToHexString(transitiontime2,2)

    if (level != null) {
        setLevel(level)
    }
    
    if (logEnable) log.debug "setcolortemp() ${colortemperature} in hex ${ct3} swapped ${ctValue} Mired value ${mired} transition is ${transition} "
    String cmds 
    if (device.currentValue("switch") == "on"){
        List<Map<String, String>> cmdFields = []
        cmdFields.add(matter.cmdField(0x05, 0x00, ctValue)) 
        cmdFields.add(matter.cmdField(0x05, 0x01, transition))
        if (logEnable) log.debug "Endpoint: ${device.endpointId} commands: ${cmdFields}"   
        cmds = matter.invoke(device.endpointId, 0x0300, 0x000A, cmdFields)   
    } else {
        on()
        List<Map<String, String>> cmdFields = []
        cmdFields.add(matter.cmdField(0x05, 0x00, ctValue)) 
        cmdFields.add(matter.cmdField(0x05, 0x01, transition))
        if (logEnable) log.debug "Endpoint: ${device.endpointId} commands: ${cmdFields}"   
        cmds = matter.invoke(device.endpointId, 0x0300, 0x000A, cmdFields)
    }
    parent.sendToDevice(cmds)
    sendEvent(name: "colorMode", value: "CT")
} 

void setColor(Map colorMap) {
    if (logEnable) log.debug "setColor(${colorMap})"
    if (colorMap.level) {
        setLevel(colorMap.level)
    }
    if (colorMap.hue != null && colorMap.saturation != null) {
        setHueSat(colorMap.hue, colorMap.saturation)
    } else if (colorMap.hue != null) {
        setHue(colorMap.hue)
    } else if (colorMap.saturation != null) {
        setSaturation(colorMap.saturation)
    }
}


void configure() {
    log.warn "configure..."
//    parent.sendToDevice(cleanSubscribeCmd())
//    parent.sendToDevice(subscribeCmd())
//    parent.sendToDevice(matter.getMatterEndpoints())
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
//    sendEvent(name: "supportedFanSpeeds", value: groovy.json.JsonOutput.toJson(getFanLevel.collect {k,v -> k}))    
//    initializeVars(fullInit = true)
//    sendToDevice(cleanSubscribeCmd())
//    sendToDevice(subscribeCmd())
    refresh()
}

void refresh() {
    if (logEnable) log.debug "refresh()"
    sendToDevice(refreshCmd())
}

String refreshCmd() {
    List<Map<String, String>> attributePaths = []
    
        attributePaths.add(matter.attributePath(device.endpointId, 0x0006, 0x0000))
        attributePaths.add(matter.attributePath(device.endpointId, 0x0008, 0x0000))
        attributePaths.add(matter.attributePath(device.endpointId, 0x0300, 0x0000))
        attributePaths.add(matter.attributePath(device.endpointId, 0x0300, 0x0001))
        attributePaths.add(matter.attributePath(device.endpointId, 0x0300, 0x0007))
    
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


/* void sendToDevice(List<String> cmds, Integer delay = 300) {
    sendHubCommand(new hubitat.device.HubMultiAction(commands(cmds, delay), hubitat.device.Protocol.MATTER))
}

void sendToDevice(String cmd, Integer delay = 300) {
    sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.MATTER))
} */

List<String> commands(List<String> cmds, Integer delay = 300) {
    return delayBetween(cmds.collect { it }, delay)
}
