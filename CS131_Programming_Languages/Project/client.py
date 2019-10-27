import logging
import asyncio
import aiohttp
import time, datetime

"""d = datetime.datetime.now()"""

"""print(time.mktime(d.timetuple()))"""


async def tcp_echo_client(message):

    reader, writer = await asyncio.open_connection(
        '127.0.0.1', 12732)

    print(f'Send: {message!r}')
    writer.write(message.encode())
    writer.write_eof()
    data = await reader.read(-1)
    print(f'Received: {data.decode()!r}')

    print('Close the connection')
    writer.close()

"""asyncio.run(tcp_echo_client('IAMAT wiki.cs.ucla.edu -33.8670522+151.1957362 1520023934.918963997'))"""
asyncio.run(tcp_echo_client('WHATSAT wiki.cs.ucla.edu 50 5'))
