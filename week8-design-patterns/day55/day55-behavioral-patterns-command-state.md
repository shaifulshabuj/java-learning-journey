# Day 55: Behavioral Patterns - Command & State

## Learning Objectives
- Master the Command pattern for encapsulating requests
- Understand the State pattern for managing object state changes
- Learn when and how to apply these patterns
- Build practical examples with real-world scenarios

---

## 1. Command Pattern

The Command pattern encapsulates a request as an object, thereby letting you parameterize clients with different requests, queue or log requests, and support undoable operations.

### Key Concepts:
- **Command**: Interface declaring execute method
- **ConcreteCommand**: Binds receiver with action
- **Receiver**: Knows how to perform operations
- **Invoker**: Invokes command to carry out request
- **Client**: Creates concrete command objects

### Example 1: Smart Home Automation System

```java
// Command interface
interface Command {
    void execute();
    void undo();
}

// Receiver classes
class Light {
    private String location;
    private boolean isOn;
    
    public Light(String location) {
        this.location = location;
        this.isOn = false;
    }
    
    public void on() {
        isOn = true;
        System.out.println("💡 " + location + " light is ON");
    }
    
    public void off() {
        isOn = false;
        System.out.println("💡 " + location + " light is OFF");
    }
    
    public boolean isOn() {
        return isOn;
    }
}

class Fan {
    private String location;
    private int speed; // 0 = off, 1-3 = speed levels
    
    public Fan(String location) {
        this.location = location;
        this.speed = 0;
    }
    
    public void on() {
        speed = 1;
        System.out.println("🌪️ " + location + " fan is ON (Speed: " + speed + ")");
    }
    
    public void off() {
        speed = 0;
        System.out.println("🌪️ " + location + " fan is OFF");
    }
    
    public void increaseSpeed() {
        if (speed < 3) {
            speed++;
            System.out.println("🌪️ " + location + " fan speed increased to " + speed);
        }
    }
    
    public void decreaseSpeed() {
        if (speed > 0) {
            speed--;
            if (speed == 0) {
                System.out.println("🌪️ " + location + " fan is OFF");
            } else {
                System.out.println("🌪️ " + location + " fan speed decreased to " + speed);
            }
        }
    }
    
    public int getSpeed() {
        return speed;
    }
}

class Thermostat {
    private int temperature;
    
    public Thermostat() {
        this.temperature = 70; // Default temperature
    }
    
    public void setTemperature(int temperature) {
        this.temperature = temperature;
        System.out.println("🌡️ Thermostat set to " + temperature + "°F");
    }
    
    public int getTemperature() {
        return temperature;
    }
}

// Concrete Command classes
class LightOnCommand implements Command {
    private Light light;
    
    public LightOnCommand(Light light) {
        this.light = light;
    }
    
    @Override
    public void execute() {
        light.on();
    }
    
    @Override
    public void undo() {
        light.off();
    }
}

class LightOffCommand implements Command {
    private Light light;
    
    public LightOffCommand(Light light) {
        this.light = light;
    }
    
    @Override
    public void execute() {
        light.off();
    }
    
    @Override
    public void undo() {
        light.on();
    }
}

class FanOnCommand implements Command {
    private Fan fan;
    
    public FanOnCommand(Fan fan) {
        this.fan = fan;
    }
    
    @Override
    public void execute() {
        fan.on();
    }
    
    @Override
    public void undo() {
        fan.off();
    }
}

class FanOffCommand implements Command {
    private Fan fan;
    
    public FanOffCommand(Fan fan) {
        this.fan = fan;
    }
    
    @Override
    public void execute() {
        fan.off();
    }
    
    @Override
    public void undo() {
        fan.on();
    }
}

class ThermostatCommand implements Command {
    private Thermostat thermostat;
    private int newTemperature;
    private int previousTemperature;
    
    public ThermostatCommand(Thermostat thermostat, int temperature) {
        this.thermostat = thermostat;
        this.newTemperature = temperature;
    }
    
    @Override
    public void execute() {
        previousTemperature = thermostat.getTemperature();
        thermostat.setTemperature(newTemperature);
    }
    
    @Override
    public void undo() {
        thermostat.setTemperature(previousTemperature);
    }
}

// Macro Command - executes multiple commands
class MacroCommand implements Command {
    private Command[] commands;
    
    public MacroCommand(Command[] commands) {
        this.commands = commands;
    }
    
    @Override
    public void execute() {
        for (Command command : commands) {
            command.execute();
        }
    }
    
    @Override
    public void undo() {
        // Undo in reverse order
        for (int i = commands.length - 1; i >= 0; i--) {
            commands[i].undo();
        }
    }
}

// No Operation Command
class NoCommand implements Command {
    @Override
    public void execute() {}
    
    @Override
    public void undo() {}
}

// Invoker class
class RemoteControl {
    private Command[] onCommands;
    private Command[] offCommands;
    private Command lastCommand;
    
    public RemoteControl() {
        onCommands = new Command[7];
        offCommands = new Command[7];
        
        Command noCommand = new NoCommand();
        for (int i = 0; i < 7; i++) {
            onCommands[i] = noCommand;
            offCommands[i] = noCommand;
        }
        lastCommand = noCommand;
    }
    
    public void setCommand(int slot, Command onCommand, Command offCommand) {
        onCommands[slot] = onCommand;
        offCommands[slot] = offCommand;
    }
    
    public void onButtonPressed(int slot) {
        System.out.println("🔘 ON button pressed for slot " + slot);
        onCommands[slot].execute();
        lastCommand = onCommands[slot];
    }
    
    public void offButtonPressed(int slot) {
        System.out.println("🔘 OFF button pressed for slot " + slot);
        offCommands[slot].execute();
        lastCommand = offCommands[slot];
    }
    
    public void undoButtonPressed() {
        System.out.println("↩️ UNDO button pressed");
        lastCommand.undo();
    }
    
    public void displayStatus() {
        System.out.println(\"\\n📋 Remote Control Status:\");
        for (int i = 0; i < onCommands.length; i++) {
            System.out.println(\"[slot \" + i + \"] \" + onCommands[i].getClass().getSimpleName() + 
                             \" - \" + offCommands[i].getClass().getSimpleName());
        }
    }
}

// Usage
public class CommandPatternDemo {
    public static void main(String[] args) {
        // Create receiver objects
        Light livingRoomLight = new Light(\"Living Room\");
        Light bedroomLight = new Light(\"Bedroom\");
        Fan ceilingFan = new Fan(\"Ceiling\");
        Thermostat thermostat = new Thermostat();
        
        // Create command objects
        LightOnCommand livingRoomLightOn = new LightOnCommand(livingRoomLight);
        LightOffCommand livingRoomLightOff = new LightOffCommand(livingRoomLight);
        
        LightOnCommand bedroomLightOn = new LightOnCommand(bedroomLight);
        LightOffCommand bedroomLightOff = new LightOffCommand(bedroomLight);
        
        FanOnCommand fanOn = new FanOnCommand(ceilingFan);
        FanOffCommand fanOff = new FanOffCommand(ceilingFan);
        
        ThermostatCommand thermostatWarm = new ThermostatCommand(thermostat, 75);
        ThermostatCommand thermostatCool = new ThermostatCommand(thermostat, 68);
        
        // Create macro command for \"Party Mode\"
        Command[] partyOn = {livingRoomLightOn, bedroomLightOn, fanOn, thermostatCool};
        Command[] partyOff = {livingRoomLightOff, bedroomLightOff, fanOff, thermostatWarm};
        
        MacroCommand partyOnMacro = new MacroCommand(partyOn);
        MacroCommand partyOffMacro = new MacroCommand(partyOff);
        
        // Create remote control
        RemoteControl remote = new RemoteControl();
        
        // Set up commands
        remote.setCommand(0, livingRoomLightOn, livingRoomLightOff);
        remote.setCommand(1, bedroomLightOn, bedroomLightOff);
        remote.setCommand(2, fanOn, fanOff);
        remote.setCommand(3, thermostatWarm, thermostatCool);
        remote.setCommand(6, partyOnMacro, partyOffMacro);
        
        // Display remote status
        remote.displayStatus();
        
        // Test individual commands
        System.out.println(\"\\n=== Testing Individual Commands ===\");
        remote.onButtonPressed(0);
        remote.offButtonPressed(0);
        remote.undoButtonPressed();
        
        System.out.println();
        remote.onButtonPressed(2);
        remote.onButtonPressed(3);
        remote.undoButtonPressed();
        
        // Test macro command
        System.out.println(\"\\n=== Testing Macro Command (Party Mode) ===\");
        remote.onButtonPressed(6);
        System.out.println();
        remote.undoButtonPressed();
    }
}
```

