SRC_DIR = ./src
BIN_DIR = ./bin
JAVAC = javac
JAVA = java

all: build

build:
	mkdir -p $(BIN_DIR)
	$(JAVAC) -d $(BIN_DIR) $(SRC_DIR)/*.java

run_client: build
	$(JAVA) -cp $(BIN_DIR) src.Client

run_server: build
	$(JAVA) -cp $(BIN_DIR) src.Server

clean:
	rm $(BIN_DIR) 
