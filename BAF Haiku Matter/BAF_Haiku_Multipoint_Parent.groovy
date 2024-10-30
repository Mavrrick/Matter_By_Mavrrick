/*
	BAF Haiku H/I (Matter) Parent

	Copyright 2023 Hubitat Inc.  All Rights Reserved

	2024-10-25 0.0.1 mavrrick
		-initial pub
    
    Built for Haiku H/I Series Fan from Big Ass Fans

*/

import groovy.transform.Field

@Field static final String   DEVICE_TYPE = 'MATTER_FAN'
@Field static final String   FAN_ENDPOINT = '01'
@Field static final String   CT_ENDPOINT = '02'
@Field static final String   TEMP_ENDPOINT= '04'
@Field static final String   OCCUPANCY_ENDPOINT= '06'


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
    definition (name: "BAF Haiku H/I Multiendpoint (Parent)", namespace: "Mavrrick", author: "Mavrrick") {
        capability "Configuration"
        capability "Initialize"
        capability "Refresh"        
        
        fingerprint endpointId:"06", inClusters:"0003,001D,0406", outClusters:"", model:"Haiku H/I Series", manufacturer:"Big Ass Fans", controllerType:"MAT"

    }
    preferences {
//        input(name:"cycleInterval", type:"number", title:"Number of seconds between cycles", defaultValue:30)
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
    device = getChildDevice("${device.deviceNetworkId}-${FAN_ENDPOINT}")
    device.sendEvent(name:"Fan_Mode", value:value, descriptionText:descriptionText)
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
    device = getChildDevice("${device.deviceNetworkId}-${FAN_ENDPOINT}")
    device.sendEvent(name:"speed", value:value, descriptionText:descriptionText)
}

private void sendFanDirEvent(String rawValue) {
    if (rawValue == "00") {
        String descriptionText = "${device.displayName} airFlow direction is forward direction"
        if (txtEnable) log.info descriptionText
        device = getChildDevice("${device.deviceNetworkId}-${FAN_ENDPOINT}")
        device.sendEvent(name:"Airflow_Direction", value:"forward", descriptionText:descriptionText)
    } else if (rawValue == "01"){
        String descriptionText = "${device.displayName} airFlow direction is reverse direction"
        if (txtEnable) log.info descriptionText
        device = getChildDevice("${device.deviceNetworkId}-${FAN_ENDPOINT}")
        device.sendEvent(name:"Airflow_Direction", value:"reverse", descriptionText:descriptionText)
    }
}

private void sendWindMode(String rawValue) {
    if (rawValue == "00") {
        String descriptionText = "${device.displayName} wind mode is Normal"
        if (txtEnable) log.info descriptionText
        device = getChildDevice("${device.deviceNetworkId}-${FAN_ENDPOINT}")
        device.sendEvent(name:"windMode", value:"Normal", descriptionText:descriptionText)
    } else if (rawValue == "02"){
        String descriptionText = "${device.displayName} wind mode is whoosh"
        if (txtEnable) log.info descriptionText
        device = getChildDevice("${device.deviceNetworkId}-${FAN_ENDPOINT}")
        device.sendEvent(name:"windMode", value:"Whoosh", descriptionText:descriptionText)
    }
}

private void sendSwitchEvent(String rawValue) {
    String value = rawValue == "01" ? "on" : "off"
    if (device.currentValue("switch") == value) return
    String descriptionText = "${device.displayName} was turned ${value}"
    if (txtEnable) log.info descriptionText
    device = getChildDevice("${device.deviceNetworkId}-${CT_ENDPOINT}")
    device.sendEvent(name:"switch", value:value, descriptionText:descriptionText)
}

private void sendOccupancyEvent(String rawValue) {
    Integer value = hexStrToUnsignedInt(rawValue)
    if (logEnable) log.debug "Ocupancy value provided is ${rawValue} converted is ${value}"
    if (value == 1) {
        String descriptionText = "${device.displayName} is present"
        if (txtEnable) log.info descriptionText
        device = getChildDevice("${device.deviceNetworkId}-${OCCUPANCY_ENDPOINT}")
        device.sendEvent(name:"presence", value:"present", descriptionText:descriptionText)
    } else {
        String descriptionText = "${device.displayName} is not present"
        if (txtEnable) log.info descriptionText
        device = getChildDevice("${device.deviceNetworkId}-${OCCUPANCY_ENDPOINT}")
        device.sendEvent(name:"presence", value:"not present", descriptionText:descriptionText)
    }
}