### Example 2: Text Editor with Undo/Redo

```java
import java.util.*;

// Command interface
interface TextCommand {
    void execute();
    void undo();
}

// Receiver class
class TextEditor {
    private StringBuilder content;
    
    public TextEditor() {
        this.content = new StringBuilder();
    }
    
    public void write(String text) {
        content.append(text);
        System.out.println(\"📝 Added: '\" + text + \"'\");
        System.out.println(\"📄 Current content: '\" + content.toString() + \"'\");
    }
    
    public void delete(int length) {
        if (content.length() >= length) {
            content.delete(content.length() - length, content.length());
            System.out.println(\"🗑️ Deleted \" + length + \" characters\");
        }
        System.out.println(\"📄 Current content: '\" + content.toString() + \"'\");
    }
    
    public String getContent() {
        return content.toString();
    }
    
    public void setContent(String text) {
        this.content = new StringBuilder(text);
    }
}

// Concrete Commands
class WriteCommand implements TextCommand {
    private TextEditor editor;
    private String text;
    
    public WriteCommand(TextEditor editor, String text) {
        this.editor = editor;
        this.text = text;
    }
    
    @Override
    public void execute() {
        editor.write(text);
    }
    
    @Override
    public void undo() {
        editor.delete(text.length());
    }
}

class DeleteCommand implements TextCommand {
    private TextEditor editor;
    private int length;
    private String deletedText;
    
    public DeleteCommand(TextEditor editor, int length) {
        this.editor = editor;
        this.length = length;
    }
    
    @Override
    public void execute() {
        String content = editor.getContent();
        if (content.length() >= length) {
            deletedText = content.substring(content.length() - length);
            editor.delete(length);
        }
    }
    
    @Override
    public void undo() {
        if (deletedText != null) {
            editor.write(deletedText);
        }
    }
}

// Invoker class
class TextEditorInvoker {
    private Stack<TextCommand> undoStack;
    private Stack<TextCommand> redoStack;
    
    public TextEditorInvoker() {
        this.undoStack = new Stack<>();
        this.redoStack = new Stack<>();
    }
    
    public void executeCommand(TextCommand command) {
        command.execute();
        undoStack.push(command);
        redoStack.clear(); // Clear redo stack when new command is executed
    }
    
    public void undo() {
        if (!undoStack.isEmpty()) {
            TextCommand command = undoStack.pop();
            command.undo();
            redoStack.push(command);
            System.out.println(\"↩️ Undo executed\");
        } else {
            System.out.println(\"❌ Nothing to undo\");
        }
    }
    
    public void redo() {
        if (!redoStack.isEmpty()) {
            TextCommand command = redoStack.pop();
            command.execute();
            undoStack.push(command);
            System.out.println(\"↪️ Redo executed\");
        } else {
            System.out.println(\"❌ Nothing to redo\");
        }
    }
}

// Usage
public class TextEditorDemo {
    public static void main(String[] args) {
        TextEditor editor = new TextEditor();
        TextEditorInvoker invoker = new TextEditorInvoker();
        
        // Execute commands
        System.out.println(\"=== Text Editor Commands ===\");
        invoker.executeCommand(new WriteCommand(editor, \"Hello \"));
        invoker.executeCommand(new WriteCommand(editor, \"World!\"));
        invoker.executeCommand(new WriteCommand(editor, \" How are you?\"));
        
        System.out.println(\"\\n=== Undo Operations ===\");
        invoker.undo();
        invoker.undo();
        
        System.out.println(\"\\n=== Redo Operations ===\");
        invoker.redo();
        
        System.out.println(\"\\n=== Delete Operations ===\");
        invoker.executeCommand(new DeleteCommand(editor, 6));
        
        System.out.println(\"\\n=== Final Undo ===\");
        invoker.undo();
        invoker.undo();
    }
}
```

