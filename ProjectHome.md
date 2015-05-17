## Introduction ##

This is an Android remote control for your PC. The software consists of a server for the PC and a client application for Android devices. It can control the mouse and the keyboard of a PC and has customizable buttons.

Based on MouseKeysRemote (http://www.linuxfunkar.se/?q=node/58) by Magnus Uppman.

There are a number of remote control applications on Google Play. But most of them aren't free. And the few free ones I could find aren't open source. Then I stumbled upon MouseKeysRemote, which is open source and has a customizable onscreen keyboard, something the other free remotes haven't. But there were still a lot of features missing, that I wanted to have.

So far I have created a new user interface and I have added some new features.

## Features ##
  * Support for mouse movement and mouse wheel
  * Keyboard commands
  * Customizable buttons
    * Label
    * Color
    * Visibility
  * Configurable custom commands
  * Traffic with server is password encrypted
  * Server logging via log4j
  * New [color picker](https://code.google.com/p/android-color-picker/)