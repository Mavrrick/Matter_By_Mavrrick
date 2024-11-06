/*
	BAF Haiku H/I (Matter)

	Copyright 2023 Hubitat Inc.  All Rights Reserved

	2024-10-25 0.0.1 mavrrick
		-initial pub
    
    Built for Haiku H/I Series Fan from Big Ass Fans

*/

@Field static final String   DEVICE_TYPE = 'MATTER_FAN'
@Field static final String  IMPORT_URL =  'https://raw.githubusercontent.com/Mavrrick/Matter_By_Mavrrick/refs/heads/main/BAF%20Haiku%20Matter/Mavrrick.BAFHaikuHIMatter.groovy'


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
    definition (name: "BAF Haiku H/I (Matter)", namespace: "Mavrrick", author: "Mavrrick", importUrl: IMPORT_URL) {
        capability "Actuator"
        capability "Switch"
        capability 'SwitchLevel'        
        capability "ColorTemperature"
        capability "ChangeLevel"
        capability 'Light'        
        capability "Configuration"
        capability "FanControl"
        capability "TemperatureMeasurement"
        capability "PresenceSensor"
        capability "Initialize"
        capability "Refresh"
        
//        command 'getInfo'
        command "setSpeed", [[name: "Fan speed*",type:"ENUM", description:"Fan speed to set", constraints: getFanLevel.collect {k,v -> k}]]
        command "airDirection", [[name: "Air Direction*",type:"ENUM", description:"Direction fan is moving", constraints: airDirection]]
        command "fanAuto"
        command "whoosh", [[name: "Whoosh Mode*",type:"ENUM", description:"Whoosh Mode on/off", constraints: whooshOptions]]
        
        attribute "Airflow_Direction", "string"
        attribute "Fan_Mode", "string"
        attribute "windMode", "string"
        
        fingerprint endpointId:"06", inClusters:"0003,001D,0406", outClusters:"", model:"Haiku H/I Series", manufacturer:"Big Ass Fans", controllerType:"MAT"

    }
    preferences {
        input(name:"cycleInterval", type:"number", title:"Number of seconds between cycles", defaultValue:30)
        input(name:"levelChgStep", type:"number", title:"Steps during level change", defaultValue:10)
        input(name:"logEnable", type:"bool", title:"Enable debug logging", defaultValue:false)
        input(name:"txtEnable", type:"bool", title:"Enable descriptionText logging", defaultValue:true)
    }
}