---

## 2. State Pattern

The State pattern allows an object to alter its behavior when its internal state changes. The object will appear to change its class.

### Key Concepts:
- **State**: Interface defining state-specific behavior
- **ConcreteState**: Implements behavior for a particular state
- **Context**: Maintains current state and delegates to state object
- **State Transitions**: States can transition to other states

### Example 1: Vending Machine

```java
// State interface
interface VendingMachineState {
    void insertCoin();
    void selectProduct();
    void dispenseProduct();
    void returnChange();
}

// Context class
class VendingMachine {
    private VendingMachineState noCoinState;
    private VendingMachineState hasCoinState;
    private VendingMachineState soldState;
    private VendingMachineState soldOutState;
    
    private VendingMachineState currentState;
    private int productCount;
    private int coinCount;
    
    public VendingMachine(int productCount) {
        this.productCount = productCount;
        this.coinCount = 0;
        
        // Initialize states
        noCoinState = new NoCoinState(this);
        hasCoinState = new HasCoinState(this);
        soldState = new SoldState(this);
        soldOutState = new SoldOutState(this);
        
        // Set initial state
        if (productCount > 0) {
            currentState = noCoinState;
        } else {
            currentState = soldOutState;
        }
    }
    
    // State transition methods
    public void setState(VendingMachineState state) {
        this.currentState = state;
    }
    
    // Delegate methods to current state
    public void insertCoin() {
        currentState.insertCoin();
    }
    
    public void selectProduct() {
        currentState.selectProduct();
    }
    
    public void dispenseProduct() {
        currentState.dispenseProduct();
    }
    
    public void returnChange() {
        currentState.returnChange();
    }
    
    // Helper methods
    public void releaseCoin() {
        if (coinCount > 0) {
            coinCount--;
            System.out.println(\"🪙 Coin returned\");
        }
    }
    
    public void releaseProduct() {
        if (productCount > 0) {
            productCount--;
            System.out.println(\"🥤 Product dispensed\");
        }
    }
    
    public void addCoin() {
        coinCount++;
    }
    
    // Getters
    public VendingMachineState getNoCoinState() { return noCoinState; }
    public VendingMachineState getHasCoinState() { return hasCoinState; }
    public VendingMachineState getSoldState() { return soldState; }
    public VendingMachineState getSoldOutState() { return soldOutState; }
    
    public int getProductCount() { return productCount; }
    public int getCoinCount() { return coinCount; }
    
    public void displayStatus() {
        System.out.println(\"\\n📊 Vending Machine Status:\");
        System.out.println(\"   Products: \" + productCount);
        System.out.println(\"   Coins: \" + coinCount);
        System.out.println(\"   State: \" + currentState.getClass().getSimpleName());
    }
}

// Concrete States
class NoCoinState implements VendingMachineState {
    private VendingMachine vendingMachine;
    
    public NoCoinState(VendingMachine vendingMachine) {
        this.vendingMachine = vendingMachine;
    }
    
    @Override
    public void insertCoin() {
        System.out.println(\"🪙 Coin inserted\");
        vendingMachine.addCoin();
        vendingMachine.setState(vendingMachine.getHasCoinState());
    }
    
    @Override
    public void selectProduct() {
        System.out.println(\"❌ Please insert coin first\");
    }
    
    @Override
    public void dispenseProduct() {
        System.out.println(\"❌ Please insert coin first\");
    }
    
    @Override
    public void returnChange() {
        System.out.println(\"❌ No coin to return\");
    }
}

class HasCoinState implements VendingMachineState {
    private VendingMachine vendingMachine;
    
    public HasCoinState(VendingMachine vendingMachine) {
        this.vendingMachine = vendingMachine;
    }
    
    @Override
    public void insertCoin() {
        System.out.println(\"🪙 Additional coin inserted\");
        vendingMachine.addCoin();
    }
    
    @Override
    public void selectProduct() {
        System.out.println(\"🎯 Product selected\");
        vendingMachine.setState(vendingMachine.getSoldState());
    }
    
    @Override
    public void dispenseProduct() {
        System.out.println(\"❌ Please select product first\");
    }
    
    @Override
    public void returnChange() {
        System.out.println(\"💰 Returning change\");
        vendingMachine.releaseCoin();
        vendingMachine.setState(vendingMachine.getNoCoinState());
    }
}

class SoldState implements VendingMachineState {
    private VendingMachine vendingMachine;
    
    public SoldState(VendingMachine vendingMachine) {
        this.vendingMachine = vendingMachine;
    }
    
    @Override
    public void insertCoin() {
        System.out.println(\"⏳ Please wait, processing your purchase\");
    }
    
    @Override
    public void selectProduct() {
        System.out.println(\"⏳ Already processing your selection\");
    }
    
    @Override
    public void dispenseProduct() {
        vendingMachine.releaseProduct();
        
        if (vendingMachine.getProductCount() > 0) {
            vendingMachine.setState(vendingMachine.getNoCoinState());
        } else {
            System.out.println(\"🚫 Machine is now sold out\");
            vendingMachine.setState(vendingMachine.getSoldOutState());
        }
    }
    
    @Override
    public void returnChange() {
        System.out.println(\"⏳ Please wait, processing your purchase\");
    }
}

class SoldOutState implements VendingMachineState {
    private VendingMachine vendingMachine;
    
    public SoldOutState(VendingMachine vendingMachine) {
        this.vendingMachine = vendingMachine;
    }
    
    @Override
    public void insertCoin() {
        System.out.println(\"🚫 Machine is sold out, coin returned\");
        vendingMachine.releaseCoin();
    }
    
    @Override
    public void selectProduct() {
        System.out.println(\"🚫 Machine is sold out\");
    }
    
    @Override
    public void dispenseProduct() {
        System.out.println(\"🚫 Machine is sold out\");
    }
    
    @Override
    public void returnChange() {
        vendingMachine.releaseCoin();
    }
}

// Usage
public class StatePatternDemo {
    public static void main(String[] args) {
        VendingMachine machine = new VendingMachine(3);
        
        machine.displayStatus();
        
        System.out.println(\"\\n=== Normal Purchase ===\");
        machine.insertCoin();
        machine.selectProduct();
        machine.dispenseProduct();
        machine.displayStatus();
        
        System.out.println(\"\\n=== Purchase with Change Return ===\");
        machine.insertCoin();
        machine.insertCoin();
        machine.returnChange();
        machine.displayStatus();
        
        System.out.println(\"\\n=== Multiple Purchases ===\");
        machine.insertCoin();
        machine.selectProduct();
        machine.dispenseProduct();
        
        machine.insertCoin();
        machine.selectProduct();
        machine.dispenseProduct();
        machine.displayStatus();
        
        System.out.println(\"\\n=== Sold Out Scenario ===\");
        machine.insertCoin();
        machine.displayStatus();
    }
}
```

