# Day 8: Polymorphism & Abstraction (July 17, 2025)

**Date:** July 17, 2025  
**Duration:** 2.5 hours  
**Focus:** Master advanced OOP concepts - Polymorphism, Abstraction, and Interfaces

---

## 🎯 Today's Learning Goals

By the end of this session, you'll:
- ✅ Master abstract classes and methods
- ✅ Create and implement interfaces with multiple inheritance
- ✅ Understand runtime polymorphism and dynamic method dispatch
- ✅ Use instanceof operator and type checking
- ✅ Build polymorphic systems with multiple implementations

---

## 📚 Part 1: Abstract Classes and Methods (45 minutes)

### **Understanding Abstraction**

**Abstract Class**: A class that cannot be instantiated directly and may contain abstract methods that must be implemented by subclasses.

**Key Rules:**
- Declared with `abstract` keyword
- Can have both abstract and concrete methods
- Can have constructors, instance variables, and static methods
- Subclasses must implement all abstract methods or be abstract themselves

### **Abstract Shape Hierarchy Example**

Create `Shape.java`:

```java
/**
 * Abstract Shape class demonstrating abstraction and polymorphism
 * Provides common functionality and defines abstract methods that subclasses must implement
 */
public abstract class Shape {
    protected String color;
    protected double x, y; // Position coordinates
    
    // Constructor
    public Shape(String color, double x, double y) {
        this.color = color;
        this.x = x;
        this.y = y;
    }
    
    // Abstract methods - must be implemented by subclasses
    public abstract double calculateArea();
    public abstract double calculatePerimeter();
    public abstract void draw();
    
    // Concrete methods - shared by all shapes
    public void move(double newX, double newY) {
        this.x = newX;
        this.y = newY;
        System.out.println(color + " shape moved to (" + x + ", " + y + ")");
    }
    
    public void setColor(String color) {
        this.color = color;
        System.out.println("Shape color changed to " + color);
    }
    
    public String getColor() {
        return color;
    }
    
    public double getX() {
        return x;
    }
    
    public double getY() {
        return y;
    }
    
    // Template method pattern - defines algorithm structure
    public void displayInfo() {
        System.out.println("=== Shape Information ===");
        System.out.println("Type: " + this.getClass().getSimpleName());
        System.out.println("Color: " + color);
        System.out.println("Position: (" + x + ", " + y + ")");
        System.out.println("Area: " + calculateArea());
        System.out.println("Perimeter: " + calculatePerimeter());
        draw();
    }
    
    // toString override for better representation
    @Override
    public String toString() {
        return this.getClass().getSimpleName() + 
               "[color=" + color + ", x=" + x + ", y=" + y + "]";
    }
}
```

### **Concrete Shape Implementations**

Create `Circle.java`:

```java
/**
 * Circle class extending abstract Shape
 * Demonstrates concrete implementation of abstract methods
 */
public class Circle extends Shape {
    private double radius;
    
    public Circle(String color, double x, double y, double radius) {
        super(color, x, y);
        this.radius = radius;
    }
    
    @Override
    public double calculateArea() {
        return Math.PI * radius * radius;
    }
    
    @Override
    public double calculatePerimeter() {
        return 2 * Math.PI * radius;
    }
    
    @Override
    public void draw() {
        System.out.println("Drawing a " + color + " circle with radius " + radius + 
                         " at position (" + x + ", " + y + ")");
        // ASCII art representation
        System.out.println("    ***");
        System.out.println("  *     *");
        System.out.println(" *       *");
        System.out.println("  *     *");
        System.out.println("    ***");
    }
    
    // Circle-specific methods
    public double getRadius() {
        return radius;
    }
    
    public void setRadius(double radius) {
        this.radius = radius;
        System.out.println("Circle radius changed to " + radius);
    }
    
    public double getDiameter() {
        return 2 * radius;
    }
    
    @Override
    public String toString() {
        return "Circle[color=" + color + ", x=" + x + ", y=" + y + 
               ", radius=" + radius + "]";
    }
}
```