//parsers
void parse(String description) {
    if (logEnable) log.debug "descMap:${description}"
    Map descMap = matter.parseDescriptionAsMap(description)
    if (logEnable) log.debug "descMap:${descMap}"
    if (descMap.endpoint == "00") {
        switch (descMap.cluster) {
            case "001D" :
                if (descMap.attrId == "0003") { //Endpoint list
                    atomic.state.endpoints = descMap.value
                    if (logEnable) log.debug "parse(): Endpoints found on device ${descMap.value}"
                }
            break
        }
    } else {  
    switch (descMap.cluster) {
        case "001D" :
                if (descMap.attrId == "0001") { //list Clusters for endpoint
                    state.endpointlist = [:]
                    List clusters = []
                    if (logEnable) log.debug "parse(): Cluster data found Endpoint:${descMap.endpoint}, Cluster returned${descMap.value}"
                    descMap.value.forEach{
                        count = it.length()
                        if (count == 2) {
                            clustCon = "00"+it
                        } else if (count == 3) {
                            clustCon = "0"+it
                        } else {
                        }
                        clusters.add(clustCon)
                    }
                    atomic.state.endpointlist.put(descMap.endpoint,clusters)
                }
                break
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
        case '0008' :   // LevelControl
            if (descMap.attrId == '0000') { //current level
                sendLevelEvent(descMap.value)
            }
            else {
                if (logEnable) log.debug "skipped level, attribute:${descMap.attrId}, value:${descMap.value}"
            }
//            gatherAttributesValuesInfo(descMap, LevelControlClusterAttributes)
            break
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
        case '0300' :   // ColorControl
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
            break 
        default :
            if (logEnable) {
                log.debug "skipped:${descMap}"
            }
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
        case 84:
            value = "speed 6";
        break;
        case 100:
            value = "speed 7";
        break; 
    }
    
//    if (device.currentValue("switch") == value) return
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

private void sendOccupancyEvent(String rawValue) {
    Integer value = hexStrToUnsignedInt(rawValue)
    if (logEnable) log.debug "Ocupancy value provided is ${rawValue} converted is ${value}"
    if (value == 1) {
        String descriptionText = "${device.displayName} is present"
        if (txtEnable) log.info descriptionText
        sendEvent(name:"presence", value:"present", descriptionText:descriptionText)
    } else {
        String descriptionText = "${device.displayName} is not present"
        if (txtEnable) log.info descriptionText
        sendEvent(name:"presence", value:"not present", descriptionText:descriptionText)
    }
}

private void sendTempEvent(String rawValue) {
    Integer value = hexStrToUnsignedInt(rawValue)
    tempValue = ((value/100)*1.8+32)
    String descriptionText = "${device.displayName} temperature is ${tempValue}"
    if (txtEnable) log.info descriptionText
    sendEvent(name:"temperature", value:tempValue, descriptionText:descriptionText, unit: "")
}

private void sendLevelEvent(String rawValue) {
    Integer value = Math.round(hexStrToUnsignedInt(rawValue) / 2.55)
    if (value == 0 || value == device.currentValue("level")) return
    String descriptionText = "${device.displayName} level was set to ${value}%"
    if (txtEnable) log.info descriptionText
    sendEvent(name:"level", value:value, descriptionText:descriptionText, unit: "%")
}

private void sendHueEvent(String rawValue, Boolean presetColor = false) {
    Integer value = hex254ToInt100(rawValue)
    if (device.currentValue("hue") != value ) {
    sendRGBNameEvent(value)
    String descriptionText = "${device.displayName} hue was set to ${value}%"
    if (txtEnable) log.info descriptionText
    sendEvent(name: "colorMode", value: "RGB")
    sendEvent(name:"hue", value:value, descriptionText:descriptionText, unit: "%")
    }   
}

private void sendSaturationEvent(String rawValue, Boolean presetColor = false) {
    Integer value = hex254ToInt100(rawValue)
    if (device.currentValue("saturation") != value ) {
    sendRGBNameEvent(null,value)
    String descriptionText = "${device.displayName} saturation was set to ${value}%"
    if (txtEnable) log.info descriptionText
    sendEvent(name: "colorMode", value: "RGB")
    sendEvent(name:"saturation", value:value, descriptionText:descriptionText, unit: "%")
    }
}

private void sendRGBNameEvent(hue, sat = null){
    String genericName
    if (device.currentValue("saturation") == 0) {
        genericName = "White"
    } else if (hue == null) {
        return
    } else {
        genericName = colorRGBName.find{k , v -> hue < k}.value
    }
    if (genericName == device.currentValue("colorName")) return
    String descriptionText = "${device.displayName} color is ${genericName}"
    if (txtEnable) log.info descriptionText    
    sendEvent(name: "colorName", value: genericName ,descriptionText: descriptionText)
}

private void sendCTEvent(String rawValue, Boolean presetColor = false) { 
    if ((hexStrToUnsignedInt(rawValue)) != 0 ) {
        value = (Math.round(10000/(hexStrToUnsignedInt(rawValue))))*100
        if (value != device.currentValue("colorTemperature")) {
            String descriptionText = "${device.displayName} ColorTemp was set to ${value}K" 
        if (txtEnable) log.info descriptionText
        sendEvent(name:"colorTemperature", value:value, descriptionText:descriptionText, unit: "K")
        sendEvent(name: "colorMode", value: "CT") 
        }
    }
}

//capability commands
void on() { // Broken need to fix
/*    unschedule()
    if (logEnable) log.debug "on()"
    sendToDevice(matter.on()) */
    String cmd = matter.invoke(0x02, 0x0006, 0x0001)
    sendToDevice(cmd)
}

void off() { //Broken Need to fix
/*    unschedule()
    if (logEnable) log.debug "off()"
    sendToDevice(matter.off()) */
    String cmd = matter.invoke(0x02, 0x0006, 0x0000)
    sendToDevice(cmd)
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
        attributeWriteRequests.add(matter.attributeWriteRequest(0x01, 0x0202, 0x0000, 0x04, autoValue ))
        String cmd = matter.writeAttributes(attributeWriteRequests)            
        sendToDevice(cmd)
    } 
    speedValue = intToHexStr(value)  
    if (logEnable) log.debug "Setting Fan Speed percent ${fanspeed}  % ${value} value to ${speedValue}"
    List<Map<String, String>> attributeWriteRequests = []
    attributeWriteRequests.add(matter.attributeWriteRequest(0x01, 0x0202, 0x0002, 0x04, speedValue ))
    String cmd = matter.writeAttributes(attributeWriteRequests)            
    sendToDevice(cmd)
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
    attributeWriteRequests.add(matter.attributeWriteRequest(0x01, 0x0202, 0x000B, 0x04, dirValue ))
    String cmd = matter.writeAttributes(attributeWriteRequests)            
    sendToDevice(cmd)
}

void fanAuto() {
    value = 5
    autoValue = intToHexStr(value)  
    if (logEnable) log.debug "Setting Fan mode  to Auto"
    List<Map<String, String>> attributeWriteRequests = []
    attributeWriteRequests.add(matter.attributeWriteRequest(0x01, 0x0202, 0x0000, 0x04, autoValue ))
    String cmd = matter.writeAttributes(attributeWriteRequests)            
    sendToDevice(cmd)
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
    attributeWriteRequests.add(matter.attributeWriteRequest(0x01, 0x0202, 0x000A, 0x04, whooshValue ))
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
        cmds = matter.invoke(0x02, 0x0008, 0x0000, cmdFields)   
    } else {
        List<Map<String, String>> cmdFields = []
        cmdFields.add(matter.cmdField(0x04, 0x00, newLevel2)) 
        cmdFields.add(matter.cmdField(0x05, 0x01, transition))
        log.debug "Endpoint: ${device.endpointId} commands: ${cmdFields}"
        cmds = matter.invoke(0x02, 0x0008, 0x0004, cmdFields) 
    }
    sendToDevice(cmds)
}

