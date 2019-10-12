#!/usr/bin/env python3

import sys
import logging
import asyncio
import aiohttp
import time
import json


neighbors = {'Goloman': {'Hands': 12733, 'Holiday': 12734, 'Wilkes':12736},
                 'Hands':{'Wilkes': 12736, 'Goloman': 12732},
                 'Holiday':{'Welsh':12735, 'Wilkes': 12736, 'Goloman': 12732},
                 'Welsh':{'Holiday': 12734}, 'Wilkes':{'Holiday': 12734, 'Hands': 12733, 'Goloman': 12732} }

SERVERS = {'12732': 'Goloman', '12733': 'Hands', '12734': 'Holiday', '12736': 'Wilkes', '12735': 'Welsh'} 

logging.basicConfig(level=logging.INFO, filename=(sys.argv[1]+'.log'), filemode='w',
                    format='%(asctime)s  - %(message)s')


async def spread_message(server_name, client_id, location, client_time):
    for x in neighbors[server_name]:
     
     try:
        logging.info(f'Attempting to Connect with server {x}')
        if await asyncio.open_connection('127.0.0.1', neighbors[server_name][x]):
            logging.info(f'Successful Connection with server {x}')
            reader,writer = await asyncio.open_connection('127.0.0.1', neighbors[server_name][x])
            message = client_id + ' ' + location + ' ' + client_time
            writer.write(message.encode())
            writer.write_eof()
            writer.close()
     except Exception:
         logging.info(f'Failed Connection with server {x}')
         continue


client_info = {}

async def handle_connection(reader, writer):
    s = time.perf_counter()
    data = await reader.read(-1)
    elapsed = time.perf_counter() - s
    message = data.decode()
    addr = writer.get_extra_info('sockname')

    if message != "":
        logging.info(f"Input: {message}")

    if "IAMAT" in message:
        client = message.split()
        client_id = client[1]
        client_location = client[2]
        client_time = client[3]

        for i,c in enumerate(client_location[1:]):
            if c == '-' or c == '+':
                lat = client_location[:i]
                lon = client_location[i+1:]


        message = ' '.join(client[1:])
        client_info[client[1]] = {'lat':lat, 'lon': lon, 'info':message}

        location = lat + ' ' + lon
        index = str(addr[1])
        await spread_message(SERVERS[index], client_id, location, client_time)

        if elapsed > 0 :
            elapsed_time = "+" + str(elapsed)
    
        else:
            elapsed_time = "-" + str(elapsed)
    
    
        print(f"AT {SERVERS[index]} {elapsed_time} {message}")
        logging.info(f"Output: AT {SERVERS[index]} {elapsed_time} {message}")
        writer.write(data)
        writer.write_eof()
        await writer.drain()
        writer.close()


    elif "WHATSAT" in message:

      client = message.split()
      if not (client[1] in client_info.keys()):
          writer.write(f"? {message}".encode())
          writer.write_eof()
          await writer.drain()
          logging.info(f'Error: ? {message}')

      else:
          latitude = client_info[client[1]]['lat']
          longtitude = client_info[client[1]]['lon']
          message = client_info[client[1]]['info']
          Radius = client[2]
          bound = int(client[3])
          index = str(addr[1])

          if elapsed > 0 :
              elapsed_time = "+" + str(elapsed)

          else:
              elapsed_time = "-" + str(elapsed)

          print(f"AT {SERVERS[index]} {elapsed_time} {message}")
          logging.info(f"Output: AT {SERVERS[index]} {elapsed_time} {message}")
          writer.write(data)
          writer.write_eof()
          await writer.drain()
    
          async with aiohttp.ClientSession() as client:
              async with client.get(f'https://maps.googleapis.com/maps/api/place/nearbysearch/json?location={latitude},{longtitude}&radius={Radius}&key=AIzaSyAzBY9Ri_6Yu3rd2Vy6NMqPVNLWzqbZL3U') as resp:
                  assert resp.status == 200
                  result = await resp.text()
                  text = json.loads(result)
                  text.update({'results':text['results'][:bound]})
                  log = json.dumps(text,indent=4)
                  print(json.dumps(text, indent=4))
                  logging.info(log)

          writer.close()

    else:
        try:
            news = message.split()    
            if not(news[0] in client_info.keys()):
                location = news[1] + news[2]
                info = news[0] + ' ' + location + ' ' + news[3]
                client_info.update({news[0] : {'lat':news[1], 'lon':news[2], 'info':info}})
                loca = news[1] + ' ' + news[2]
                await spread_message(sys.argv[1], news[0], loca, news[3])
    
            elif (news[0] + ' ' + news[1]+news[2] + ' ' + news[3]) != client_info[news[0]]['info']:
                location = news[1] + news[2]
                info = news[0] + ' ' + location + ' ' + news[3]
                client_info.update({news[0] : {'lat':news[1], 'lon':news[2], 'info':info}})
                loca = news[1] + ' ' + news[2]
                await spread_message(sys.argv[1], news[0], loca, news[3])

        except IndexError:
            logging.info(f'Disconnceting from other servers.')
            pass


async def main(server_name):


    if server_name == 'Goloman':
      server = await asyncio.start_server(
          handle_connection, '127.0.0.1', 12732)

    elif server_name == 'Hands':
      server = await asyncio.start_server(
          handle_connection, '127.0.0.1', 12733)

    elif server_name == 'Holiday':
      server = await asyncio.start_server(
          handle_connection, '127.0.0.1', 12734)

    elif server_name == 'Welsh':
      server = await asyncio.start_server(
          handle_connection, '127.0.0.1', 12735)

    elif server_name == 'Wilkes':
      server = await asyncio.start_server(
          handle_connection, '127.0.0.1', 12736)

    else:
      print('No such server exists.')
      sys.exit(1)

    addr = server.sockets[0].getsockname()
    print(f'Serving on {addr}')


    try:
        async with server:
            await server.serve_forever()
    except KeyboardInterrupt:
        server.close()

if __name__ == '__main__':
    asyncio.run(main(sys.argv[1]))
