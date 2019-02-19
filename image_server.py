# Import packages
import os
import sys
import collections
import time
import numpy as np
import socket

sys.path.append("..")

port = 60000  # Reserve a port for your service.
s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)  # Create a socket object
s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)  # Create a socket object
s.bind(('', port))  # Bind to the port
s.listen(5)  # Now wait for client connection. max 3 connections

print('Server is ready and listening....')

while True:
    conn, addr = s.accept()  # Establish connection with client.
    print('\nconnection from', addr)
    recv_data = conn.recv(1024)
    # print(recv_data)

    txt = recv_data.decode()  # JK, recv_data format example: [size n .jpg]
    if txt.startswith('size'):
        tmp = txt.split()
        size = int(tmp[1])
        file_format = tmp[2]

        conn.sendall("request image\n".encode())

        print('receiving image file...')
        file_name = 'image_file' + file_format
        image_file = open(file_name, 'wb')
        length = 0
        while (True):
            recv_data = conn.recv(1024)
            image_file.write(recv_data)
            length = length + len(recv_data)
            if (length >= size):
                break

        image_file.close()

        print('image file recv done, sending ack')
        conn.sendall("ok\n".encode())

        print('processing scanning...')

        brand = 'tide'
        fl_value = 200
        liter_value = 2
        load_value = 50

        network_data = 'Brand:' + brand + ' Volume:' + str(fl_value) + 'FLOZ (' + str(
            liter_value) + 'liter) Load:' + str(load_value) + "\n"
        print(network_data)

        conn.sendall(network_data.encode())

        print('waiting result...')

        while (True):
            try:
                recv_data = conn.recv(1024)
                txt = recv_data.decode()
                if txt.startswith('ok'):
                    print('send success, socket close')
                    conn.close()
                    break
                else:
                    print('resend result')
                    conn.sendall(network_data.encode())
            except:
                print("Wrong received data")

    else:
        print('wrong data received, format is size xxx .ext - example: size: 500000 .jpg\n')
        conn.close()