void startLevelChange(direction) {
    if (logEnable) log.debug "Start level change in ${direction}"  
        switch(direction) {
        case "up":
            if (logEnable) log.debug "Found ${direction} setting value to 0"
            value = 0;
        break;
        case "down":
            if (logEnable) log.debug "Found ${direction} setting value to 1" 
            value = 1;
        break; 
    }
    dirValue = intToHexStr(value)
    rateValue = intToHexStr(levelChgStep)
    if (logEnable) log.debug "Sending command to change in ${direction} numvalue: ${value} value ${dirValue} Rate Number ${levelChgStep} hex rate ${rateValue}"
    List<Map<String, String>> cmdFields = []
    cmdFields.add(matter.cmdField(0x04, 0x00, dirValue))
    cmdFields.add(matter.cmdField(0x04, 0x01, rateValue))
    cmds = matter.invoke(0x02, 0x0008, 0x0005, cmdFields)            
    sendToDevice(cmds)
}

void stopLevelChange() {
    if (logEnable) log.debug "Stoping level change"  
    dirValue = intToHexStr(value)  
    if (logEnable) log.debug "Sending command to change in ${direction} with value ${action}"
    
    List<Map<String, String>> cmdFields = []
    cmds = matter.invoke(device.getDataValue("endpointId"), 0x0008, 0x0007)            
    sendToDevice(cmds)
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
        cmds = matter.invoke(0x02, 0x0300, 0x0000, cmdFields)   
    } else {
        on()
        List<Map<String, String>> cmdFields = []
        cmdFields.add(matter.cmdField(0x04, 0x00, newHue)) 
        cmdFields.add(matter.cmdField(0x04, 0x01, direction)) 
        cmdFields.add(matter.cmdField(0x05, 0x02, transition))
        if (logEnable) log.debug "Endpoint: ${device.endpointId} commands: ${cmdFields}"
        cmds = matter.invoke(0x02, 0x0300, 0x0000, cmdFields) 
    } 
    sendToDevice(cmds)
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
        cmds = matter.invoke(0x02, 0x0300, 0x0003, cmdFields)   
    } else {
        on()
        List<Map<String, String>> cmdFields = []
        cmdFields.add(matter.cmdField(0x04, 0x00, newSat)) 
        cmdFields.add(matter.cmdField(0x04, 0x01, direction)) 
        cmdFields.add(matter.cmdField(0x05, 0x02, transition))
        if (logEnable) log.debug "Endpoint: ${device.endpointId} commands: ${cmdFields}"
        cmds = matter.invoke(0x02, 0x0300, 0x0003, cmdFields) 
    } 
    sendToDevice(cmds)
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
        cmds = matter.invoke(0x02, 0x0300, 0x0006, cmdFields)   
    } else {
        on()
        List<Map<String, String>> cmdFields = []
        cmdFields.add(matter.cmdField(0x04, 0x00, newHue)) 
        cmdFields.add(matter.cmdField(0x04, 0x01, newSat)) 
        cmdFields.add(matter.cmdField(0x05, 0x02, transition))
        if (logEnable) log.debug "Endpoint: ${device.endpointId} commands: ${cmdFields}"
        cmds = matter.invoke(0x02, 0x0300, 0x0006, cmdFields) 
    } 
    sendToDevice(cmds)
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
        cmds = matter.invoke(0x02, 0x0300, 0x000A, cmdFields)   
    } else {
        on()
        List<Map<String, String>> cmdFields = []
        cmdFields.add(matter.cmdField(0x05, 0x00, ctValue)) 
        cmdFields.add(matter.cmdField(0x05, 0x01, transition))
        if (logEnable) log.debug "Endpoint: ${device.endpointId} commands: ${cmdFields}"   
        cmds = matter.invoke(0x02, 0x0300, 0x000A, cmdFields)
    }
    sendToDevice(cmds)
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
    sendToDevice(cleanSubscribeCmd())
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

