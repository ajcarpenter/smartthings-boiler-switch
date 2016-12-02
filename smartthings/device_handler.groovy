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
    }

    simulator {
        // TODO: define status and reply messages here
    }

    tiles(scale: 2) {    
        standardTile("refresh", "device.temperatureMeasurement", inactiveLabel: false, width:2, height:2, decoration: "flat") {
            state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
        }
        
        standardTile("toggleHeat", "device.thermostatMode", width: 2, height: 2) {
            state "off", label: "off", icon: "st.switches.switch.off", backgroundColor: "#ffffff", action: "heat"
            state "heat", label: "on", icon: "st.switches.switch.on", backgroundColor: "#79b821", action: "off"
        }
    
        // value tile (read only)
        valueTile("temperatureMeasurement", "device.temperatureMeasurement", decoration: "flat", width: 2, height: 2) {
            state "temperatureMeasurement", label:'${currentValue} C'
        }

        // the "switch" tile will appear in the Things view
        main("temperatureMeasurement")

        // the "switch" and "power" tiles will appear in the Device Details
        // view (order is left-to-right, top-to-bottom)
        details(["thermostatFull", "refresh", "temperatureMeasurement", "toggleHeat"])
    }
}

// parse events into attributes
def parse(String description) {
    def response = parseLanMessage(description)
    def celsius = response.json.temperature_c
    def mode = response.json.mode
    return [
        createEvent(name:"temperatureMeasurement", value: celsius),
        createEvent(name:"thermostatMode", value: mode)
    ]
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