import sys
import config
import re
from twisted.internet import reactor, task
from twisted.internet.protocol import DatagramProtocol


SSDP_ADDR = '239.255.255.250'
SSDP_PORT = 1900

class Base(DatagramProtocol):
    def datagramReceived(self, datagram, address):
        first_line = datagram.rsplit('\r\n')[0]
        print datagram

    def stop(self):
        pass

class Client(Base):
    def __init__(self, desc_location, usn, st):
        self.ssdp = reactor.listenMulticast(SSDP_PORT, self, listenMultiple=True)
        self.ssdp.setLoopbackMode(1)
        self.ssdp.joinGroup(SSDP_ADDR)
        self.desc_location = desc_location
        self.usn = usn
        self.st = st
        reactor.run()

    def datagramReceived(self, datagram, address):
        matches = re.search(r"M-SEARCH.*[^.]*MX:[\s]?(.*)[^.]*MAN:[\s]?\"([^\"]*)\"[^.]*HOST:[\s]?(.*)[^.]*ST:[\s]?(.*)", datagram)
        if matches:
            mx, man, host, st = (i.strip() for i in matches.group(1, 2, 3, 4))
            if (st == self.st and man == "ssdp:discover"):
                response = (
                    "HTTP/1.1 200 OK\r\n"
                    "CACHE-CONTROL: max-age=100\r\n"
                    "EXT: \r\n"
                    "LOCATION: %s\r\n"
                    "ST: %s\r\n"
                    "USN: %s\r\n"
                ) % (self.desc_location, self.st, self.usn)

                self.ssdp.write(response, address)

    def stop(self):
        self.ssdp.leaveGroup(SSDP_ADDR)
        self.ssdp.stopListening()

def main(mode):
    obj = Client()
    reactor.addSystemEventTrigger('before', 'shutdown', obj.stop)
    reactor.callWhenRunning(main, mode)
    reactor.run()