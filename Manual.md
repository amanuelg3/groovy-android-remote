## Installation ##

### Server ###
You will need a JAVA JRE 1.6 to use the server.

  1. Download the current version of the server.
  1. Extract the zip into a folder on your hard drive
  1. Start the server with start.cmd (Windows) or start.sh (Linux)
    * The dfeault password is '123'. You can change this by replacing the password in the start script.
On Windows the start script opens a black console window. Leave this open.

#### Custom commands ####
The Android client can send custom commands to the server. Each custom command is essentially a line of text. This text has to be mapped to a shell command. (Beta note: Currently this works only for Windows. This will change in a later version.)

A command is created in the file properties.cfg.

Each command has the following structure:

`<cmd> = $/<cmdline>/$`

As an example the properties.cfg already has a command for use with [EventGhost](http://www.eventghost.org/). Change the path to your installation of EventGhost.

### Android Client ###
#### Manual Install ####

  1. Download and unzip the Groovy Android Remote.
  1. Copy com.linuxfunkar.mousekeysremote.MouseKeysRemote.apk onto your Android device.
  1. Install the APK on your Android device with your favourite file manager.
  1. Start the server, if you haven't done so already.
  1. Add an exception to the firewall for the server port (default: 5555)
  1. Start Groovy Remote Control from the App Menu on your Android device.
  1. Open the Options menu and select Options/Settings
  1. Enter the IP-Address and Port of your server. (The server will show these value in the first line of the console.)
  1. Set the password. If you haven't changed the start script of the server, this has to be '123'.
  1. You can change other settings here. (Beta notice: Not all settings are working right now.)
  1. Leave the settings with the back button.
  1. If everything ist setup correctly, you should see a 'Connection Established' message, otherwise a 'Connection failed' message.


#### Google Play ####
Coming soon...

## Usage ##
### Basics ###

On startup you will see a number of buttons and below these a large and a small gray rectangle.

The large rectangle is a touchpad. It works like a touchpad on a laptop. Moving the finger on the touchpad will move the mouse on your PC. A short tap is translated to a left mouse click.

The smaller rectangle functions as a mouse wheel. Moving the finger up and down on the small rectangle is treated as a movement of the mouse wheel. This is very useful for scrolling in windows.

The buttons on the top can be configured to send keystrokes to the PC. The number of buttons per column and row is also configurable. You can also change the color and label of a button. Or you can hide them. Buttons can also made sticky, so they stay pressed when clicked.

### Configuring Buttons ###
To modify a button make a long touch on the button. A context menu with several options appears. (Note: for a sticky button the menu will appear only after you release the buttons.)

  * Command: You can select a keyboard event from the list or enter a custom command into the textbox on the top. Custom commands override the keyboard command. The textbox must be empty for keyboard events to work. For security reasons custom commands have to be setup in the server. A custom command can have any number of arguments, which will be added to the command as command line arguments.
  * Label: By default the label on the button will be identical with the keyboard event selected. The label can be changed with this menu item. Setting a label is necessary for custom commands, because htey have no default label.
  * Color: Changes the background color of the button.
  * Sticky: Makes a button sticky or not sticky.
  * Hide: Hides the button. For obvious reasons buttons can only be unhidden by using the Option Menu item 'Unhide All'. This will unhide all buttons.

### The Options Menu ###
Pressing the Menu button will bring up an Options Menu.

  * Virtual keyboard: This opens the Android keyboard and a screen with a single textbox. Text entered in this textbox will be sent verbatim to the PC.
  * Mouse/Keypad: Beta note: This button does nothing and will be removed in a later version.
  * Unhide All: Unhides all hidden buttons.
  * Select Layout: You can switch between different button layouts. Up to 32 different layouts can be used. Every layout can be configured indiviually.
  * Settings: Opens the application settings.
  * Calibrate sensors: If activated in the settings, you can control the mouse cursor by tilting your Android device. This menu item calibrates the position sensors of your device.
  * About: Shows infos about this App.
  * Exit: Does what it says.

## Using with EventGhost ##
[EventGhost](http://www.eventghost.org/) is a powerful automation software for Windows. It interfaces with remote controls to allow complete control over your computer and certain attached devices.

Groovy Android Remote can send events to EventGhost via custom commands.

The custom command

`eg -e event1`

would send an event 'Main.event1' to EventGhost. In EventGhost you can map this event to a macro.