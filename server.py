from flask import Flask, jsonify, request, abort,render_template, make_response
from switch import BoilerSwitch
from thermometer import read_temp
from crossdomain import crossdomain
from ssdp import Client
from multiprocessing import Process
from uuid import getnode as get_mac
import atexit
import config
import argparse

SSDP_ST = config.st
SSDP_USN = "uuid:%s::%s" % (hex(get_mac()), SSDP_ST)
SSDP_LOCATION = "http://192.168.0.9:5000/description.xml"

app = Flask(__name__)
boiler_switch = BoilerSwitch(GPIO_pin=config.wireless_tx_gpio_pin)

@app.route('/description.xml', methods=['GET'])
def get_ssdp_desc():
	xml = render_template('description.xml', st=SSDP_ST, usn=SSDP_USN)
	response = make_response(xml)
	response.headers["Content-Type"] = "application/xml"
	return response

@app.route('/state', methods=['GET'])
def get_state():
	response = jsonify({
		'mode': boiler_switch.get_state(),
		'temperature_c': read_temp()
	})

	return response

@app.route('/state', methods=['PUT', 'POST'])
def set_state():

	body = request.get_json(force=True)
	print body

	if (body['mode'] == 'heat'):
		boiler_switch.turn_on()
	elif (body['mode'] == 'off'):
		boiler_switch.turn_off()
	else:
		abort(400)

	response = jsonify({
		'mode': boiler_switch.get_state(),
		'temperature_c': read_temp()
	})

	return response

def cleanup():
	boiler_switch.cleanup()

if __name__ == '__main__':
	atexit.register(cleanup)

	parser = argparse.ArgumentParser()
	parser.add_argument('--iface', dest="iface", required=True)
	args = parser.parse_args()

	SSDP_LOCATION="http://%s:5000/description.xml" % args.iface

	ssdp_client = Process(target=Client, args=(SSDP_LOCATION, SSDP_USN, SSDP_ST))
	ssdp_client.start()
	app.run(debug=True, host='0.0.0.0', use_reloader=False);