Create `Rectangle.java`:

```java
/**
 * Rectangle class extending abstract Shape
 * Demonstrates concrete implementation with multiple properties
 */
public class Rectangle extends Shape {
    private double width;
    private double height;
    
    public Rectangle(String color, double x, double y, double width, double height) {
        super(color, x, y);
        this.width = width;
        this.height = height;
    }
    
    @Override
    public double calculateArea() {
        return width * height;
    }
    
    @Override
    public double calculatePerimeter() {
        return 2 * (width + height);
    }
    
    @Override
    public void draw() {
        System.out.println("Drawing a " + color + " rectangle " + width + "x" + height + 
                         " at position (" + x + ", " + y + ")");
        // ASCII art representation
        System.out.println("*********");
        System.out.println("*       *");
        System.out.println("*       *");
        System.out.println("*********");
    }
    
    // Rectangle-specific methods
    public double getWidth() {
        return width;
    }
    
    public void setWidth(double width) {
        this.width = width;
    }
    
    public double getHeight() {
        return height;
    }
    
    public void setHeight(double height) {
        this.height = height;
    }
    
    public boolean isSquare() {
        return width == height;
    }
    
    @Override
    public String toString() {
        return "Rectangle[color=" + color + ", x=" + x + ", y=" + y + 
               ", width=" + width + ", height=" + height + "]";
    }
}
```

---

## 🔌 Part 2: Interfaces and Multiple Inheritance (45 minutes)

### **Understanding Interfaces**

**Interface**: A contract that defines what methods a class must implement, promoting multiple inheritance of type.

**Key Features:**
- All methods are implicitly `public abstract` (before Java 8)
- All variables are implicitly `public static final`
- A class can implement multiple interfaces
- Java 8+ allows default and static methods in interfaces

### **Interface Examples**

Create `Drawable.java`:

```java
/**
 * Drawable interface demonstrating interface concepts
 * Defines contract for objects that can be drawn and highlighted
 */
public interface Drawable {
    // Abstract method (implicitly public abstract)
    void draw();
    
    // Default method (Java 8+) - provides default implementation
    default void highlight() {
        System.out.println("🔥 Highlighting the drawable object with yellow border!");
    }
    
    default void fade() {
        System.out.println("🌫️ Fading the drawable object to 50% opacity");
    }
    
    // Static method (Java 8+) - belongs to interface, not instances
    static void printInfo() {
        System.out.println("📋 Drawable Interface Info:");
        System.out.println("   - Provides drawing capabilities");
        System.out.println("   - Supports highlighting and fading");
        System.out.println("   - Version 2.0 with default methods");
    }
    
    // Constant (implicitly public static final)
    String VERSION = "2.0";
    int MAX_OPACITY = 100;
    int MIN_OPACITY = 0;
}
```

Create `Resizable.java`:

```java
/**
 * Resizable interface demonstrating multiple interface inheritance
 * Defines contract for objects that can be resized
 */
public interface Resizable {
    void resize(double factor);
    void setDimensions(double width, double height);
    
    // Default methods for common resizing operations
    default void doubleSize() {
        resize(2.0);
        System.out.println("📏 Object size doubled!");
    }
    
    default void halveSize() {
        resize(0.5);
        System.out.println("📏 Object size halved!");
    }
    
    default void resetSize() {
        System.out.println("📏 Resetting to original size...");
        // Implementation would depend on specific object
    }
}
```

Create `Animatable.java`:

```java
/**
 * Animatable interface for objects that can be animated
 * Demonstrates interface with animation capabilities
 */
public interface Animatable {
    void startAnimation();
    void stopAnimation();
    void setAnimationSpeed(double speed);
    
    default void pauseAnimation() {
        System.out.println("⏸️ Animation paused");
    }
    
    default void resumeAnimation() {
        System.out.println("▶️ Animation resumed");
    }
    
    default boolean isAnimating() {
        return false; // Default implementation
    }
}
```

### **Multiple Interface Implementation**