### Example 2: Audio Player State Machine

```java
// State interface
interface AudioPlayerState {
    void play();
    void pause();
    void stop();
    void next();
    void previous();
}

// Context class
class AudioPlayer {
    private AudioPlayerState playingState;
    private AudioPlayerState pausedState;
    private AudioPlayerState stoppedState;
    
    private AudioPlayerState currentState;
    private String currentSong;
    private int currentTrack;
    private String[] playlist;
    
    public AudioPlayer(String[] playlist) {
        this.playlist = playlist;
        this.currentTrack = 0;
        this.currentSong = playlist[currentTrack];
        
        // Initialize states
        playingState = new PlayingState(this);
        pausedState = new PausedState(this);
        stoppedState = new StoppedState(this);
        
        // Set initial state
        currentState = stoppedState;
    }
    
    public void setState(AudioPlayerState state) {
        this.currentState = state;
    }
    
    // Delegate methods to current state
    public void play() {
        currentState.play();
    }
    
    public void pause() {
        currentState.pause();
    }
    
    public void stop() {
        currentState.stop();
    }
    
    public void next() {
        currentState.next();
    }
    
    public void previous() {
        currentState.previous();
    }
    
    // Helper methods
    public void startPlayback() {
        System.out.println(\"▶️ Playing: \" + currentSong);
    }
    
    public void stopPlayback() {
        System.out.println(\"⏹️ Stopped: \" + currentSong);
    }
    
    public void pausePlayback() {
        System.out.println(\"⏸️ Paused: \" + currentSong);
    }
    
    public void nextTrack() {
        if (currentTrack < playlist.length - 1) {
            currentTrack++;
            currentSong = playlist[currentTrack];
            System.out.println(\"⏭️ Next track: \" + currentSong);
        } else {
            System.out.println(\"📻 End of playlist\");
        }
    }
    
    public void previousTrack() {
        if (currentTrack > 0) {
            currentTrack--;
            currentSong = playlist[currentTrack];
            System.out.println(\"⏮️ Previous track: \" + currentSong);
        } else {
            System.out.println(\"📻 Beginning of playlist\");
        }
    }
    
    // Getters
    public AudioPlayerState getPlayingState() { return playingState; }
    public AudioPlayerState getPausedState() { return pausedState; }
    public AudioPlayerState getStoppedState() { return stoppedState; }
    
    public String getCurrentSong() { return currentSong; }
    public int getCurrentTrack() { return currentTrack; }
    
    public void displayStatus() {
        System.out.println(\"\\n🎵 Audio Player Status:\");
        System.out.println(\"   Current Song: \" + currentSong);
        System.out.println(\"   Track: \" + (currentTrack + 1) + \"/\" + playlist.length);
        System.out.println(\"   State: \" + currentState.getClass().getSimpleName());
    }
}

// Concrete States
class StoppedState implements AudioPlayerState {
    private AudioPlayer player;
    
    public StoppedState(AudioPlayer player) {
        this.player = player;
    }
    
    @Override
    public void play() {
        player.startPlayback();
        player.setState(player.getPlayingState());
    }
    
    @Override
    public void pause() {
        System.out.println(\"❌ Can't pause - player is stopped\");
    }
    
    @Override
    public void stop() {
        System.out.println(\"❌ Already stopped\");
    }
    
    @Override
    public void next() {
        player.nextTrack();
    }
    
    @Override
    public void previous() {
        player.previousTrack();
    }
}

class PlayingState implements AudioPlayerState {
    private AudioPlayer player;
    
    public PlayingState(AudioPlayer player) {
        this.player = player;
    }
    
    @Override
    public void play() {
        System.out.println(\"❌ Already playing\");
    }
    
    @Override
    public void pause() {
        player.pausePlayback();
        player.setState(player.getPausedState());
    }
    
    @Override
    public void stop() {
        player.stopPlayback();
        player.setState(player.getStoppedState());
    }
    
    @Override
    public void next() {
        player.nextTrack();
        player.startPlayback();
    }
    
    @Override
    public void previous() {
        player.previousTrack();
        player.startPlayback();
    }
}

class PausedState implements AudioPlayerState {
    private AudioPlayer player;
    
    public PausedState(AudioPlayer player) {
        this.player = player;
    }
    
    @Override
    public void play() {
        player.startPlayback();
        player.setState(player.getPlayingState());
    }
    
    @Override
    public void pause() {
        System.out.println(\"❌ Already paused\");
    }
    
    @Override
    public void stop() {
        player.stopPlayback();
        player.setState(player.getStoppedState());
    }
    
    @Override
    public void next() {
        player.nextTrack();
        player.startPlayback();
        player.setState(player.getPlayingState());
    }
    
    @Override
    public void previous() {
        player.previousTrack();
        player.startPlayback();
        player.setState(player.getPlayingState());
    }
}

// Usage
public class AudioPlayerStateDemo {
    public static void main(String[] args) {
        String[] playlist = {
            \"Song 1 - Artist A\",
            \"Song 2 - Artist B\",
            \"Song 3 - Artist C\",
            \"Song 4 - Artist D\"
        };
        
        AudioPlayer player = new AudioPlayer(playlist);
        player.displayStatus();
        
        System.out.println(\"\\n=== Starting Playback ===\");
        player.play();
        player.displayStatus();
        
        System.out.println(\"\\n=== Pausing and Resuming ===\");
        player.pause();
        player.play();
        player.displayStatus();
        
        System.out.println(\"\\n=== Navigation ===\");
        player.next();
        player.next();
        player.displayStatus();
        
        System.out.println(\"\\n=== Stopping ===\");
        player.stop();
        player.displayStatus();
        
        System.out.println(\"\\n=== Navigation while stopped ===\");
        player.previous();
        player.play();
        player.displayStatus();
    }
}
```

