all:
	make compile

compile:
	javac -d out src/main/pt/up/fe/cpd2324/**/*.java

server:
	make compile && java -cp out pt.up.fe.cpd2324.server.Server

client:
	make compile && java -cp out pt.up.fe.cpd2324.client.Client $(ARGS)