Create `EnhancedCircle.java`:

```java
/**
 * Enhanced Circle that implements multiple interfaces
 * Demonstrates multiple interface inheritance and polymorphism
 */
public class EnhancedCircle extends Circle implements Drawable, Resizable, Animatable {
    private boolean isAnimating;
    private double animationSpeed;
    private double originalRadius;
    
    public EnhancedCircle(String color, double x, double y, double radius) {
        super(color, x, y, radius);
        this.originalRadius = radius;
        this.isAnimating = false;
        this.animationSpeed = 1.0;
    }
    
    // Resizable interface implementation
    @Override
    public void resize(double factor) {
        double newRadius = getRadius() * factor;
        setRadius(newRadius);
        System.out.println("🔄 Circle resized by factor " + factor + 
                         " (new radius: " + newRadius + ")");
    }
    
    @Override
    public void setDimensions(double width, double height) {
        // For circle, we'll use the average of width and height as diameter
        double newRadius = (width + height) / 4.0;
        setRadius(newRadius);
        System.out.println("📐 Circle dimensions set to radius: " + newRadius);
    }
    
    @Override
    public void resetSize() {
        setRadius(originalRadius);
        System.out.println("↩️ Circle reset to original radius: " + originalRadius);
    }
    
    // Animatable interface implementation
    @Override
    public void startAnimation() {
        isAnimating = true;
        System.out.println("🎬 Circle animation started!");
        animatePulse();
    }
    
    @Override
    public void stopAnimation() {
        isAnimating = false;
        System.out.println("⏹️ Circle animation stopped!");
    }
    
    @Override
    public void setAnimationSpeed(double speed) {
        this.animationSpeed = speed;
        System.out.println("⚡ Animation speed set to " + speed + "x");
    }
    
    @Override
    public boolean isAnimating() {
        return isAnimating;
    }
    
    // Custom animation method
    private void animatePulse() {
        System.out.println("💓 Circle pulsing animation:");
        for (int i = 0; i < 3; i++) {
            System.out.println("  Pulse " + (i + 1) + ": ◯ → ⬢ → ◯");
        }
    }
    
    // Override draw to show enhancement
    @Override
    public void draw() {
        super.draw();
        if (isAnimating) {
            System.out.println("✨ (Currently animating at " + animationSpeed + "x speed)");
        }
    }
    
    // Enhanced info display
    public void displayEnhancedInfo() {
        displayInfo();
        System.out.println("Enhanced Features:");
        System.out.println("- Drawable: ✅");
        System.out.println("- Resizable: ✅");
        System.out.println("- Animatable: ✅");
        System.out.println("- Currently Animating: " + (isAnimating ? "Yes" : "No"));
        System.out.println("- Animation Speed: " + animationSpeed + "x");
    }
}
```

---

## 🎵 Part 3: Media Player System Project (45 minutes)

### **Abstract Media File Base Class**

Create `MediaFile.java`:

```java
/**
 * Abstract MediaFile class for different media types
 * Demonstrates abstraction in a media player system
 */
public abstract class MediaFile {
    protected String fileName;
    protected String filePath;
    protected long fileSize; // in bytes
    protected String format;
    protected boolean isPlaying;
    
    public MediaFile(String fileName, String filePath, long fileSize, String format) {
        this.fileName = fileName;
        this.filePath = filePath;
        this.fileSize = fileSize;
        this.format = format;
        this.isPlaying = false;
    }
    
    // Abstract methods that must be implemented by subclasses
    public abstract void play();
    public abstract void pause();
    public abstract void stop();
    public abstract String getMediaInfo();
    public abstract double getDuration(); // in seconds
    
    // Concrete methods shared by all media files
    public void displayFileInfo() {
        System.out.println("=== Media File Information ===");
        System.out.println("File Name: " + fileName);
        System.out.println("Path: " + filePath);
        System.out.println("Size: " + formatFileSize(fileSize));
        System.out.println("Format: " + format);
        System.out.println("Duration: " + formatDuration(getDuration()));
        System.out.println("Status: " + (isPlaying ? "Playing" : "Stopped"));
        System.out.println(getMediaInfo());
    }
    
    protected String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
    
    protected String formatDuration(double seconds) {
        int hours = (int) (seconds / 3600);
        int minutes = (int) ((seconds % 3600) / 60);
        int secs = (int) (seconds % 60);
        
        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, secs);
        } else {
            return String.format("%02d:%02d", minutes, secs);
        }
    }
    
    // Getters
    public String getFileName() { return fileName; }
    public String getFilePath() { return filePath; }
    public long getFileSize() { return fileSize; }
    public String getFormat() { return format; }
    public boolean isPlaying() { return isPlaying; }
    
    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[" + fileName + ", " + format + ", " + 
               formatFileSize(fileSize) + "]";
    }
}
```