private void sendTempEvent(String rawValue) {
    Integer value = hexStrToUnsignedInt(rawValue)
    tempValue = ((value/100)*1.8+32)
    String descriptionText = "${device.displayName} temperature is ${tempValue}"
    if (txtEnable) log.info descriptionText
    device = getChildDevice("${device.deviceNetworkId}-${TEMP_ENDPOINT}")
    device.sendEvent(name:"temperature", value:tempValue, descriptionText:descriptionText, unit: "")
}

private void sendLevelEvent(String rawValue) {
    Integer value = Math.round(hexStrToUnsignedInt(rawValue) / 2.55)
    if (value == 0 || value == device.currentValue("level")) return
    String descriptionText = "${device.displayName} level was set to ${value}%"
    if (txtEnable) log.info descriptionText
    device = getChildDevice("${device.deviceNetworkId}-${CT_ENDPOINT}")
    device.sendEvent(name:"level", value:value, descriptionText:descriptionText, unit: "%")
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
        device = getChildDevice("${device.deviceNetworkId}-${CT_ENDPOINT}")
        device.sendEvent(name:"colorTemperature", value:value, descriptionText:descriptionText, unit: "K")
        device.sendEvent(name: "colorMode", value: "CT") 
        }
    }
}

void configure() {
    log.warn "configure..."
    sendToDevice(cleanSubscribeCmd())
    sendToDevice(subscribeCmd())
    sendToDevice(matter.getMatterEndpoints())
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
    sendEvent(name: "supportedFanSpeeds", value: groovy.json.JsonOutput.toJson(getFanLevel.collect {k,v -> k}))    
//    initializeVars(fullInit = true)
    sendToDevice(cleanSubscribeCmd())
    sendToDevice(subscribeCmd())
    childDNI = getChildDevices().deviceNetworkId
    log.info "Initialize(): Child DNI's are ${childDNI}"
    if (childDNI.contains("${device.deviceNetworkId}-${FAN_ENDPOINT}") == false) {
        addFanDeviceHelper()
    }
    if (childDNI.contains("${device.deviceNetworkId}-${CT_ENDPOINT}") == false) {
        addLightDeviceHelper()
    }
    if (childDNI.contains("${device.deviceNetworkId}-${TEMP_ENDPOINT}") == false) {
        addTempDeviceHelper()
    }
    if (childDNI.contains("${device.deviceNetworkId}-${OCCUPANCY_ENDPOINT}") == false) {
        addOccupyDeviceHelper()
    }
}

