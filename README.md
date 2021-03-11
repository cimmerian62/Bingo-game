# Bingo-game
This project is a bingo game based on TCP connections. It allows a bingoserver to host multiple bingo clients in a bingo game.

After establishing a connection the client sends a message to the server telling its name

The server sends a message to all connected clients that x has joined

If there are enough clients connected the server begins a game method

The server sends five lines of five unique random numbers between 1 and 99 to each client that joined before the game started to create a bingo card

Every thirty seconds the server sends a random number to each client that joined before the game started and declares it to be the number

When a client has a bingo they send the string BINGO to the server

When a server receives BINGO from a client it sends a message to all clients that joined before the game started that x has won and stops the game method. If there are still enough clients thirty seconds later, then a new game begins

If a client sends the message QUIT to the server, it sends a message to all clients that x is leaving and then closes the connection

If too many clients quit during a game then the game will end and the server will send a message to the clients in the game that there weren’t enough players to continue
