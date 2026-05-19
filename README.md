# CMSC 137 Submarine

Milestone 1 single-player foundation for a 2D cooperative submarine survival game.

## Project Structure

src/main/java/edu/cmsc137/submarine/
- GameLauncher.java
- core/GameState.java
- input/InputHandler.java
- ui/GamePanel.java

## Run Locally

Requires Java 21 or newer.

Compile:

```powershell
javac --release 21 -d out (Get-ChildItem -Recurse src\main\java\*.java | ForEach-Object FullName)
```

Run:

```powershell
java -cp out edu.cmsc137.submarine.GameLauncher
```

Controls:
- W A S D to move
- E to interact with task station