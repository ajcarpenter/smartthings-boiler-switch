class DebugGPIO:
	BCM = None
	OUT = None
	LOW = 0
	HIGH = 1
	def setmode(self, mode):
		pass
	def setup(self, pin, mode):
		pass
	def cleanup(self):
		pass
	def output(self, pin, value):
		print('Outputting', value, 'on pin', pin)

try:
	import RPi.GPIO as GPIO
except ImportError:
	print("RPi.GPIO not found. Defaulting to debug mode")
	GPIO = DebugGPIO()

import time
import sys

class BoilerSwitch: 
	sample_rate = 1 / float(2400)
	control_sequences = {
		'on': [1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 0, 1, 1, 0, 1, 1, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0, 1, 1, 0, 0, 1, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 0, 1, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 0, 1, 0, 1, 1, 0, 1, 0, 1, 0, 0, 1],
		'off': [1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 0, 1, 1, 0, 1, 1, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0, 1, 1, 0, 0, 1, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 0, 1, 1, 0, 0, 1, 1,  0, 1, 0, 1, 0, 0, 1]
	}
	GPIO_pin = 4
	__state = None

	def __init__(self, GPIO_pin = 24):
		self.GPIO_pin = GPIO_pin
		GPIO.setmode(GPIO.BCM)
		GPIO.setup(self.GPIO_pin, GPIO.OUT)

	def main(self):
		{
			'on': self.turn_on,
			'off': self.turn_off
		}[sys.argv[1]]()

	def turn_on(self):
		self.__send_sequence(self.control_sequences['on'])
		self.__state = 'heat'

	def turn_off(self):
		self.__send_sequence(self.control_sequences['off'])
		self.__state = 'off'

	def get_state(self):
		return self.__state;

	def cleanup(self):
		print('CLEANING UP GPIO')
		GPIO.cleanup()

	def __send_sequence(self, sequence, count=3):
		for j in range(0, count):
			print('Starting pulse', j)
			for i in sequence:
				GPIO.output(self.GPIO_pin, GPIO.HIGH if (i != 1) else GPIO.LOW)
				time.sleep(self.sample_rate);
			time.sleep(1)

if __name__ == "__main__": BoilerSwitch().main()



