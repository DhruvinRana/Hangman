# Hangman - Multiplayer Game

A fun and interactive multiplayer Hangman game built in Java with a client-server architecture. Players can join rooms, compete against each other, and enjoy the classic word-guessing game with modern networking features.

## 🎮 Features

### Core Gameplay
- **Multiplayer Support**: Play with friends in real-time across different rooms
- **Word Guessing**: Classic hangman mechanics with letter-by-letter guessing
- **Turn-based System**: Players take turns guessing letters or words
- **Strike System**: Track wrong guesses with a visual hangman display
- **Points System**: Earn points for correct guesses and word completions
- **Multiple Rounds**: Play up to 5 rounds per session

### Game Modes
- **Normal Mode**: Standard hangman gameplay
- **Hard Mode**: More challenging with additional restrictions
- **Spectator Mode**: Watch games without participating
- **Away Status**: Mark yourself as away when needed

### Room Management
- **Create Rooms**: Start new game rooms with custom names
- **Join Rooms**: Enter existing rooms to play with others
- **Lobby System**: Browse available rooms before joining
- **Room List**: See all active rooms and their status

### User Interface
- **Graphical Interface**: Clean, modern Swing-based UI
- **Chat System**: Communicate with other players
- **Real-time Updates**: Live game state synchronization
- **Visual Feedback**: Color-coded messages and status indicators

## 🏗️ Architecture

### Client-Server Model
- **Server**: Handles game logic, room management, and player coordination
- **Client**: Provides user interface and communicates with server
- **Network Protocol**: Custom payload-based communication system

### Key Components

#### Server Side
- `Server.java`: Main server entry point and connection management
- `GameRoom.java`: Game logic implementation and state management
- `ServerThread.java`: Individual client connection handling
- `Room.java`: Base room functionality

#### Client Side
- `ClientUI.java`: Main application window and UI management
- `Client.java`: Network communication and server interaction
- `GamePanel.java`: Game interface and user interaction
- `ChatPanel.java`: Chat functionality and message display

#### Common
- `Payload.java`: Network message structure
- `User.java`: Player data model
- `Constants.java`: Shared configuration values

## 🚀 Getting Started

### Prerequisites
- Java 8 or higher
- Network connectivity for multiplayer features

### Running the Server
```bash
# Navigate to the Project directory
cd Project

# Compile the server
javac -cp . Server/Server.java

# Run the server (default port 3000)
java -cp . Server.Server

# Or specify a custom port
java -cp . Server.Server 3001
```

### Running the Client
```bash
# In a new terminal, compile the client
javac -cp . Client/ClientUI.java

# Run the client
java -cp . Client.ClientUI
```

### Game Setup
1. **Start the server** first (see above)
2. **Launch the client** application
3. **Connect to server** using the connection panel
4. **Set your username** with `/name YourName`
5. **Create or join a room** to start playing

## 🎯 How to Play

### Basic Commands
- `/name <name>` - Set your display name
- `/create <room>` - Create a new game room
- `/join <room>` - Join an existing room
- `/list` - List all available rooms
- `/ready` - Mark yourself as ready to play
- `/quit` - Disconnect from the server

### Game Rules
1. **Word Selection**: Server randomly selects words from a dictionary
2. **Letter Guessing**: Players take turns guessing letters
3. **Word Guessing**: Players can attempt to guess the complete word
4. **Strikes**: Wrong guesses add strikes (max 6)
5. **Scoring**: Points awarded for correct letters and word completions
6. **Rounds**: Games consist of multiple rounds with score tracking

### Scoring System
- **Correct Letter**: Points based on letter frequency
- **Word Completion**: Bonus points for guessing the entire word
- **Round Bonus**: Additional points for winning rounds

## 🔧 Configuration

### Server Options
- **Port**: Default 3000, configurable via command line
- **Word List**: Customizable word dictionary
- **Max Players**: Configurable room capacity
- **Timer Settings**: Adjustable turn and round timers

### Client Options
- **Hard Mode**: Enable additional game restrictions
- **Strike Removal**: Option to remove strikes on correct guesses
- **UI Themes**: Customizable interface appearance

## 🐛 Troubleshooting

### Common Issues
- **Connection Failed**: Ensure server is running and port is correct
- **Room Not Found**: Check room name spelling or create a new room
- **Game Not Starting**: Make sure all players are marked as ready

### Log Files
- `server.log`: Server-side activity and error logs
- `client-ui.log`: Client-side interface and connection logs

## 📁 Project Structure

```
Project/
├── Client/                 # Client-side application
│   ├── Views/             # UI components
│   ├── Interfaces/        # Event interfaces
│   └── ClientUI.java      # Main client application
├── Server/                # Server-side application
│   ├── GameRoom.java      # Game logic implementation
│   ├── Server.java        # Main server application
│   └── ServerThread.java  # Client connection handling
├── Common/                # Shared components
│   ├── Payload.java       # Network message structure
│   ├── User.java          # Player data model
│   └── Constants.java     # Shared constants
└── Exceptions/            # Custom exception classes
```

## 🤝 Contributing

This project was developed as part of a Java networking course. The codebase demonstrates:
- **Multi-threading**: Concurrent client handling
- **Network Programming**: Custom protocol implementation
- **Event-Driven Architecture**: Observer pattern for UI updates
- **Object-Oriented Design**: Clean separation of concerns

## 📄 License

This project is part of an educational assignment and is not intended for commercial use.

---

**Enjoy playing Hangman with friends!** 🎮✨