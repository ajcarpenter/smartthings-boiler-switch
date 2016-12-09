/**
 *  Generic UPnP Device
 *
 *  Copyright 2016 SmartThings
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
metadata {
    definition (name: "Boiler Switch", namespace: "ajcarpenter", author: "Andrew Carpenter") {
        capability "Thermostat Mode"
        capability "Temperature Measurement"
        capability "Refresh"
        capability "Image Capture"
    }

    preferences {
        section {
            input title: "Temperature Offset", description: "This feature allows you to correct any temperature variations by selecting an offset. Ex: If your sensor consistently reports a temp that's 5 degrees too warm, you'd enter '-5'. If 3 degrees too cold, enter '+3'.", displayDuringSetup: false, type: "paragraph", element: "paragraph"
            input "tempOffset", "decimal", title: "Degrees", description: "Adjust temperature by this many degrees", displayDuringSetup: false
        }
    }

    simulator {
        // TODO: define status and reply messages here
    }

    tiles(scale: 2) {    
        standardTile("refresh", "device.temperatureMeasurement", inactiveLabel: false, width:2, height:2, decoration: "flat") {
            state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
        }
        
        carouselTile("cameraDetails", "device.image", width: 3, height: 2) { }
        standardTile("take", "device.image", width: 2, height: 2, canChangeIcon: false, inactiveLabel: true, canChangeBackground: false) {
            state "take", label: "Take", action: "Image Capture.take", icon: "st.camera.camera", backgroundColor: "#FFFFFF", nextState:"taking"
            state "taking", label:'Taking', action: "", icon: "st.camera.take-photo", backgroundColor: "#53a7c0"
            state "image", label: "Take", action: "Image Capture.take", icon: "st.camera.camera", backgroundColor: "#FFFFFF", nextState:"taking"
        }
        
        standardTile("toggleHeat", "device.thermostatMode", width: 2, height: 2) {
            state "off", label: "off", icon: "st.switches.switch.off", backgroundColor: "#ffffff", action: "heat"
            state "heat", label: "on", icon: "st.switches.switch.on", backgroundColor: "#79b821", action: "off"
        }
    
        // value tile (read only)
        valueTile("temperature", "device.temperature", decoration: "flat", width: 2, height: 2) {
            state "temperature", label:'${currentValue} C'
        }

        // the "switch" tile will appear in the Things view
        main("temperature")

        // the "switch" and "power" tiles will appear in the Device Details
        // view (order is left-to-right, top-to-bottom)
        details(["cameraDetails", "take", "refresh", "temperature"])
    }
}

// parse events into attributes
def parse(String description) {
    def response = parseLanMessage(description)
    def celsius = response?.json?.temperature_c
    def mode = response?.json?.mode
    def image = response?.headers?.location
    log.debug response
    def events = []
    
    if (celsius) {
        if (tempOffset) {
            celsius += tempOffset
        }
        events.push(createEvent(name:"temperature", value: celsius, unit: 'C'))
    }
    
    if (mode) {
        events.push(createEvent(name:"thermostatMode", value: mode))
    }
    
    if (image) {
        httpGet( image ){ imgResponse ->
            def id = new java.text.SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
            storeImage(id, imgResponse.data)
            sendEvent(name:"image", value: id)
        }
    }

    return events
}

def poll() {
    getTemperature()
}

def getTemperature() {
    new physicalgraph.device.HubAction(
        method: "GET",
        path: "/state",
        headers: [
            HOST: getHostAddress()
        ]
    )
}

def take() {
    def hubAction = new physicalgraph.device.HubAction(
        method: "GET",
        path: "/captureimage",
        headers: [
            HOST: getHostAddress()
        ]
    )
    return hubAction
}

def heat() {
    def json = new groovy.json.JsonBuilder()
    json.call("mode":"heat")
    
    new physicalgraph.device.HubAction(
        method: "PUT",
        path: "/state",
        body: json.toString(),
        headers: [
            "Content-Type": "application/json",
            HOST: getHostAddress()
        ]
    )
}

def setThermostatMode(mode) {
    switch(mode) {
        case "heat":
            return heat();
        case "off":
            return off();
    }
}

def off() {
    def json = new groovy.json.JsonBuilder()
    json.call("mode":"off")
    
    new physicalgraph.device.HubAction(
        method: "PUT",
        path: "/state",
        body: json.toString(),
        headers: [
            "Content-Type": "application/json",
            HOST: getHostAddress()
        ]
    )
}

def refresh() {
    getTemperature()
}

def getHostAddress() {
    def ip = getDataValue("ip")
    def port = getDataValue("port")
    
    return convertHexToIP(ip) + ":" + convertHexToInt(port)
}

private Integer convertHexToInt(hex) {
    return Integer.parseInt(hex,16)
}

private String convertHexToIP(hex) {
    return [convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}

def sync(ip, port) {
    def existingIp = getDataValue("ip")
    def existingPort = getDataValue("port")
    if (ip && ip != existingIp) {
        updateDataValue("ip", ip)
    }
    if (port && port != existingPort) {
        updateDataValue("port", port)
    }
}