void installed(){
    if (debugLog) {log.warn "installed(): Driver Installed"}
    initialize()
}

void initialize() {
    log.info "initialize..."
    sendEvent(name: "supportedFanSpeeds", value: groovy.json.JsonOutput.toJson(getFanLevel.collect {k,v -> k}))    
//    initializeVars(fullInit = true)
    sendToDevice(getEndpoints())
    pauseExecution(1000)
    sendToDevice(getClusters())
    sendToDevice(cleanSubscribeCmd())
//    sendToDevice(subscribeCmd())
}

void refresh() {
    if (logEnable) log.debug "refresh()"
    sendToDevice(refreshCmd())
}

String getEndpoints() {
    List<Map<String, String>> attributePaths = []
    
        attributePaths.add(matter.attributePath(0x00, 0x001D, 0x0003))         // Descriptor to list endpoints
    
    String cmd = matter.readAttributes(attributePaths)
    return cmd
}

String getClusters() {
    List<Map<String, String>> attributePaths = []
    
    state.endpoints.forEach {
        attributePaths.add(matter.attributePath(it, 0x001D, 0x0001))           // Descriptor to list cluster for a endpoint
    }
        
    String cmd = matter.readAttributes(attributePaths)
    return cmd
}

String refreshCmd() {
    List<Map<String, String>> attributePaths = []
    
        attributePaths.add(matter.attributePath(0x01, 0x0006, 0x0000))         // on/off
        attributePaths.add(matter.attributePath(0x01, 0x0202, 0x0000))         // FanMode
        attributePaths.add(matter.attributePath(0x01, 0x0202, 0x0002))         // PercentSetting
        attributePaths.add(matter.attributePath(0x01, 0x0202, 0x0003))         // PercentCurrent
        attributePaths.add(matter.attributePath(0x01, 0x0202, 0x000A))         // WindSetting
        attributePaths.add(matter.attributePath(0x01, 0x0202, 0x000B))         // AirflowDirectionEnum
        attributePaths.add(matter.attributePath(0x01, 0x0003, 0x0000))         
        attributePaths.add(matter.attributePath(0x01, 0x0003, 0x0001))                        // Power Configuration Cluster : Status
    
        attributePaths.add(matter.attributePath(0x02, 0x0006, 0x0000))
        attributePaths.add(matter.attributePath(0x02, 0x0008, 0x0000))
        attributePaths.add(matter.attributePath(0x02, 0x0300, 0x0000))
        attributePaths.add(matter.attributePath(0x02, 0x0300, 0x0001))
        attributePaths.add(matter.attributePath(0x02, 0x0300, 0x0007))
//        attributePaths.add(matter.attributePath(0x02, 0x0300, 0x0008)) 
   
        attributePaths.add(matter.attributePath(0x04, 0x0402, 0x0000))        //Temperature mesurement
        attributePaths.add(matter.attributePath(0x06, 0x0406, 0x0000))        //Occupancy Value
    
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
        attributePaths.add(matter.attributePath(0x02, 0x0005, 0x01))
        attributePaths.add(matter.attributePath(0x02, 0x0006, 0x00))
        attributePaths.add(matter.attributePath(0x02, 0x0008, 0x00))
        attributePaths.add(matter.attributePath(0x02, 0x0300, 0x00))
        attributePaths.add(matter.attributePath(0x02, 0x0300, 0x01))
        attributePaths.add(matter.attributePath(0x02, 0x0300, 0x07))
//        attributePaths.add(matter.attributePath(0x01, 0x0300, 0x08))
    
        attributePaths.add(matter.attributePath(0x04, 0x0402, 0x00))
        attributePaths.add(matter.attributePath(0x06, 0x0406, 0x00))
    
        cmd = matter.subscribe(0, 0xFFFF, attributePaths)

    return cmd
}

