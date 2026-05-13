# Wits AISnakeAgent

This project contains a Java snake agent for the SnakeRunner environment.

## Project Structure

```text
project-root/
│
├── .vscode/
│   └── launch.json
│
├── src/
│   └── MyAgent.java
│
├── lib/
     └── SnakeRunner.jar
```

---

## Requirements

Before running the project, make sure you have:

- Java JDK 17+ installed
- VS Code installed
- VS Code Extension Pack for Java installed

Recommended VS Code extensions:
- Extension Pack for Java
- Debugger for Java

---

## Opening the Project

1. Open VS Code
2. Select **File → Open Folder**
3. Open the project root folder

---

## Compile the Agent

Open the VS Code terminal and run:

```bash
javac -cp "lib/SnakeRunner.jar" -d . src/MyAgent.java
```

This compiles the agent using the SnakeRunner library.

---

## Running in VS Code

The project already includes a debug configuration in:

```text
.vscode/launch.json
```

Configuration:

```json
{
    "version": "0.2.0",
    "configurations": [
        {
            "type": "java",
            "name": "Launch MyAgent",
            "request": "launch",
            "mainClass": "MyAgent",
            "vmArgs": "-cp lib/SnakeRunner.jar",
            "args": "-develop"
        }
    ]
}
```

### To Run

1. Open the **Run and Debug** panel in VS Code
2. Select **Launch MyAgent**
3. Click the green **Run** button

---

## Exporting the Agent JAR

After compiling, create the agent JAR using:

```bash
jar cfe myAgent.jar MyAgent MyAgent.class
```

---

## Running the Exported JAR

You can run the exported agent with:

```bash
java -jar myAgent.jar
```

---

## Notes

- Ensure `SnakeRunner.jar` remains inside the `lib/` folder.
- The class name in `launch.json` must match the Java class name exactly.
- If your file is named `myAgent.java`, rename it to:

```text
MyAgent.java
```

because Java class names and file names are case-sensitive.
