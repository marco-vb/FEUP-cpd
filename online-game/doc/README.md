# CPD - Distributed Systems project

## How to run the project

All the following commands assume that the terminal is in the root of the project. They work in UNIX operating systems.

### Compiling the project
Just run:
```bash
javac -d out src/main/pt/up/fe/cpd2324/**/*.java
```

Or:
```bash
make compile
```
### Running the server
Just run:

```bash
java -cp out pt.up.fe.cpd2324.server.Server
```

Or:
```bash
make server
```


### Running a client
Just run:
```bash
java -cp out pt.up.fe.cpd2324.client.Client
```

Or:
```bash
make client
```

#### Running a client with a username
Just run:

```bash
java -cp out pt.up.fe.cpd2324.client.Client <username>  # used for tokens
```

Or:
```bash
make client ARGS="<username>"
```

If the client is run with a username, it will look for its token file to authenticate the user, in case of a reconnect attempt.
The `<username>` argument is used to identify the token file of that user in the file system, inside the tokens' folder.


## Project description

This project consists of an implementation of a simple game in a distributed system, taking into consideration concurrency and fault tolerance.

### Game description

We decided to implement a simple game similar to [Nim](https://en.wikipedia.org/wiki/Nim).

In our game, there is a certain number of piles (or stacks), each with a certain number of rocks. The players take turns removing rocks from the piles. In each turn, a player can remove any number of rocks from a single pile. The player that removes the last rock from the last pile wins the game.

This is an example of a game with just 2 piles, and 2 players:

```
o
o o
o o
1 2
```
There are 3 rocks on the 1st pile and 2 on the 2nd pile. The numbers correspond to the number of each pile, and the `o` characters represent the rocks.

Here, if player 1 takes 3 rocks from the first pile, the second player can win by taking the last 2 rocks from the second pile, since he was the last player to remove rocks.

### Implemented features

- Game modes
  - Simple game mode, without ratings
  - Ranked game mode, where players are paired according to their ratings
- Fault tolerance
  - Clients that are disconnected in the queue can reconnect and not lose their spot
- Concurrency control
  - Usage of locks to control concurrent accesses (e.g., implementation of our own concurrent TreeSet)
- User registration and authentication
  - Secure storage of passwords using SHA-256 and salt
- Secure communication between players and server
  - Using SSL sockets

### Project Architecture

This is the project's directory structure:

```
src/main/pt/up/fe/cpd2324/
├── client
│   ├── Client.java
│   └── Player.java
├── common
│   ├── Connection.java
│   ├── Message.java
│   ├── TreeSet.java
│   └── Utils.java
├── game
│   └── Stones.java
├── queue
│   ├── NormalQueue.java
│   ├── Queue.java
│   ├── RankedQueue.java
│   └── Rateable.java
└── server
    ├── ClientAuthenticator.java
    ├── Database.java
    ├── Game.java
    ├── GameScheduler.java
    ├── QueueManager.java
    └── Server.java
```

The most important classes for understanding our architecture are located in the Server folder.

#### Client

---

- [`client/Client`](../src/main/pt/up/fe/cpd2324/client/Client.java): main class that starts the client and connects to the server. It's responsible for sending messages to the server and receiving responses.

- [`client/Player`](../src/main/pt/up/fe/cpd2324/client/Player.java): represents a player (an authenticated client) in the game, with a name, password, rating and the associated socket.

#### Common

---

- [`common/Message`](../src/main/pt/up/fe/cpd2324/common/Message.java): represents a message that can be sent between the client and the server. It has a type and content.

- [`common/Connection`](../src/main/pt/up/fe/cpd2324/common/Connection.java): class used to send and receive messages between the client and the server.

- [`common/TreeSet`](../src/main/pt/up/fe/cpd2324/common/TreeSet.java): implementation of a thread-safe TreeSet.

- [`common/Utils`](../src/main/pt/up/fe/cpd2324/common/Utils.java): simple class with general utility methods.

#### Queues

---

- [`queue/Queue`](../src/main/pt/up/fe/cpd2324/queue/Queue.java): abstract class that represents a queue of players waiting to play.

- [`queue/NormalQueue`](../src/main/pt/up/fe/cpd2324/queue/NormalQueue.java): implementation of a simple queue. Players are paired according to first come, first served.

- [`queue/Rateable`](../src/main/pt/up/fe/cpd2324/queue/Rateable.java): simple interface that represents an object that can be rated.

- [`queue/RankedQueue`](../src/main/pt/up/fe/cpd2324/queue/RankedQueue.java): implementation of a queue that pairs players according to their ratings. This queue consists of a series of 'buckets' that gathers players inside some rating range, and two players can only match up if they are in the same bucket. When a player joins the queue, it is placed in the bucket that corresponds to its rating range. To make sure no player waits forever in the queue because of rating disparity, when the queue has at least 2 players, the queue periodically enlarges bucket sizes, redistributing the players. This guarantees that after long enough time, a game will be found. After a game starts, the buckets are reset to their original size.

#### Game

---

- [`game/Stones`](../src/main/pt/up/fe/cpd2324/game/Stones.java): Holds the logic for building a game and making “moves” in the game, i.e., removing rocks.

#### Server

---

- [`server/Server`](../src/main/pt/up/fe/cpd2324/server/Server.java): Main class that starts the server and listens for connections on a certain port. It's responsible for creating threads to manage the queues and games, as well as handling connections from clients. It also creates a thread to ping players to check if they are still connected.

- [`server/ClientAuthenticator`](../src/main/pt/up/fe/cpd2324/server/ClientAuthenticator.java): created by the `Server` for each client. It is responsible for authenticating clients. Furthermore, it deals with reconnecting clients that were "unexpectedly" disconnected. To do this, it checks if there is a username as an argument of the program and if the correct token for that user is in the file system. Reconnects are only allowed when the client crashes (e.g. CTRL+C), not when exiting through the menu.

- [`server/Database`](../src/main/pt/up/fe/cpd2324/server/Database.java): Simulates a database, handles storing players and their information.

- [`server/Game`](../src/main/pt/up/fe/cpd2324/server/Game.java): Represents a game in the server between 2 players. It handles the game logic and interactions with the players. In case of a ranked game, it also updates the ratings of the players at the end of the game. Uses an instance of `game/Stones` to keep the game state and handle the more basic game logic.

- [`server/QueueManager`](../src/main/pt/up/fe/cpd2324/server/QueueManager.java): Responsible for adding players to the normal or ranked queues, according to the client's choice.

- [`server/GameScheduler`](../src/main/pt/up/fe/cpd2324/server/GameScheduler.java): Monitors the queues and starts games when there are enough players.