// availabe from HE platform version [2.3.9.186]
String cleanSubscribeCmd() {
    List<Map<String, String>> attributePaths = [] 
    
   String cmd = ''
    
    attributePaths.add(matter.attributePath(0x01, 0x0006, 0x00))
    attributePaths.add(matter.attributePath(0x01, 0x0202, 0x00))
    attributePaths.add(matter.attributePath(0x01, 0x0202, 0x02))
    attributePaths.add(matter.attributePath(0x01, 0x0202, 0x03))
    attributePaths.add(matter.attributePath(0x01, 0x0202, 0x0A))
    attributePaths.add(matter.attributePath(0x01, 0x0202, 0x0B))
    attributePaths.add(matter.attributePath(0x02, 0x0005, 0x01))
    attributePaths.add(matter.attributePath(0x02, 0x0006, 0x00))
    attributePaths.add(matter.attributePath(0x02, 0x0008, 0x00))
    attributePaths.add(matter.attributePath(0x02, 0x0300, 0x00))
    attributePaths.add(matter.attributePath(0x02, 0x0300, 0x01))
    attributePaths.add(matter.attributePath(0x02, 0x0300, 0x07))
//        attributePaths.add(matter.attributePath(0x01, 0x0300, 0x08))
    
    attributePaths.add(matter.attributePath(0x04, 0x0402, 0x00))
    attributePaths.add(matter.attributePath(0x06, 0x0406, 0x00))
    
    return matter.cleanSubscribe(0, 0xFFFF, attributePaths)
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


void sendToDevice(List<String> cmds, Integer delay = 300) {
    sendHubCommand(new hubitat.device.HubMultiAction(commands(cmds, delay), hubitat.device.Protocol.MATTER))
}

void sendToDevice(String cmd, Integer delay = 300) {
    sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.MATTER))
}

List<String> commands(List<String> cmds, Integer delay = 300) {
    return delayBetween(cmds.collect { it }, delay)
}
    
    
