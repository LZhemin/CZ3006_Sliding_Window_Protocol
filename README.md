# CZ3006_Sliding_Window_Protocol
Project for CZ3006 Net Centric Computing at Nanyang Technological University.
Project was done as part of a coursework requirement for CZ3006 in AY17/18 Semester 1.

## About
This assignment aims to enhance students' understanding of the network protocol hierarchy and flow control and error control techniques by implementing a sliding window protocol in a simulated communication network system. The programming language will be Java.


Folder Structure Conventions
============================

> Folder structure options and naming conventions for project

### Source Files

Two java source files are provided:

    .
    ├── SWP.java        # The skeleton of the Sliding Window Protocol component. Note: this is the only provided java source file that you can change. You may add new java classes in order to fully implement the sliding window protocol.
    └── PFrame.java     # The frame class source file. Note: this java source file is provided for your reference in implementing sliding window protocol, but you should not change anything in this file.


 ### Class Files
 
A number of java class files are provided, which implement the Network Simulator component and the Sliding Window Environment component:

    .
    ├── NetSim.class                               # the main class of the Network Simulator component.
    ├── Forwarder.class                            # an auxiliary thread class of the NetSim component.
    ├── VMach.class                                # the main class of the Virtual Machine component
    ├── SWE.class                                  # the major class of the Sliding Window Environment component.
    ├── FrameHanlder.class                         # an auxiliary thread class of the SWE component.
    ├── NetworkSender.class                        # an auxiliary thread class of the SWE component.
    ├── NetworkReceiver.class                      # an auxiliary thread class of the SWE component.
    ├── EventQueue.class                           #an auxiliary class of the SWE component.
    ├── Packet.class                               # an auxiliary class of the SWE component.
    ├── PacketQueue.class                          # an auxiliary class of the SWE component.
    ├── PEvent.class                               #  an auxiliary class of the SWE component.
    ├── PFrame.class                               # an auxiliary class of the SWE component
    └── PFrameMsg.class                            # an auxiliary class of the SWE component.


### Testing Text Files
 
Two text files are provided for testing purpose. They are used by the Sliding Window Environment component to generate a sequence of packets to be sent to the other communicating machines:


    .
    ├── send_file_1.txt                              # the file is used by VMach 1 to generate a sequence of (text) packets to VMach 2
    └── send_file_2.txt                              # the file is used by VMach 2 to generate a sequence of (text) packets to VMach 1.
    
****

***Disclaimer:*** This repo is depreciated and no longer maintained. All rights reserved to Nanyang Technological Univiserity and the Designer of course CZ3006. The author will bear no responsibilities for any issues arised from academic integrity of anyone who takes this repository as a reference.
