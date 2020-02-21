## Pintos Operating System (Summer at UC Berkeley)

### Course Info

The purpose of this course is to teach the design of operating systems and operating systems concepts that appear in other computer systems. Topics we will cover include concepts of operating systems, system programming, networked and distributed systems, and storage systems, including multiple-program systems (processes, interprocess communication, and synchronization), memory allocation (segmentation, paging), resource allocation and scheduling, file systems, basic networking (sockets, layering, APIs, reliability), transactions, security, and privacy.

We will be using the Pintos educational operating system for all three projects.


### Project 3

Project 3 folder contains a `inode.c` file, where I implemented the `Fast File System` for the Pintos Operating system.
The whole Pintos code base is too large so I won't include all of it in here. Besices, I only reponsible for implementing the file system in my team for this project; thus there is no need to inlcude other people's work.


### Project 2
Project 2 folder contains a `process.c` file and a `syscall.c` file. 


### Project 1
We implemented multithreading and synchronization in Project 1. But I didn't have a copy of the project 1, so I didn't include it here.

### Homework 2
For homework 2, you’ll be building a shell, similar to the Bash shell you use on your CS 162 Virtual Machine. When you open a terminal window on your computer, you are running a shell program, which is bash on your VM. The purpose of a shell is to provide an interface for users to access an operating system’s services, which include file and process management. The Bourne shell (sh) is the original Unix shell, and there are many different flavors of shells available. Some other examples include ksh (Korn shell), tcsh (TENEX C shell), and zsh (Z shell). Shells can be interactive or non-interactive. For instance, you are using bash non-interactively when you run a bash script. It is important to note that bash is non-interactive by default; run bash -i for a interactive shell.
The operating system kernel provides well-documented interfaces for building shells. By building your own, you’ll become more familiar with these interfaces and you’ll probably learn more about other shells as well.