### **Audio File Implementation**

Create `AudioFile.java`:

```java
/**
 * AudioFile class extending MediaFile
 * Represents audio files with specific audio properties
 */
public class AudioFile extends MediaFile {
    private String artist;
    private String album;
    private int bitrate; // in kbps
    private int sampleRate; // in Hz
    private double duration; // in seconds
    private int volume; // 0-100
    
    public AudioFile(String fileName, String filePath, long fileSize, String format,
                    String artist, String album, int bitrate, int sampleRate, double duration) {
        super(fileName, filePath, fileSize, format);
        this.artist = artist;
        this.album = album;
        this.bitrate = bitrate;
        this.sampleRate = sampleRate;
        this.duration = duration;
        this.volume = 50; // Default volume
    }
    
    @Override
    public void play() {
        if (!isPlaying) {
            isPlaying = true;
            System.out.println("🎵 Playing audio: " + fileName + " by " + artist);
            System.out.println("♪♫♪ Now playing from album: " + album);
            System.out.println("🔊 Volume: " + volume + "%");
        } else {
            System.out.println("▶️ Audio is already playing!");
        }
    }
    
    @Override
    public void pause() {
        if (isPlaying) {
            isPlaying = false;
            System.out.println("⏸️ Audio paused: " + fileName);
        } else {
            System.out.println("⏸️ Audio is not currently playing!");
        }
    }
    
    @Override
    public void stop() {
        if (isPlaying) {
            isPlaying = false;
            System.out.println("⏹️ Audio stopped: " + fileName);
        } else {
            System.out.println("⏹️ Audio is already stopped!");
        }
    }
    
    @Override
    public String getMediaInfo() {
        return "Audio Info - Artist: " + artist + ", Album: " + album + 
               ", Bitrate: " + bitrate + " kbps, Sample Rate: " + sampleRate + " Hz";
    }
    
    @Override
    public double getDuration() {
        return duration;
    }
    
    // Audio-specific methods
    public void setVolume(int volume) {
        if (volume >= 0 && volume <= 100) {
            this.volume = volume;
            System.out.println("🔊 Volume set to " + volume + "%");
        } else {
            System.out.println("❌ Volume must be between 0 and 100!");
        }
    }
    
    public void increaseVolume() {
        if (volume < 100) {
            volume = Math.min(100, volume + 10);
            System.out.println("🔊 Volume increased to " + volume + "%");
        }
    }
    
    public void decreaseVolume() {
        if (volume > 0) {
            volume = Math.max(0, volume - 10);
            System.out.println("🔉 Volume decreased to " + volume + "%");
        }
    }
    
    public void mute() {
        volume = 0;
        System.out.println("🔇 Audio muted");
    }
    
    // Getters
    public String getArtist() { return artist; }
    public String getAlbum() { return album; }
    public int getBitrate() { return bitrate; }
    public int getSampleRate() { return sampleRate; }
    public int getVolume() { return volume; }
    
    @Override
    public String toString() {
        return "AudioFile[" + fileName + " by " + artist + ", " + format + 
               ", " + formatDuration(duration) + "]";
    }
}
```

### **Video File Implementation**

Create `VideoFile.java`:

```java
/**
 * VideoFile class extending MediaFile
 * Represents video files with video-specific properties
 */
public class VideoFile extends MediaFile {
    private int width;
    private int height;
    private double frameRate; // frames per second
    private String codec;
    private double duration; // in seconds
    private boolean hasSubtitles;
    private int volume; // 0-100
    
    public VideoFile(String fileName, String filePath, long fileSize, String format,
                    int width, int height, double frameRate, String codec, double duration, boolean hasSubtitles) {
        super(fileName, filePath, fileSize, format);
        this.width = width;
        this.height = height;
        this.frameRate = frameRate;
        this.codec = codec;
        this.duration = duration;
        this.hasSubtitles = hasSubtitles;
        this.volume = 75; // Default volume for videos
    }
    
    @Override
    public void play() {
        if (!isPlaying) {
            isPlaying = true;
            System.out.println("🎬 Playing video: " + fileName);
            System.out.println("📺 Resolution: " + width + "x" + height + " at " + frameRate + " FPS");
            System.out.println("🎞️ Codec: " + codec);
            if (hasSubtitles) {
                System.out.println("💬 Subtitles: Available");
            }
            System.out.println("🔊 Volume: " + volume + "%");
        } else {
            System.out.println("▶️ Video is already playing!");
        }
    }
    
    @Override
    public void pause() {
        if (isPlaying) {
            isPlaying = false;
            System.out.println("⏸️ Video paused: " + fileName);
        } else {
            System.out.println("⏸️ Video is not currently playing!");
        }
    }
    
    @Override
    public void stop() {
        if (isPlaying) {
            isPlaying = false;
            System.out.println("⏹️ Video stopped: " + fileName);
        } else {
            System.out.println("⏹️ Video is already stopped!");
        }
    }
    
    @Override
    public String getMediaInfo() {
        return "Video Info - Resolution: " + width + "x" + height + 
               ", Frame Rate: " + frameRate + " FPS, Codec: " + codec + 
               ", Subtitles: " + (hasSubtitles ? "Yes" : "No");
    }
    
    @Override
    public double getDuration() {
        return duration;
    }
    
    // Video-specific methods
    public void setVolume(int volume) {
        if (volume >= 0 && volume <= 100) {
            this.volume = volume;
            System.out.println("🔊 Video volume set to " + volume + "%");
        } else {
            System.out.println("❌ Volume must be between 0 and 100!");
        }
    }
    
    public void toggleSubtitles() {
        hasSubtitles = !hasSubtitles;
        System.out.println("💬 Subtitles " + (hasSubtitles ? "enabled" : "disabled"));
    }
    
    public void fastForward() {
        if (isPlaying) {
            System.out.println("⏩ Fast forwarding video...");
        } else {
            System.out.println("❌ Cannot fast forward - video is not playing!");
        }
    }
    
    public void rewind() {
        if (isPlaying) {
            System.out.println("⏪ Rewinding video...");
        } else {
            System.out.println("❌ Cannot rewind - video is not playing!");
        }
    }
    
    public String getResolution() {
        return width + "x" + height;
    }
    
    public String getQuality() {
        if (height >= 2160) return "4K Ultra HD";
        if (height >= 1440) return "1440p QHD";
        if (height >= 1080) return "1080p Full HD";
        if (height >= 720) return "720p HD";
        if (height >= 480) return "480p SD";
        return "Low Quality";
    }
    
    // Getters
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public double getFrameRate() { return frameRate; }
    public String getCodec() { return codec; }
    public boolean hasSubtitles() { return hasSubtitles; }
    public int getVolume() { return volume; }
    
    @Override
    public String toString() {
        return "VideoFile[" + fileName + ", " + getResolution() + " " + getQuality() + 
               ", " + formatDuration(duration) + "]";
    }
}
```

---

## 🔍 Part 4: Runtime Polymorphism and instanceof (30 minutes)

### **instanceof Operator and Type Checking**

The `instanceof` operator tests whether an object is an instance of a specific class or interface:

```java
if (shape instanceof Circle) {
    Circle circle = (Circle) shape;
    System.out.println("Radius: " + circle.getRadius());
}
```

### **Complete Polymorphism Demonstration**

Create `PolymorphismDemo.java`:

```java
/**
 * Comprehensive demonstration of Polymorphism and Abstraction
 * Shows runtime polymorphism, interface implementation, and instanceof usage
 */
public class PolymorphismDemo {
    
    public static void main(String[] args) {
        System.out.println("=".repeat(70));
        System.out.println("🎯 POLYMORPHISM & ABSTRACTION DEMONSTRATION");
        System.out.println("=".repeat(70));
        
        // Part 1: Abstract Classes and Polymorphism
        demonstrateShapePolymorphism();
        
        System.out.println("\n" + "=".repeat(70));
        
        // Part 2: Interface Implementation
        demonstrateInterfaceImplementation();
        
        System.out.println("\n" + "=".repeat(70));
        
        // Part 3: Media Player System
        demonstrateMediaPlayerSystem();
        
        System.out.println("\n" + "=".repeat(70));
        
        // Part 4: instanceof and Type Checking
        demonstrateInstanceofOperator();
        
        System.out.println("\n" + "=".repeat(70));
        System.out.println("🎉 DEMONSTRATION COMPLETE!");
        System.out.println("=".repeat(70));
    }
    
    public static void demonstrateShapePolymorphism() {
        System.out.println("📐 PART 1: ABSTRACT CLASSES & POLYMORPHISM");
        System.out.println("-".repeat(50));
        
        // Create array of different shapes - demonstrates polymorphism
        Shape[] shapes = {
            new Circle("Red", 0, 0, 5),
            new Rectangle("Blue", 10, 10, 8, 6),
            new Circle("Yellow", 20, 20, 3)
        };
        
        System.out.println("Created " + shapes.length + " different shapes\n");
        
        // Polymorphic method calls - same method name, different implementations
        for (int i = 0; i < shapes.length; i++) {
            System.out.println("Shape " + (i + 1) + ":");
            System.out.println("Type: " + shapes[i].getClass().getSimpleName());
            System.out.println("Area: " + String.format("%.2f", shapes[i].calculateArea()));
            System.out.println("Perimeter: " + String.format("%.2f", shapes[i].calculatePerimeter()));
            shapes[i].draw();
            shapes[i].move(i * 2, i * 2);
            System.out.println();
        }
        
        // Calculate total area using polymorphism
        double totalArea = calculateTotalArea(shapes);
        System.out.println("📊 Total area of all shapes: " + String.format("%.2f", totalArea));
    }
    
    public static void demonstrateInterfaceImplementation() {
        System.out.println("🔌 PART 2: INTERFACE IMPLEMENTATION");
        System.out.println("-".repeat(50));
        
        // Static method call from interface
        Drawable.printInfo();
        System.out.println();
        
        // Create enhanced circle that implements multiple interfaces
        EnhancedCircle enhancedCircle = new EnhancedCircle("Purple", 0, 0, 4);
        
        // Use as different interface types - demonstrates interface polymorphism
        Drawable drawable = enhancedCircle;
        Resizable resizable = enhancedCircle;
        Animatable animatable = enhancedCircle;
        
        System.out.println("🎨 Testing Drawable interface:");
        drawable.draw();
        drawable.highlight();
        drawable.fade();
        
        System.out.println("\n📏 Testing Resizable interface:");
        resizable.doubleSize();
        resizable.halveSize();
        resizable.resetSize();
        resizable.setDimensions(10, 8);
        
        System.out.println("\n🎬 Testing Animatable interface:");
        animatable.startAnimation();
        animatable.setAnimationSpeed(2.0);
        animatable.pauseAnimation();
        animatable.resumeAnimation();
        animatable.stopAnimation();
        
        System.out.println("\n📋 Enhanced Circle Complete Info:");
        enhancedCircle.displayEnhancedInfo();
    }
    
    public static void demonstrateMediaPlayerSystem() {
        System.out.println("🎵 PART 3: MEDIA PLAYER SYSTEM");
        System.out.println("-".repeat(50));
        
        // Create different media files - demonstrates abstract class inheritance
        MediaFile[] mediaFiles = {
            new AudioFile("Song.mp3", "/music/song.mp3", 3456789, "MP3",
                         "The Beatles", "Abbey Road", 320, 44100, 187.5),
            new VideoFile("Movie.mp4", "/videos/movie.mp4", 1234567890, "MP4",
                         1920, 1080, 24.0, "H.264", 7350.0, true)
        };
        
        System.out.println("📚 Media Library contains " + mediaFiles.length + " files:\n");
        
        // Polymorphic operations on media files
        for (MediaFile media : mediaFiles) {
            media.displayFileInfo();
            System.out.println("\n▶️ Playing media:");
            media.play();
            
            // Type-specific operations
            if (media instanceof AudioFile) {
                AudioFile audio = (AudioFile) media;
                audio.setVolume(80);
                audio.increaseVolume();
            } else if (media instanceof VideoFile) {
                VideoFile video = (VideoFile) media;
                video.toggleSubtitles();
                video.fastForward();
            }
            
            System.out.println("\n⏸️ Pausing media:");
            media.pause();
            System.out.println("\n⏹️ Stopping media:");
            media.stop();
            System.out.println("-".repeat(40));
        }
    }
    
    public static void demonstrateInstanceofOperator() {
        System.out.println("🔍 PART 4: INSTANCEOF OPERATOR & TYPE CHECKING");
        System.out.println("-".repeat(50));
        
        // Create mixed array of objects
        Object[] objects = {
            new Circle("Orange", 0, 0, 3),
            new AudioFile("Test.wav", "/test.wav", 123456, "WAV", "Artist", "Album", 256, 48000, 120.0),
            new EnhancedCircle("Pink", 5, 5, 2),
            "Hello World",
            new Integer(42),
            new Rectangle("Black", 0, 0, 5, 3)
        };
        
        System.out.println("🧪 Testing instanceof with " + objects.length + " different objects:\n");
        
        for (int i = 0; i < objects.length; i++) {
            Object obj = objects[i];
            System.out.println("Object " + (i + 1) + ": " + obj.getClass().getSimpleName());
            
            // Test various instanceof checks
            System.out.println("  instanceof Object: " + (obj instanceof Object));
            System.out.println("  instanceof Shape: " + (obj instanceof Shape));
            System.out.println("  instanceof Circle: " + (obj instanceof Circle));
            System.out.println("  instanceof MediaFile: " + (obj instanceof MediaFile));
            System.out.println("  instanceof Drawable: " + (obj instanceof Drawable));
            System.out.println("  instanceof String: " + (obj instanceof String));
            
            // Safe casting based on instanceof
            if (obj instanceof Shape) {
                Shape shape = (Shape) obj;
                System.out.println("  → Shape area: " + String.format("%.2f", shape.calculateArea()));
            }
            
            if (obj instanceof Drawable) {
                Drawable drawable = (Drawable) obj;
                System.out.println("  → Can be drawn!");
                drawable.highlight();
            }
            
            if (obj instanceof MediaFile) {
                MediaFile media = (MediaFile) obj;
                System.out.println("  → Media duration: " + media.getDuration() + " seconds");
            }
            
            System.out.println();
        }
        
        // Demonstrate interface type checking
        demonstrateInterfaceTypeChecking();
    }
    
    private static void demonstrateInterfaceTypeChecking() {
        System.out.println("🔌 Interface Type Checking:");
        
        EnhancedCircle enhancedCircle = new EnhancedCircle("Gold", 0, 0, 1);
        
        System.out.println("EnhancedCircle implements:");
        System.out.println("  Drawable: " + (enhancedCircle instanceof Drawable));
        System.out.println("  Resizable: " + (enhancedCircle instanceof Resizable));
        System.out.println("  Animatable: " + (enhancedCircle instanceof Animatable));
        System.out.println("  Shape: " + (enhancedCircle instanceof Shape));
        System.out.println("  Circle: " + (enhancedCircle instanceof Circle));
        
        // Demonstrate multiple interface casting
        if (enhancedCircle instanceof Drawable && enhancedCircle instanceof Resizable) {
            System.out.println("\n✅ EnhancedCircle can be both drawn and resized!");
            ((Drawable) enhancedCircle).draw();
            ((Resizable) enhancedCircle).doubleSize();
        }
    }
    
    // Utility method demonstrating polymorphism
    public static double calculateTotalArea(Shape[] shapes) {
        double total = 0;
        for (Shape shape : shapes) {
            total += shape.calculateArea(); // Polymorphic method call
        }
        return total;
    }
    
    // Method overloading demonstration
    public static void playMedia(MediaFile media) {
        System.out.println("Playing generic media: " + media.getFileName());
        media.play();
    }
    
    public static void playMedia(AudioFile audio) {
        System.out.println("Playing audio with enhanced features: " + audio.getFileName());
        audio.play();
        audio.setVolume(75);
    }
    
    public static void playMedia(VideoFile video) {
        System.out.println("Playing video with enhanced features: " + video.getFileName());
        video.play();
        if (video.hasSubtitles()) {
            video.toggleSubtitles();
        }
    }
}
```

