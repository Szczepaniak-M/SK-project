CC = gcc
CFLAGS= -Wall -pthread


all: main.c structures.h server.c server.h database.c database.h client-thread.c client-thread.h
	$(CC) $(CFLAGS) main.c server.c database.c client-thread.c -o server -lcrypt -lsqlite3

