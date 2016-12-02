from fabric.api import run, env, put, cd, sudo

env.hosts = ['pi@192.168.0.91']

def deploy():
	sudo('rm -rf /home/pi/boiler_switch')
	run('mkdir -p /home/pi/boiler_switch')
	put('.', '/home/pi/boiler_switch')
	with cd('/home/pi/boiler_switch'):
		sudo('pip install -r requirements.txt')