---

## 📝 Key Takeaways from Day 8

### **Abstract Classes:**
- ✅ **Purpose:** Provide template for related classes with some common implementation
- ✅ **Abstract Methods:** Must be implemented by subclasses
- ✅ **Concrete Methods:** Shared functionality across subclasses
- ✅ **Cannot Instantiate:** Can only be extended, not instantiated directly

### **Interfaces:**
- ✅ **Contract:** Define what methods a class must implement
- ✅ **Multiple Inheritance:** Class can implement multiple interfaces
- ✅ **Default Methods:** Java 8+ allows default implementations
- ✅ **Static Methods:** Utility methods that belong to interface

### **Polymorphism:**
- ✅ **Runtime Binding:** Method called depends on actual object type
- ✅ **Interface Polymorphism:** Treat different objects uniformly
- ✅ **Method Overriding:** Subclass provides specific implementation
- ✅ **Dynamic Dispatch:** JVM determines which method to call at runtime

### **instanceof Operator:**
- ✅ **Type Checking:** Test if object is instance of specific type
- ✅ **Safe Casting:** Check before casting to avoid ClassCastException
- ✅ **Interface Testing:** Works with both classes and interfaces
- ✅ **Inheritance Hierarchy:** Returns true for all types in hierarchy

---

## 🎯 Today's Success Criteria

