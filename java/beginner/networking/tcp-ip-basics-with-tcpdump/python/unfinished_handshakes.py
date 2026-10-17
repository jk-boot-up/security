"""Reads a raw-IP pcap file and prints, one per line and sorted, every source address that sent a SYN and never sent the final ACK.

    python3 python/unfinished_handshakes.py capture.pcap

Standard library only. It is the same check as Capture.unfinishedHandshakes in the Java code, written independently.
"""
import socket
import struct
import sys


def packets(data):
    magic, = struct.unpack_from("<I", data, 0)
    if magic != 0xA1B2C3D4:
        raise SystemExit("not a little-endian pcap file")
    offset = 24
    while offset < len(data):
        _, _, captured, _ = struct.unpack_from("<IIII", data, offset)
        offset += 16
        yield data[offset:offset + captured]
        offset += captured


def main(path):
    with open(path, "rb") as handle:
        data = handle.read()
    finished = {}
    for raw in packets(data):
        header = (raw[0] & 0x0F) * 4
        source = socket.inet_ntoa(raw[12:16])
        source_port, = struct.unpack_from(">H", raw, header)
        flags = raw[header + 13]
        syn, ack = bool(flags & 0x02), bool(flags & 0x10)
        key = (source, source_port)
        if syn and not ack:
            finished.setdefault(key, False)
        elif ack and not syn and key in finished:
            finished[key] = True
    for source in sorted({k[0] for k, done in finished.items() if not done}, key=lambda a: tuple(int(p) for p in a.split("."))):
        print(source)


if __name__ == "__main__":
    main(sys.argv[1])
