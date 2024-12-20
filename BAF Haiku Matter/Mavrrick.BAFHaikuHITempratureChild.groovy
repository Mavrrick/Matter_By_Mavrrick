/*
	BAF Haiku H/I Temperature - Child

	Copyright 2023 Hubitat Inc.  All Rights Reserved

	2024-10-25 0.0.1 mavrrick
		-initial pub
    
    Built for Haiku H/I Series Fan from Big Ass Fans

*/

@Field static final String   DEVICE_TYPE = 'MATTER_TEMPERATURE'
@Field static final String  IMPORT_URL =  'https://raw.githubusercontent.com/Mavrrick/Matter_By_Mavrrick/refs/heads/main/BAF%20Haiku%20Matter/Mavrrick.BAFHaikuHITempChild.groovy'


import groovy.transform.Field
import hubitat.helper.HexUtils

metadata {
    definition (name: "BAF Haiku H/I Temprature - Child", namespace: "Mavrrick", author: "Mavrrick", importUrl: IMPORT_URL) {
        capability "TemperatureMeasurement"
        capability "Refresh"

    }
    preferences {
        input(name:"logEnable", type:"bool", title:"Enable debug logging", defaultValue:false)
        input(name:"txtEnable", type:"bool", title:"Enable descriptionText logging", defaultValue:true)
    }
}

//capability commands

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
}

void refresh() {
    if (logEnable) log.debug "refresh()"
    parent.sendToDevice(refreshCmd())
}

String refreshCmd() {
    List<Map<String, String>> attributePaths = []
      
        attributePaths.add(matter.attributePath(device.getDataValue("endpointId"), 0x0402, 0x0000))        //Temperature mesurement
    
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

List<String> commands(List<String> cmds, Integer delay = 300) {
    return delayBetween(cmds.collect { it }, delay)
}
    
    