**Completed Tasks:**
- [ ] Create abstract Shape hierarchy with multiple implementations
- [ ] Implement interfaces with default and static methods
- [ ] Build EnhancedCircle with multiple interface inheritance
- [ ] Develop Media Player System with polymorphic behavior
- [ ] Demonstrate runtime polymorphism and instanceof usage
- [ ] Test all polymorphic scenarios with comprehensive examples
- [ ] Understand when to use abstract classes vs interfaces

**Bonus Achievements:**
- [ ] Create additional shape types (Triangle, Pentagon)
- [ ] Add more interfaces (Colorable, Transformable)
- [ ] Implement ImageFile for complete media system
- [ ] Experiment with complex inheritance hierarchies
- [ ] Create real-world polymorphic scenarios

---

## 🚀 Tomorrow's Preview (Day 9 - July 18)

**Focus:** Exception Handling
- try-catch-finally blocks and exception flow
- Checked vs unchecked exceptions
- Creating custom exceptions
- Exception handling best practices
- Building robust error-resistant applications

---

## 🎉 Excellent Progress!

You've mastered advanced OOP concepts! Polymorphism and abstraction are powerful tools that enable flexible, maintainable, and extensible code. Tomorrow we'll learn to handle errors gracefully with exception handling.

**Keep building on this solid foundation!** 🚀