---

## 3. Practical Exercises

### Exercise 1: Document Management System with Command Pattern

```java
import java.util.*;

// Document class
class Document {
    private String name;
    private String content;
    
    public Document(String name) {
        this.name = name;
        this.content = \"\";
    }
    
    public void addContent(String text) {
        content += text;
        System.out.println(\"📄 Added content to \" + name + \": '\" + text + \"'\");
    }
    
    public void removeContent(int length) {
        if (content.length() >= length) {
            content = content.substring(0, content.length() - length);
            System.out.println(\"🗑️ Removed \" + length + \" characters from \" + name);
        }
    }
    
    public void save() {
        System.out.println(\"💾 Saved document: \" + name);
    }
    
    public void print() {
        System.out.println(\"🖨️ Printing document: \" + name);
        System.out.println(\"Content: \" + content);
    }
    
    // Getters and setters
    public String getName() { return name; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}

// Command interface
interface DocumentCommand {
    void execute();
    void undo();
}

// Concrete Commands
class AddTextCommand implements DocumentCommand {
    private Document document;
    private String text;
    
    public AddTextCommand(Document document, String text) {
        this.document = document;
        this.text = text;
    }
    
    @Override
    public void execute() {
        document.addContent(text);
    }
    
    @Override
    public void undo() {
        document.removeContent(text.length());
    }
}

class SaveCommand implements DocumentCommand {
    private Document document;
    
    public SaveCommand(Document document) {
        this.document = document;
    }
    
    @Override
    public void execute() {
        document.save();
    }
    
    @Override
    public void undo() {
        System.out.println(\"↩️ Save operation cannot be undone\");
    }
}

class PrintCommand implements DocumentCommand {
    private Document document;
    
    public PrintCommand(Document document) {
        this.document = document;
    }
    
    @Override
    public void execute() {
        document.print();
    }
    
    @Override
    public void undo() {
        System.out.println(\"↩️ Print operation cannot be undone\");
    }
}

// Document Manager (Invoker)
class DocumentManager {
    private Stack<DocumentCommand> commandHistory;
    
    public DocumentManager() {
        this.commandHistory = new Stack<>();
    }
    
    public void executeCommand(DocumentCommand command) {
        command.execute();
        commandHistory.push(command);
    }
    
    public void undoLastCommand() {
        if (!commandHistory.isEmpty()) {
            DocumentCommand lastCommand = commandHistory.pop();
            lastCommand.undo();
        } else {
            System.out.println(\"❌ No commands to undo\");
        }
    }
}

// Usage
public class DocumentManagementDemo {
    public static void main(String[] args) {
        Document doc = new Document(\"Report.txt\");
        DocumentManager manager = new DocumentManager();
        
        // Execute commands
        manager.executeCommand(new AddTextCommand(doc, \"Introduction: \"));
        manager.executeCommand(new AddTextCommand(doc, \"This is a sample report. \"));
        manager.executeCommand(new AddTextCommand(doc, \"It contains multiple sections.\"));
        manager.executeCommand(new SaveCommand(doc));
        manager.executeCommand(new PrintCommand(doc));
        
        System.out.println(\"\\n=== Undo Operations ===\");
        manager.undoLastCommand(); // Can't undo print
        manager.undoLastCommand(); // Can't undo save
        manager.undoLastCommand(); // Undo last text addition
        manager.undoLastCommand(); // Undo second text addition
        
        System.out.println(\"\\n=== Current Document State ===\");
        manager.executeCommand(new PrintCommand(doc));
    }
}
```