void refresh() {
    if (logEnable) log.debug "refresh()"
    sendToDevice(refreshCmd())
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
        attributePaths.add(matter.attributePath(0x01, 0x0005, 0x01))
        attributePaths.add(matter.attributePath(0x01, 0x0006, 0x00))
        attributePaths.add(matter.attributePath(0x01, 0x0008, 0x00))
        attributePaths.add(matter.attributePath(0x01, 0x0300, 0x00))
        attributePaths.add(matter.attributePath(0x01, 0x0300, 0x01))
        attributePaths.add(matter.attributePath(0x01, 0x0300, 0x07))
//        attributePaths.add(matter.attributePath(0x01, 0x0300, 0x08))
    
        attributePaths.add(matter.attributePath(0x01, 0x0402, 0x00))
        attributePaths.add(matter.attributePath(0x01, 0x0406, 0x00))
    
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
    attributePaths.add(matter.attributePath(0x01, 0x0005, 0x01))
    attributePaths.add(matter.attributePath(0x01, 0x0006, 0x00))
    attributePaths.add(matter.attributePath(0x01, 0x0008, 0x00))
    attributePaths.add(matter.attributePath(0x01, 0x0300, 0x00))
    attributePaths.add(matter.attributePath(0x01, 0x0300, 0x01))
    attributePaths.add(matter.attributePath(0x01, 0x0300, 0x07))
//        attributePaths.add(matter.attributePath(0x01, 0x0300, 0x08))
    
    attributePaths.add(matter.attributePath(0x01, 0x0402, 0x00))
    attributePaths.add(matter.attributePath(0x01, 0x0406, 0x00))
    
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

//
// Create Child Devices
//

void addFanDeviceHelper() {
	//Driver Settings
	Map deviceType = [namespace:"Mavrrick", typeName: "BAF Haiku H/I Fan - Child"]
	Map deviceTypeBak = [:]
	String devModel = deviceModel
    String dni = "${device.deviceNetworkId}-${FAN_ENDPOINT}"
	Map properties = [endpointId: FAN_ENDPOINT, label: "BAF Haiku H/I Fan - Child"]
	if (debugLog) "Creating Child Fan Device"

	def childDev
	try {
		childDev = addChildDevice(deviceType.namespace, deviceType.typeName, dni, properties)
	}
	catch (e) {
		log.warn "The '${deviceType}' driver failed"
		if (deviceTypeBak) {
			logWarn "Defaulting to '${deviceTypeBak}' instead"
			childDev = addChildDevice(deviceTypeBak.namespace, deviceTypeBak.typeName, dni, properties)
		}
	}
}

void addLightDeviceHelper() {
	//Driver Settings
	Map deviceType = [namespace:"Mavrrick", typeName: "BAF Haiku H/I Light - Child"]
	Map deviceTypeBak = [:]
	String devModel = deviceModel
    String dni = "${device.deviceNetworkId}-${CT_ENDPOINT}"
	Map properties = [endpointId: CT_ENDPOINT, label: "BAF Haiku H/I Light - Child"]
	if (debugLog) "Creating Child Light Device"

	def childDev
	try {
		childDev = addChildDevice(deviceType.namespace, deviceType.typeName, dni, properties)
	}
	catch (e) {
		log.warn "The '${deviceType}' driver failed"
		if (deviceTypeBak) {
			logWarn "Defaulting to '${deviceTypeBak}' instead"
			childDev = addChildDevice(deviceTypeBak.namespace, deviceTypeBak.typeName, dni, properties)
		}
	}
}

void addTempDeviceHelper() {
	//Driver Settings
	Map deviceType = [namespace:"Mavrrick", typeName: "BAF Haiku H/I Temp - Child"]
	Map deviceTypeBak = [:]
	String devModel = deviceModel
    String dni = "${device.deviceNetworkId}-${TEMP_ENDPOINT}"
	Map properties = [endpointId: TEMP_ENDPOINT, label: "BAF Haiku H/I Temp - Child"]
	if (debugLog) "Creating Child Light Device"

	def childDev
	try {
		childDev = addChildDevice(deviceType.namespace, deviceType.typeName, dni, properties)
	}
	catch (e) {
		log.warn "The '${deviceType}' driver failed"
		if (deviceTypeBak) {
			logWarn "Defaulting to '${deviceTypeBak}' instead"
			childDev = addChildDevice(deviceTypeBak.namespace, deviceTypeBak.typeName, dni, properties)
		}
	}
}

void addOccupyDeviceHelper() {
	//Driver Settings
	Map deviceType = [namespace:"Mavrrick", typeName: "BAF Haiku H/I Occupancy - Child"]
	Map deviceTypeBak = [:]
	String devModel = deviceModel
    String dni = "${device.deviceNetworkId}-${OCCUPANCY_ENDPOINT}"
	Map properties = [endpointId: OCCUPANCY_ENDPOINT, label: "BAF Haiku H/I Occupancy - Child"]
	if (debugLog) "Creating Child Light Device"

	def childDev
	try {
		childDev = addChildDevice(deviceType.namespace, deviceType.typeName, dni, properties)
	}
	catch (e) {
		log.warn "The '${deviceType}' driver failed"
		if (deviceTypeBak) {
			logWarn "Defaulting to '${deviceTypeBak}' instead"
			childDev = addChildDevice(deviceTypeBak.namespace, deviceTypeBak.typeName, dni, properties)
		}
	}
}