### Exercise 2: ATM State Machine

```java
// ATM State interface
interface ATMState {
    void insertCard();
    void ejectCard();
    void enterPin(int pin);
    void requestCash(int amount);
}

// ATM Context
class ATM {
    private ATMState noCardState;
    private ATMState hasCardState;
    private ATMState correctPinState;
    private ATMState outOfCashState;
    
    private ATMState currentState;
    private int cashAmount;
    private int correctPin;
    private boolean cardInserted;
    
    public ATM(int cashAmount, int correctPin) {
        this.cashAmount = cashAmount;
        this.correctPin = correctPin;
        this.cardInserted = false;
        
        // Initialize states
        noCardState = new NoCardState(this);
        hasCardState = new HasCardState(this);
        correctPinState = new CorrectPinState(this);
        outOfCashState = new OutOfCashState(this);
        
        // Set initial state
        currentState = noCardState;
    }
    
    public void setState(ATMState state) {
        this.currentState = state;
    }
    
    // Delegate methods
    public void insertCard() {
        currentState.insertCard();
    }
    
    public void ejectCard() {
        currentState.ejectCard();
    }
    
    public void enterPin(int pin) {
        currentState.enterPin(pin);
    }
    
    public void requestCash(int amount) {
        currentState.requestCash(amount);
    }
    
    // Helper methods
    public void setCardInserted(boolean inserted) {
        this.cardInserted = inserted;
    }
    
    public boolean validatePin(int pin) {
        return pin == correctPin;
    }
    
    public void dispenseCash(int amount) {
        if (cashAmount >= amount) {
            cashAmount -= amount;
            System.out.println(\"💰 Dispensed $\" + amount);
        }
    }
    
    // Getters
    public ATMState getNoCardState() { return noCardState; }
    public ATMState getHasCardState() { return hasCardState; }
    public ATMState getCorrectPinState() { return correctPinState; }
    public ATMState getOutOfCashState() { return outOfCashState; }
    
    public int getCashAmount() { return cashAmount; }
    public boolean isCardInserted() { return cardInserted; }
    
    public void displayStatus() {
        System.out.println(\"\\n🏧 ATM Status:\");
        System.out.println(\"   Cash Available: $\" + cashAmount);
        System.out.println(\"   Card Inserted: \" + cardInserted);
        System.out.println(\"   State: \" + currentState.getClass().getSimpleName());
    }
}

// Concrete States
class NoCardState implements ATMState {
    private ATM atm;
    
    public NoCardState(ATM atm) {
        this.atm = atm;
    }
    
    @Override
    public void insertCard() {
        System.out.println(\"💳 Card inserted\");
        atm.setCardInserted(true);
        atm.setState(atm.getHasCardState());
    }
    
    @Override
    public void ejectCard() {
        System.out.println(\"❌ No card to eject\");
    }
    
    @Override
    public void enterPin(int pin) {
        System.out.println(\"❌ Please insert card first\");
    }
    
    @Override
    public void requestCash(int amount) {
        System.out.println(\"❌ Please insert card first\");
    }
}

class HasCardState implements ATMState {
    private ATM atm;
    
    public HasCardState(ATM atm) {
        this.atm = atm;
    }
    
    @Override
    public void insertCard() {
        System.out.println(\"❌ Card already inserted\");
    }
    
    @Override
    public void ejectCard() {
        System.out.println(\"💳 Card ejected\");
        atm.setCardInserted(false);
        atm.setState(atm.getNoCardState());
    }
    
    @Override
    public void enterPin(int pin) {
        if (atm.validatePin(pin)) {
            System.out.println(\"✅ PIN accepted\");
            atm.setState(atm.getCorrectPinState());
        } else {
            System.out.println(\"❌ Incorrect PIN\");
            ejectCard();
        }
    }
    
    @Override
    public void requestCash(int amount) {
        System.out.println(\"❌ Please enter PIN first\");
    }
}

class CorrectPinState implements ATMState {
    private ATM atm;
    
    public CorrectPinState(ATM atm) {
        this.atm = atm;
    }
    
    @Override
    public void insertCard() {
        System.out.println(\"❌ Card already inserted\");
    }
    
    @Override
    public void ejectCard() {
        System.out.println(\"💳 Card ejected\");
        atm.setCardInserted(false);
        atm.setState(atm.getNoCardState());
    }
    
    @Override
    public void enterPin(int pin) {
        System.out.println(\"❌ PIN already accepted\");
    }
    
    @Override
    public void requestCash(int amount) {
        if (atm.getCashAmount() >= amount) {
            atm.dispenseCash(amount);
            if (atm.getCashAmount() == 0) {
                System.out.println(\"🚫 ATM is out of cash\");
                atm.setState(atm.getOutOfCashState());
            } else {
                ejectCard();
            }
        } else {
            System.out.println(\"❌ Insufficient funds in ATM\");
        }
    }
}

class OutOfCashState implements ATMState {
    private ATM atm;
    
    public OutOfCashState(ATM atm) {
        this.atm = atm;
    }
    
    @Override
    public void insertCard() {
        System.out.println(\"🚫 ATM is out of service\");
    }
    
    @Override
    public void ejectCard() {
        if (atm.isCardInserted()) {
            System.out.println(\"💳 Card ejected\");
            atm.setCardInserted(false);
        } else {
            System.out.println(\"❌ No card to eject\");
        }
    }
    
    @Override
    public void enterPin(int pin) {
        System.out.println(\"🚫 ATM is out of service\");
    }
    
    @Override
    public void requestCash(int amount) {
        System.out.println(\"🚫 ATM is out of cash\");
    }
}

// Usage
public class ATMStateDemo {
    public static void main(String[] args) {
        ATM atm = new ATM(500, 1234);
        atm.displayStatus();
        
        System.out.println(\"\\n=== Normal Transaction ===\");
        atm.insertCard();
        atm.enterPin(1234);
        atm.requestCash(200);
        atm.displayStatus();
        
        System.out.println(\"\\n=== Another Transaction ===\");
        atm.insertCard();
        atm.enterPin(1234);
        atm.requestCash(300);
        atm.displayStatus();
        
        System.out.println(\"\\n=== Out of Cash Scenario ===\");
        atm.insertCard();
        atm.enterPin(1234);
        atm.requestCash(100);
        atm.displayStatus();
    }
}
```

---

## 4. Key Takeaways

### Command Pattern Benefits:
1. **Decoupling**: Separates invoker from receiver
2. **Undo/Redo**: Easy to implement undo operations
3. **Macro Commands**: Can combine multiple commands
4. **Queuing**: Commands can be queued and executed later
5. **Logging**: Commands can be logged for replay

### State Pattern Benefits:
1. **Clean State Management**: Eliminates complex conditional logic
2. **Extensibility**: Easy to add new states
3. **Encapsulation**: State-specific behavior is encapsulated
4. **Maintainability**: Each state is a separate class

### When to Use:
- **Command**: When you need to parameterize objects with operations, queue operations, or support undo
- **State**: When an object's behavior depends on its state and must change behavior at runtime

### Best Practices:
1. **Command**: Use null object pattern for empty slots
2. **State**: Keep state objects lightweight and stateless
3. **Both**: Consider using with Factory pattern for object creation
4. **Both**: Document state transitions and command sequences

---

## 5. Next Steps

Tomorrow we'll complete Week 8 with a comprehensive **Capstone Project** that combines multiple design patterns into an enterprise-level framework.

**Practice Assignment**: Create a text editor that uses both Command pattern (for operations) and State pattern (for editing modes like insert/overwrite).

---

*\"The Command pattern is like a remote control - it encapsulates all the information needed to perform an action later.\"*

*\"The State pattern is like a person's mood - the same stimulus can produce different responses depending on the current state.\"*